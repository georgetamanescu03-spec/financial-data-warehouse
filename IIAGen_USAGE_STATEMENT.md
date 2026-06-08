# IIAGen Usage Statement Draft

Fill in your course template with your own wording. This draft summarizes the AI-assisted work done for this project.

## Generative AI tools used

Codex / ChatGPT was used as a programming assistant for the Java Spring Boot data warehouse project.

## Activities supported by AI

- Reading and interpreting the project PDFs.
- Identifying missing implementation parts in the initial IntelliJ project.
- Designing a MongoDB temporal data model for assets, data sources, and time-series data.
- Implementing Java/Spring Boot model classes, repositories, services, controllers, ingestion logic, analytics logic, and assistant/MCP-style endpoints.
- Preparing bundled sample data for a reproducible local demo.
- Drafting README instructions, project report, and demo script.
- Adding unit tests for the temporal latest-version behavior.

## Human decisions and responsibility

I reviewed the generated code and documentation, selected the implementation approach, and remain responsible for understanding, running, testing, and presenting the final project. I will verify the project in IntelliJ and adjust the explanation/demo to match my course requirements.

## Limitations

The bundled dataset is a local demonstration dataset, not live market data. The analytics job is implemented as a local Java workflow that mirrors the Spark use cases; a production-scale version can be moved to Apache Spark with the MongoDB Spark Connector.
