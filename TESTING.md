# Testing

In this project, "test" means an automated check that verifies part of the code works correctly.

Tests are not the same as the demo video:

- The demo shows the professor the application working.
- Tests are code checks run by Maven or IntelliJ.

## Existing Tests

The project has tests in:

```text
src/test/java
```

Important test:

```text
TemporalSelectorsTests
```

This test verifies the temporal data warehouse rule:

- if two versions exist for the same business date, the newest `systemDate` version wins
- if the newest version is a deletion marker, the old value is not returned

## Run Tests

In IntelliJ terminal:

```powershell
.\mvnw.cmd test
```

If Maven dependencies are already downloaded, this should run quickly.

## What To Say If Asked

```text
I added unit tests for the temporal versioning logic. They check that the data warehouse returns the latest version of a time-series point and correctly handles deletion marker records.
```
