# Project Report: Financial Data Warehouse

## Objective

Acme Ltd needs a financial market data warehouse that can ingest market data from external providers, store it safely over time, expose it to consumers, support analytics, and provide data-grounded assistant tools. This project implements those requirements with Java, Spring Boot and MongoDB.

## Technology stack

- Java 17
- Spring Boot
- Spring Data MongoDB
- MongoDB 7
- SpringDoc OpenAPI
- JUnit 5
- Docker Compose for local MongoDB

## Data model

The platform stores three core warehouse concepts:

- `Asset`: logical financial instrument such as `BTCUSD`, including symbol, asset class, region, description, and flexible attributes.
- `DataSource`: provider metadata and provenance, such as the bundled Nasdaq Data Link / Bitfinex sample.
- `TimeSeriesData`: market observations for one asset and provider on a business date, with flexible indicator values such as open, high, low, close and volume.

MongoDB document ids are separated from business ids. This is important because the warehouse must allow multiple versions of the same asset, source or time-series point.

## Temporal warehouse behavior

The project follows the temporal rule from the assignment:

- Existing records are not updated in place as the logical source of truth.
- A changed asset/source/time-series point is represented by adding a new version.
- `systemDate` records when the version entered the warehouse.
- `businessDate` records when a time-series value is valid in the market.
- `deletedMarker=true` represents logical deletion.
- Query logic returns the newest `systemDate` version for each business date and suppresses older values when the newest version is a deletion marker.

The `TemporalSelectorsTests` unit tests verify latest-version selection and deletion marker behavior.

## Ingestion

The ingestion module loads a bundled provider dataset from:

```text
src/main/resources/data/sample-market-data.json
```

The workflow is split into:

1. Extract: read the provider payload.
2. Transform: normalize assets, source metadata, and time-series points.
3. Load: write warehouse records through `WarehouseService`.

The ingestion result reports fetched records, transformed records, stored assets, stored data sources, stored time-series records, duplicate skips and failures. The import is duplicate-safe by using version hashes.

## REST consumption API

The API supports the required queries:

- Q1: list financial assets with pagination.
- Q2: fetch asset details by id.
- Q3: list financial data sources with pagination.
- Q4: fetch data source details by id.
- Q5: fetch latest-version time-series data for an asset and data source, optionally constrained by business-date range and `asOfSystemTime`.

The API base URL is:

```text
http://localhost:8080/api/v1
```

Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Analytics

The analytics module reads current time-series records and stores outputs back into MongoDB:

- `analytics_yearly_summaries`: yearly count, min close, max close, and average close.
- `analytics_predictions`: next-business-day close prediction using ordinary least squares over business date and close price.

This provides the same functional shape as the Spark lab requirements: read warehouse data, aggregate it, build a prediction workflow and persist derived outputs. For a larger deployment, the same collections can be consumed by Apache Spark through the MongoDB Spark Connector.

## LLM / MCP Assistant

The assistant exposes data-grounded tools that can be called by a model or demonstrated directly:

- `list_assets`
- `get_asset`
- `fetch_time_series`
- `summarize_trends`
- `compare_assets`
- `agent_market_brief`
- `run_ingestion`
- `run_analytics`

Endpoints:

- `GET /api/v1/assistant/tools`
- `POST /api/v1/assistant/chat`
- `POST /api/v1/mcp`

The `/api/v1/mcp` endpoint accepts simple JSON-RPC-style `tools/list` and `tools/call` messages, making the assistant behavior demonstrable without requiring an external LLM API key.

For the agentic AI requirement, the `agent_market_brief` tool performs a multi-step workflow: discover assets, summarize a primary asset, summarize a secondary asset, compare both assets, run analytics, and return a grounded market brief. This workflow is implemented directly inside the Spring Boot application and can be called through the `/api/v1/mcp` endpoint.

## Reproducibility

1. Start MongoDB with Docker Compose.
2. Run the Spring Boot application from IntelliJ.
3. Use Swagger or the commands from README.md to call ingestion, data exploration, analytics, and assistant endpoints.

The bundled sample data keeps the demo reproducible without external provider credentials.
