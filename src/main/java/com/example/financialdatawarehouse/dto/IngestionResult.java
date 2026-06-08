package com.example.financialdatawarehouse.dto;

import java.time.Instant;
import java.util.List;

public record IngestionResult(
        String jobId,
        Instant startedAt,
        Instant finishedAt,
        int fetchedRecords,
        int transformedRecords,
        int storedAssets,
        int storedDataSources,
        int storedTimeSeriesRecords,
        int skippedDuplicates,
        List<String> failures
) {
}
