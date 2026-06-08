package com.example.financialdatawarehouse.ingestion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SampleMarketDataset(
        String dataSourceId,
        String provider,
        String name,
        String description,
        String baseUrl,
        Map<String, Object> provenance,
        List<SampleAsset> assets
) {
    public record SampleAsset(
            String assetId,
            String symbol,
            String assetClass,
            String region,
            String name,
            String description,
            Map<String, Object> attributes,
            List<SamplePoint> points
    ) {
    }

    public record SamplePoint(
            LocalDate businessDate,
            Map<String, Object> values,
            String sourceReference
    ) {
    }
}
