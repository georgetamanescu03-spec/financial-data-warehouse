package com.example.financialdatawarehouse.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.count;
import static org.apache.spark.sql.functions.current_timestamp;
import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.max;
import static org.apache.spark.sql.functions.min;
import static org.apache.spark.sql.functions.row_number;

public class SparkYearlyAggregationJob {
    public static void main(String[] args) {
        String mongoUri = arg(args, 0, "mongodb://localhost:27017/financial_dwh");
        String database = arg(args, 1, "financial_dwh");
        String master = arg(args, 2, "local[*]");

        SparkSession spark = SparkSession.builder()
                .appName("Financial DWH - Spark Yearly Aggregation")
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
        Dataset<Row> recordsWithClose = latestRecords
                .withColumn("close", col("values.close").cast("double"))
                .where(col("close").isNotNull());

        Dataset<Row> yearlySummaries = recordsWithClose
                .groupBy(col("assetId"), col("dataSourceId"), col("businessYear"))
                .agg(
                        count(lit(1)).alias("recordCount"),
                        min("close").alias("minClose"),
                        max("close").alias("maxClose"),
                        avg("close").alias("averageClose")
                )
                .withColumn("computedAt", current_timestamp());

        yearlySummaries.write()
                .format("mongodb")
                .mode(SaveMode.Append)
                .option("database", database)
                .option("collection", "spark_yearly_summaries")
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
