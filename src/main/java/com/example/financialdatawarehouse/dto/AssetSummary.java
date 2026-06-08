package com.example.financialdatawarehouse.dto;

public record AssetSummary(
        String assetId,
        String symbol,
        String assetClass,
        String region,
        String name
) {
}
