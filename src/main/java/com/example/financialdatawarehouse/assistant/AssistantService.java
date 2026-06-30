package com.example.financialdatawarehouse.assistant;

import com.example.financialdatawarehouse.dto.AssistantResponse;
import com.example.financialdatawarehouse.dto.TimeSeriesResponse;
import com.example.financialdatawarehouse.ingestion.IngestionService;
import com.example.financialdatawarehouse.model.TimeSeriesData;
import com.example.financialdatawarehouse.service.AnalyticsService;
import com.example.financialdatawarehouse.service.WarehouseService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

@Service
public class AssistantService {
    private static final String DEFAULT_SOURCE = "NASDAQ-DATA-LINK.QDL/BITFINEX";

    private final WarehouseService warehouseService;
    private final IngestionService ingestionService;
    private final AnalyticsService analyticsService;

    public AssistantService(
            WarehouseService warehouseService,
            IngestionService ingestionService,
            AnalyticsService analyticsService
    ) {
        this.warehouseService = warehouseService;
        this.ingestionService = ingestionService;
        this.analyticsService = analyticsService;
    }

    public List<ToolSpec> listTools() {
        return List.of(
                new ToolSpec("list_assets", "List available financial assets.", schema("offset", "limit")),
                new ToolSpec("get_asset", "Fetch the latest details for one asset.", schema("assetId")),
                new ToolSpec("fetch_time_series", "Fetch latest-version time-series records for an asset and data source.", schema("assetId", "dataSourceId", "startBusinessDate", "endBusinessDate")),
                new ToolSpec("summarize_trends", "Compute close-price trend statistics from warehouse records.", schema("assetId", "dataSourceId", "startBusinessDate", "endBusinessDate")),
                new ToolSpec("compare_assets", "Compare latest close and period change for two assets.", schema("assetIdA", "assetIdB", "dataSourceId")),
                new ToolSpec("agent_market_brief", "Run a multi-step agent workflow: discover assets, summarize trends, compare assets, and generate analytics.", schema("primaryAssetId", "secondaryAssetId", "dataSourceId")),
                new ToolSpec("run_ingestion", "Load the bundled sample provider dataset.", schema()),
                new ToolSpec("run_analytics", "Run yearly aggregation and next-close prediction.", schema("assetId", "dataSourceId"))
        );
    }

    public AssistantResponse answer(String message) {
        String prompt = message == null ? "" : message.toLowerCase();
        if (prompt.contains("compare")) {
            Object data = callTool("compare_assets", Map.of("assetIdA", "BTCUSD", "assetIdB", "ETHUSD", "dataSourceId", DEFAULT_SOURCE));
            return new AssistantResponse("Compared BTCUSD and ETHUSD using the warehouse time-series records.", data);
        }
        if (prompt.contains("trend") || prompt.contains("summarize") || prompt.contains("summary")) {
            Object data = callTool("summarize_trends", Map.of("assetId", "BTCUSD", "dataSourceId", DEFAULT_SOURCE));
            return new AssistantResponse("Computed a BTCUSD trend summary grounded in stored close prices.", data);
        }
        if (prompt.contains("analytics") || prompt.contains("prediction") || prompt.contains("forecast")) {
            Object data = callTool("run_analytics", Map.of("assetId", "BTCUSD", "dataSourceId", DEFAULT_SOURCE));
            return new AssistantResponse("Ran analytics and generated a next-close prediction from warehouse records.", data);
        }
        if (prompt.contains("brief") || prompt.contains("agent")) {
            Object data = callTool("agent_market_brief", Map.of(
                    "primaryAssetId", "BTCUSD",
                    "secondaryAssetId", "ETHUSD",
                    "dataSourceId", DEFAULT_SOURCE
            ));
            return new AssistantResponse("Prepared a multi-step market brief using only warehouse data.", data);
        }
        if (prompt.contains("source")) {
            Object data = warehouseService.listDataSources(0, 20);
            return new AssistantResponse("These are the available data sources in the warehouse.", data);
        }
        Object data = callTool("list_assets", Map.of("offset", 0, "limit", 20));
        return new AssistantResponse("These are the available financial assets in the warehouse.", data);
    }

    public Object callTool(String toolName, Map<String, Object> arguments) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        return switch (toolName) {
            case "list_assets" -> warehouseService.listAssets(intArg(args, "offset", 0), intArg(args, "limit", 20));
            case "get_asset" -> warehouseService.latestAsset(textArg(args, "assetId", "BTCUSD")).orElse(null);
            case "fetch_time_series" -> warehouseService.getTimeSeries(
                    textArg(args, "assetId", "BTCUSD"),
                    textArg(args, "dataSourceId", DEFAULT_SOURCE),
                    dateArg(args, "startBusinessDate", LocalDate.of(1900, 1, 1)),
                    dateArg(args, "endBusinessDate", LocalDate.now().plusDays(1)),
                    true,
                    null,
                    intArg(args, "offset", 0),
                    intArg(args, "limit", 100)
            );
            case "summarize_trends" -> summarizeTrends(
                    textArg(args, "assetId", "BTCUSD"),
                    textArg(args, "dataSourceId", DEFAULT_SOURCE),
                    dateArg(args, "startBusinessDate", LocalDate.of(1900, 1, 1)),
                    dateArg(args, "endBusinessDate", LocalDate.now().plusDays(1))
            );
            case "compare_assets" -> compareAssets(
                    textArg(args, "assetIdA", "BTCUSD"),
                    textArg(args, "assetIdB", "ETHUSD"),
                    textArg(args, "dataSourceId", DEFAULT_SOURCE)
            );
            case "agent_market_brief" -> agentMarketBrief(
                    textArg(args, "primaryAssetId", "BTCUSD"),
                    textArg(args, "secondaryAssetId", "ETHUSD"),
                    textArg(args, "dataSourceId", DEFAULT_SOURCE)
            );
            case "run_ingestion" -> ingestionService.importBundledSample();
            case "run_analytics" -> analyticsService.runAnalytics(
                    textArg(args, "assetId", "BTCUSD"),
                    textArg(args, "dataSourceId", DEFAULT_SOURCE)
            );
            default -> Map.of("error", "Unknown tool: " + toolName);
        };
    }

    private Map<String, Object> summarizeTrends(String assetId, String dataSourceId, LocalDate start, LocalDate end) {
        List<TimeSeriesData> records = warehouseService.findLatestTimeSeries(assetId, dataSourceId, start, end, null);
        DoubleSummaryStatistics stats = records.stream()
                .flatMap(record -> closeValue(record).stream().mapToObj(Double::valueOf))
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("assetId", assetId);
        result.put("dataSourceId", dataSourceId);
        result.put("recordCount", stats.getCount());
        if (stats.getCount() > 0) {
            result.put("minClose", stats.getMin());
            result.put("maxClose", stats.getMax());
            result.put("averageClose", stats.getAverage());
            result.put("latestClose", latestClose(records));
            result.put("periodChange", periodChange(records));
        }
        return result;
    }

    private Map<String, Object> compareAssets(String assetIdA, String assetIdB, String dataSourceId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSourceId", dataSourceId);
        result.put(assetIdA, summarizeTrends(assetIdA, dataSourceId, LocalDate.of(1900, 1, 1), LocalDate.now().plusDays(1)));
        result.put(assetIdB, summarizeTrends(assetIdB, dataSourceId, LocalDate.of(1900, 1, 1), LocalDate.now().plusDays(1)));
        return result;
    }

    private Map<String, Object> agentMarketBrief(String primaryAssetId, String secondaryAssetId, String dataSourceId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("goal", "Create a grounded market brief from warehouse data");
        result.put("plan", List.of(
                "Discover available assets",
                "Summarize the primary asset trend",
                "Summarize the secondary asset trend",
                "Compare both assets",
                "Run analytics and next-close prediction for the primary asset"
        ));
        result.put("availableAssets", warehouseService.listAssets(0, 20));
        result.put("primaryTrend", summarizeTrends(primaryAssetId, dataSourceId, LocalDate.of(1900, 1, 1), LocalDate.now().plusDays(1)));
        result.put("secondaryTrend", summarizeTrends(secondaryAssetId, dataSourceId, LocalDate.of(1900, 1, 1), LocalDate.now().plusDays(1)));
        result.put("comparison", compareAssets(primaryAssetId, secondaryAssetId, dataSourceId));
        result.put("analytics", analyticsService.runAnalytics(primaryAssetId, dataSourceId));
        result.put("grounding", "All values come from MongoDB warehouse records exposed through the data access layer.");
        return result;
    }

    private Double latestClose(List<TimeSeriesData> records) {
        return records.stream()
                .max(Comparator.comparing(TimeSeriesData::getBusinessDate))
                .flatMap(record -> closeValue(record).stream().boxed().findFirst())
                .orElse(null);
    }

    private Double periodChange(List<TimeSeriesData> records) {
        List<TimeSeriesData> sorted = records.stream()
                .sorted(Comparator.comparing(TimeSeriesData::getBusinessDate))
                .toList();
        if (sorted.size() < 2) {
            return null;
        }
        Double first = closeValue(sorted.get(0)).stream().boxed().findFirst().orElse(null);
        Double last = closeValue(sorted.get(sorted.size() - 1)).stream().boxed().findFirst().orElse(null);
        return first == null || last == null ? null : last - first;
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

    private Map<String, Object> schema(String... fields) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : fields) {
            properties.put(field, Map.of("type", "string"));
        }
        return Map.of(
                "type", "object",
                "properties", properties
        );
    }

    private String textArg(Map<String, Object> args, String key, String defaultValue) {
        Object value = args.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int intArg(Map<String, Object> args, String key, int defaultValue) {
        Object value = args.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private LocalDate dateArg(Map<String, Object> args, String key, LocalDate defaultValue) {
        Object value = args.get(key);
        return value == null || String.valueOf(value).isBlank()
                ? defaultValue
                : LocalDate.parse(String.valueOf(value));
    }
}
