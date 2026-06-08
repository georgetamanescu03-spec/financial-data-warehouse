package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.dto.AnalyticsRunResponse;
import com.example.financialdatawarehouse.model.AnalyticsSummary;
import com.example.financialdatawarehouse.model.PredictionResult;
import com.example.financialdatawarehouse.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/run")
    public AnalyticsRunResponse runAnalytics(
            @RequestParam(defaultValue = "BTCUSD") String assetId,
            @RequestParam(defaultValue = "NASDAQ-DATA-LINK.QDL/BITFINEX") String dataSourceId
    ) {
        return analyticsService.runAnalytics(assetId, dataSourceId);
    }

    @GetMapping("/summaries")
    public List<AnalyticsSummary> listSummaries(
            @RequestParam(defaultValue = "BTCUSD") String assetId,
            @RequestParam(defaultValue = "NASDAQ-DATA-LINK.QDL/BITFINEX") String dataSourceId
    ) {
        return analyticsService.listSummaries(assetId, dataSourceId);
    }

    @GetMapping("/predictions")
    public List<PredictionResult> listPredictions(
            @RequestParam(defaultValue = "BTCUSD") String assetId,
            @RequestParam(defaultValue = "NASDAQ-DATA-LINK.QDL/BITFINEX") String dataSourceId
    ) {
        return analyticsService.listPredictions(assetId, dataSourceId);
    }
}
