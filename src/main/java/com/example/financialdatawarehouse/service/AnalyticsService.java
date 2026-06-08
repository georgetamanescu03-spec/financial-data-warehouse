package com.example.financialdatawarehouse.service;

import com.example.financialdatawarehouse.dto.AnalyticsRunResponse;
import com.example.financialdatawarehouse.model.AnalyticsSummary;
import com.example.financialdatawarehouse.model.PredictionResult;
import com.example.financialdatawarehouse.model.TimeSeriesData;
import com.example.financialdatawarehouse.repository.AnalyticsSummaryRepository;
import com.example.financialdatawarehouse.repository.PredictionResultRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {
    private final WarehouseService warehouseService;
    private final AnalyticsSummaryRepository summaryRepository;
    private final PredictionResultRepository predictionRepository;

    public AnalyticsService(
            WarehouseService warehouseService,
            AnalyticsSummaryRepository summaryRepository,
            PredictionResultRepository predictionRepository
    ) {
        this.warehouseService = warehouseService;
        this.summaryRepository = summaryRepository;
        this.predictionRepository = predictionRepository;
    }

    public AnalyticsRunResponse runAnalytics(String assetId, String dataSourceId) {
        List<TimeSeriesData> records = warehouseService.findLatestTimeSeries(
                assetId,
                dataSourceId,
                LocalDate.of(1900, 1, 1),
                LocalDate.now().plusYears(5),
                null
        );
        Instant computedAt = Instant.now();
        List<AnalyticsSummary> summaries = computeYearlySummaries(assetId, dataSourceId, records, computedAt);
        PredictionResult prediction = computePrediction(assetId, dataSourceId, records, computedAt);
        summaryRepository.saveAll(summaries);
        if (prediction != null) {
            predictionRepository.save(prediction);
        }
        return new AnalyticsRunResponse(assetId, dataSourceId, summaries, prediction);
    }

    public List<AnalyticsSummary> listSummaries(String assetId, String dataSourceId) {
        return summaryRepository.findByAssetIdAndDataSourceIdOrderByBusinessYearDescComputedAtDesc(assetId, dataSourceId);
    }

    public List<PredictionResult> listPredictions(String assetId, String dataSourceId) {
        return predictionRepository.findByAssetIdAndDataSourceIdOrderByComputedAtDesc(assetId, dataSourceId);
    }

    private List<AnalyticsSummary> computeYearlySummaries(
            String assetId,
            String dataSourceId,
            List<TimeSeriesData> records,
            Instant computedAt
    ) {
        Map<Integer, DoubleSummaryStatistics> statsByYear = records.stream()
                .filter(record -> record.getBusinessYear() != null)
                .flatMap(record -> closeValue(record).stream()
                        .mapToObj(close -> Map.entry(record.getBusinessYear(), close)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        LinkedHashMap::new,
                        Collectors.summarizingDouble(Map.Entry::getValue)
                ));

        return statsByYear.entrySet().stream()
                .sorted(Map.Entry.<Integer, DoubleSummaryStatistics>comparingByKey().reversed())
                .map(entry -> toSummary(assetId, dataSourceId, entry.getKey(), entry.getValue(), computedAt))
                .toList();
    }

    private AnalyticsSummary toSummary(
            String assetId,
            String dataSourceId,
            Integer businessYear,
            DoubleSummaryStatistics stats,
            Instant computedAt
    ) {
        AnalyticsSummary summary = new AnalyticsSummary();
        summary.setDocumentId(assetId + "::" + dataSourceId + "::summary::" + businessYear + "::" + computedAt.toEpochMilli());
        summary.setAssetId(assetId);
        summary.setDataSourceId(dataSourceId);
        summary.setBusinessYear(businessYear);
        summary.setRecordCount(stats.getCount());
        summary.setMinClose(stats.getMin());
        summary.setMaxClose(stats.getMax());
        summary.setAverageClose(stats.getAverage());
        summary.setComputedAt(computedAt);
        return summary;
    }

    private PredictionResult computePrediction(
            String assetId,
            String dataSourceId,
            List<TimeSeriesData> records,
            Instant computedAt
    ) {
        List<Observation> observations = records.stream()
                .flatMap(record -> closeValue(record).stream()
                        .mapToObj(close -> new Observation(record.getBusinessDate(), close)))
                .sorted(Comparator.comparing(Observation::businessDate))
                .toList();
        if (observations.isEmpty()) {
            return null;
        }

        LocalDate predictedDate = observations.get(observations.size() - 1).businessDate().plusDays(1);
        double predictedClose = observations.size() == 1
                ? observations.get(0).close()
                : linearRegressionPrediction(observations, predictedDate);

        PredictionResult result = new PredictionResult();
        result.setDocumentId(assetId + "::" + dataSourceId + "::prediction::" + computedAt.toEpochMilli());
        result.setAssetId(assetId);
        result.setDataSourceId(dataSourceId);
        result.setPredictedBusinessDate(predictedDate);
        result.setPredictedClose(Math.round(predictedClose * 100.0) / 100.0);
        result.setMethod("ordinary least squares over businessDate epoch-day and close price");
        result.setComputedAt(computedAt);
        return result;
    }

    private double linearRegressionPrediction(List<Observation> observations, LocalDate predictedDate) {
        int n = observations.size();
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXX = 0.0;

        for (Observation observation : observations) {
            double x = observation.businessDate().toEpochDay();
            double y = observation.close();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }

        double denominator = n * sumXX - sumX * sumX;
        if (Math.abs(denominator) < 0.000001) {
            return observations.get(n - 1).close();
        }
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        return intercept + slope * predictedDate.toEpochDay();
    }

    private OptionalDouble closeValue(TimeSeriesData record) {
        if (record.getValues() == null) {
            return OptionalDouble.empty();
        }
        for (String key : List.of("close", "Close", "last", "Last", "price", "Price")) {
            Object value = record.getValues().get(key);
            if (value instanceof Number number) {
                return OptionalDouble.of(number.doubleValue());
            }
            if (value instanceof String text) {
                try {
                    return OptionalDouble.of(Double.parseDouble(text));
                } catch (NumberFormatException ignored) {
                    return OptionalDouble.empty();
                }
            }
        }
        return OptionalDouble.empty();
    }

    private record Observation(LocalDate businessDate, double close) {
    }
}
