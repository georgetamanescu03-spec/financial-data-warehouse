package com.example.financialdatawarehouse.spark;

import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.time.LocalDate;
import java.util.List;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.datediff;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.row_number;
import static org.apache.spark.sql.functions.to_date;

public class SparkClosePredictionJob {
    public static void main(String[] args) {
        String assetId = arg(args, 0, "BTCUSD");
        String dataSourceId = arg(args, 1, "NASDAQ-DATA-LINK.QDL/BITFINEX");
        String mongoUri = arg(args, 2, "mongodb://localhost:27017/financial_dwh");
        String database = arg(args, 3, "financial_dwh");
        String master = arg(args, 4, "local[*]");

        SparkSession spark = SparkSession.builder()
                .appName("Financial DWH - Spark Close Prediction")
                .master(master)
                .config("spark.mongodb.read.connection.uri", mongoUri)
                .config("spark.mongodb.write.connection.uri", mongoUri)
                .getOrCreate();

        Dataset<Row> rawRecords = spark.read()
                .format("mongodb")
                .option("database", database)
                .option("collection", "time_series")
                .load();

        Dataset<Row> latestRecords = latestNonDeletedVersion(rawRecords);
        Dataset<Row> trainingData = latestRecords
                .where(col("assetId").equalTo(assetId))
                .where(col("dataSourceId").equalTo(dataSourceId))
                .withColumn("businessDateAsDate", to_date(col("businessDate").cast("string")))
                .withColumn("epochDay", datediff(col("businessDateAsDate"), lit("1970-01-01")).cast("double"))
                .withColumn("close", col("values.close").cast("double"))
                .where(col("epochDay").isNotNull())
                .where(col("close").isNotNull())
                .select("businessDateAsDate", "epochDay", "close");

        if (trainingData.count() < 2) {
            throw new IllegalStateException("At least two records are required to train the Spark prediction model.");
        }

        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{"epochDay"})
                .setOutputCol("features");

        Dataset<Row> features = assembler.transform(trainingData);
        LinearRegressionModel model = new LinearRegression()
                .setLabelCol("close")
                .setFeaturesCol("features")
                .setMaxIter(20)
                .setRegParam(0.1)
                .fit(features);

        Row maxEpochRow = trainingData.agg(max("epochDay").alias("maxEpochDay")).first();
        double nextEpochDay = ((Number) maxEpochRow.getAs("maxEpochDay")).doubleValue() + 1.0;
        String predictedBusinessDate = LocalDate.ofEpochDay((long) nextEpochDay).toString();

        StructType predictionSchema = new StructType(new StructField[]{
                new StructField("epochDay", DataTypes.DoubleType, false, Metadata.empty())
        });
        Dataset<Row> predictionInput = spark.createDataFrame(
                List.of(RowFactory.create(nextEpochDay)),
                predictionSchema
        );

        Dataset<Row> prediction = model.transform(assembler.transform(predictionInput))
                .select(col("prediction").alias("predictedClose"))
                .withColumn("assetId", lit(assetId))
                .withColumn("dataSourceId", lit(dataSourceId))
                .withColumn("predictedBusinessDate", lit(predictedBusinessDate))
                .withColumn("method", lit("Apache Spark ML LinearRegression"))
                .withColumn("computedAt", current_timestamp());

        prediction.write()
                .format("mongodb")
                .mode(SaveMode.Append)
                .option("database", database)
                .option("collection", "spark_close_predictions")
                .save();

        spark.stop();
    }

    private static Dataset<Row> latestNonDeletedVersion(Dataset<Row> rawRecords) {
        WindowSpec latestVersionWindow = Window
                .partitionBy("assetId", "dataSourceId", "businessDate")
                .orderBy(col("systemDate").desc());

        return rawRecords
                .withColumn("versionRank", row_number().over(latestVersionWindow))
                .where(col("versionRank").equalTo(1))
                .where(col("deletedMarker").equalTo(false).or(col("deletedMarker").isNull()))
                .drop("versionRank");
    }

    private static String arg(String[] args, int index, String defaultValue) {
        return args.length > index && args[index] != null && !args[index].isBlank()
                ? args[index]
                : defaultValue;
    }
}
