# Real Spark analytics module

This project includes a real Apache Spark module in addition to the Spring Boot analytics endpoints.

The Spark source code is stored separately:

```text
src/spark/java/com/example/financialdatawarehouse/spark
```

It is compiled only when the Maven `spark` profile is enabled. This keeps the normal Spring Boot application simple and stable, while still providing real Spark jobs for the analytics requirement.

## Spark Jobs

### 1. Yearly aggregation

Class:

```text
com.example.financialdatawarehouse.spark.SparkYearlyAggregationJob
```

What it does:

1. Reads `time_series` records from MongoDB.
2. Selects the latest temporal version for each asset, data source, and business date.
3. Ignores deletion marker records.
4. Groups records by asset, data source, and business year.
5. Computes:
   - count
   - minimum close
   - maximum close
   - average close
6. Writes results to MongoDB collection:

```text
spark_yearly_summaries
```

### 2. Close price prediction

Class:

```text
com.example.financialdatawarehouse.spark.SparkClosePredictionJob
```

What it does:

1. Reads `time_series` records from MongoDB.
2. Filters records for one asset and data source.
3. Builds a training dataset from business date and close price.
4. Uses Spark ML `LinearRegression`.
5. Predicts the next close price.
6. Writes results to MongoDB collection:

```text
spark_close_predictions
```

## Build Spark module

Use:

```powershell
.\mvnw.cmd -Pspark -DskipTests package
```

The first run may take time because Maven downloads Spark dependencies.

## Run Spark Aggregation

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkYearlyAggregationJob"
```

Optional arguments:

```text
mongoUri database master
```

Example:

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkYearlyAggregationJob" -Dexec.args="mongodb://localhost:27017/financial_dwh financial_dwh local[*]"
```

## Run Spark prediction

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkClosePredictionJob"
```

Optional arguments:

```text
assetId dataSourceId mongoUri database master
```

Example:

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkClosePredictionJob" -Dexec.args="BTCUSD NASDAQ-DATA-LINK.QDL/BITFINEX mongodb://localhost:27017/financial_dwh financial_dwh local[*]"
```
