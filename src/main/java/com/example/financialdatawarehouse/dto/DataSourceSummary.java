package com.example.financialdatawarehouse.dto;

public record DataSourceSummary(
        String dataSourceId,
        String provider,
        String name,
        String description
) {
}
