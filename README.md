# Financial Data Warehouse

Spring Boot and MongoDB implementation for the Data Warehouses lab project. The platform stores temporal financial market data, ingests sample provider data, exposes REST APIs for consumers, computes analytics outputs, and provides MCP-style tool endpoints for an LLM assistant.

## What is included

- MongoDB NoSQL storage for assets, data sources, time-series points, yearly summaries and predictions.
- Temporal warehouse model: records are versioned with `systemDate`; updates create new versions; deletion is represented with marker records.
- Flexible attributes and indicator maps for heterogeneous providers and asset classes.
- Data access layer through repositories and `WarehouseService`.
- Batch ingestion flow for a bundled Nasdaq Data Link / Bitfinex-style sample dataset.
- REST API for Q1-Q5:
  - `GET /api/v1/assets`
  - `GET /api/v1/assets/{assetId}`
  - `GET /api/v1/data-sources`
  - `GET /api/v1/data-sources/{dataSourceId}`
  - `GET /api/v1/data?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX`
- Analytics:
  - yearly close-price summaries
  - simple next-close prediction stored back in MongoDB
- Real Spark module:
  - `SparkYearlyAggregationJob`
  - `SparkClosePredictionJob`
  - see `SPARK_MODULE.md`
- Assistant and MCP-style tool surface:
  - `GET /api/v1/assistant/tools`
  - `POST /api/v1/assistant/chat`
  - `POST /api/v1/mcp`
- MCP-style assistant tools:
  - `agent_market_brief` performs a multi-step workflow over warehouse data.
  - The tools can be called directly through the `/api/v1/mcp` endpoint.
- OpenAPI/Swagger UI through SpringDoc.

## Run locally

1. Start MongoDB:

```powershell
docker compose up -d
```

2. Open the project in IntelliJ IDEA as a Maven project.

3. Run:

```text
com.example.financialdatawarehouse.FinancialDataWarehouseApplication
```

4. The app starts at:

```text
http://localhost:8080
```

5. Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

The app seeds the bundled sample data automatically by default. To disable that, set:

```properties
warehouse.seed-sample-data=false
```

## Useful demo requests

List assets:

```powershell
curl http://localhost:8080/api/v1/assets
```

Inspect one asset:

```powershell
curl http://localhost:8080/api/v1/assets/BTCUSD
```

List data sources:

```powershell
curl http://localhost:8080/api/v1/data-sources
```

Fetch time series:

```powershell
curl "http://localhost:8080/api/v1/data?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX&startBusinessDate=2026-05-27&endBusinessDate=2026-06-03&includeAttributes=true"
```

Run ingestion manually:

```powershell
curl -X POST http://localhost:8080/api/v1/ingestion/sample
```

Run analytics:

```powershell
curl -X POST "http://localhost:8080/api/v1/analytics/run?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX"
```

Ask the assistant for a trend:

```powershell
curl -X POST http://localhost:8080/api/v1/assistant/chat -H "Content-Type: application/json" -d "{\"message\":\"summarize BTC trend\"}"
```

Call MCP-style tools:

Initialize the MCP-style server:

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"
```

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"
```

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"summarize_trends\",\"arguments\":{\"assetId\":\"BTCUSD\",\"dataSourceId\":\"NASDAQ-DATA-LINK.QDL/BITFINEX\"}}}"
```

Run the agent-style market brief:

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"agent_market_brief\",\"arguments\":{\"primaryAssetId\":\"BTCUSD\",\"secondaryAssetId\":\"ETHUSD\",\"dataSourceId\":\"NASDAQ-DATA-LINK.QDL/BITFINEX\"}}}"
```

## Spark Module

Build the Spark module:

```powershell
.\mvnw.cmd -Pspark -DskipTests package
```

Run Spark yearly aggregation:

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkYearlyAggregationJob"
```

Run Spark close prediction:

```powershell
.\mvnw.cmd -Pspark exec:java -Dexec.mainClass="com.example.financialdatawarehouse.spark.SparkClosePredictionJob"
```

More details are in `SPARK_MODULE.md`.

## Tests

Run automated tests:

```powershell
.\mvnw.cmd test
```

More details are in `TESTING.md`.

## Notes

The Spring Boot analytics endpoints are intentionally runnable inside the application for a simple local demo. The project also includes a real Apache Spark module under `src/spark/java`, with one aggregation job and one Spark ML prediction job that read from MongoDB and write results back to MongoDB.
