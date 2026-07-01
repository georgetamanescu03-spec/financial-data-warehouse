# LLM / Agentic AI Consumer With MCP

This file documents the part of the project related to the LLM / Agentic AI consumer. The Spring Boot application exposes a lightweight MCP-style JSON-RPC endpoint at:

```text
POST http://localhost:8080/api/v1/mcp
```

The assistant is grounded in warehouse data. It does not answer from generic finance knowledge, it calls tools backed by the platform services and MongoDB records.

## MCP Methods

### 1. Initialize

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}"
```

Expected result includes:

- protocol version
- server name
- tool capability

### 2. List tools

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"
```

Available tools:

- `list_assets`
- `get_asset`
- `fetch_time_series`
- `summarize_trends`
- `compare_assets`
- `agent_market_brief`
- `run_ingestion`
- `run_analytics`

### 3. Call A Tool

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"summarize_trends\",\"arguments\":{\"assetId\":\"BTCUSD\",\"dataSourceId\":\"NASDAQ-DATA-LINK.QDL/BITFINEX\"}}}"
```

The result contains:

- `content`: text output suitable for an LLM client
- `structuredContent`: structured JSON data for programmatic use
- `isError`: tool execution status

## Agentic workflow

The project includes an agent-style tool:

```text
agent_market_brief
```

This demonstrates a multi-step workflow:

1. Discover available assets.
2. Summarize the primary asset trend.
3. Summarize the secondary asset trend.
4. Compare both assets.
5. Run analytics and next-close prediction.
6. Return a grounded market brief.

Call it with:

```powershell
curl -X POST http://localhost:8080/api/v1/mcp -H "Content-Type: application/json" -d "{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"agent_market_brief\",\"arguments\":{\"primaryAssetId\":\"BTCUSD\",\"secondaryAssetId\":\"ETHUSD\",\"dataSourceId\":\"NASDAQ-DATA-LINK.QDL/BITFINEX\"}}}"
```

## Assistant chat endpoint

For a simpler demo, the project also exposes:

```text
POST http://localhost:8080/api/v1/assistant/chat
```

Example:

```powershell
curl -X POST http://localhost:8080/api/v1/assistant/chat -H "Content-Type: application/json" -d "{\"message\":\"prepare an agent market brief\",\"arguments\":{}}"
```
