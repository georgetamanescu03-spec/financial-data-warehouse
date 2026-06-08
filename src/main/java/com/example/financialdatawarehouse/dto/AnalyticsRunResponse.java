package com.example.financialdatawarehouse.dto;

import com.example.financialdatawarehouse.model.AnalyticsSummary;
import com.example.financialdatawarehouse.model.PredictionResult;

import java.util.List;

public record AnalyticsRunResponse(
        String assetId,
        String dataSourceId,
        List<AnalyticsSummary> yearlySummaries,
        PredictionResult prediction
) {
}
