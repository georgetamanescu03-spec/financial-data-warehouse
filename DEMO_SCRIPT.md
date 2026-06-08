# Demo Script

Use this script for the required short video. It shows ingestion, storage, API exploration, analytics, and assistant/MCP usage.

## 1. Start the platform

Start MongoDB:

```powershell
docker compose up -d
```

Run the Spring Boot app in IntelliJ.

Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## 2. Ingestion

Say:

```text
I start by running the ingestion endpoint. It reads a bundled external-provider dataset, transforms it into the warehouse model, stores assets, data-source metadata, and time-series records, and reports counts.
```

Run:

```powershell
curl -X POST http://localhost:8080/api/v1/ingestion/sample
```

Show:

- fetched records
- stored assets
- stored time-series records
- skipped duplicates if run again

## 3. API exploration

Say:

```text
Now I use the consumption API to discover assets and data sources without exposing MongoDB details to the client.
```

Run:

```powershell
curl http://localhost:8080/api/v1/assets
```

```powershell
curl http://localhost:8080/api/v1/assets/BTCUSD
```

```powershell
curl http://localhost:8080/api/v1/data-sources
```

Run the Q5 time-series query:

```powershell
curl "http://localhost:8080/api/v1/data?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX&startBusinessDate=2026-05-27&endBusinessDate=2026-06-03&includeAttributes=true"
```

Mention:

```text
The response returns latest versions only, ordered by business date, and includes heterogeneous attributes when requested.
```

## 4. Analytics

Say:

```text
Next I run analytics. The job reads the warehouse records, computes yearly close-price summaries, generates a next-close prediction, and stores the outputs back into MongoDB.
```

Run:

```powershell
curl -X POST "http://localhost:8080/api/v1/analytics/run?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX"
```

Then show stored outputs:

```powershell
curl "http://localhost:8080/api/v1/analytics/summaries?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX"
```

```powershell
curl "http://localhost:8080/api/v1/analytics/predictions?assetId=BTCUSD&dataSourceId=NASDAQ-DATA-LINK.QDL/BITFINEX"
```

## 5. Assistant and MCP tools

Say:

```text
The platform also exposes assistant tools. The assistant answers using the warehouse data and can be connected to an LLM through the MCP-style JSON-RPC endpoint.
```

List tools:

```powershell
curl http://localhost:8080/api/v1/assistant/tools
```

Ask a grounded trend question:

```powershell
curl -X POST http://localhost:8080/api/v1/assistant/chat -H "Content-Type: application/json" -d "{\"message\":\"summarize BTC trend\"}"
```

Call MCP tools:

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}"
```

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\",\"params\":{\"name\":\"compare_assets\",\"arguments\":{\"assetIdA\":\"BTCUSD\",\"assetIdB\":\"ETHUSD\",\"dataSourceId\":\"NASDAQ-DATA-LINK.QDL/BITFINEX\"}}}"
```

Finish with:

```text
This demonstrates ingestion, temporal storage, REST consumption, analytics outputs, and assistant tool usage end to end.
```
