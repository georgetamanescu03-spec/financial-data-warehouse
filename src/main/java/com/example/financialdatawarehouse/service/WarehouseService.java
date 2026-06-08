package com.example.financialdatawarehouse.service;

import com.example.financialdatawarehouse.dto.AssetSummary;
import com.example.financialdatawarehouse.dto.DataSourceSummary;
import com.example.financialdatawarehouse.dto.PageResponse;
import com.example.financialdatawarehouse.dto.TimeSeriesRecordResponse;
import com.example.financialdatawarehouse.dto.TimeSeriesResponse;
import com.example.financialdatawarehouse.model.Asset;
import com.example.financialdatawarehouse.model.DataSource;
import com.example.financialdatawarehouse.model.TimeSeriesData;
import com.example.financialdatawarehouse.repository.AssetRepository;
import com.example.financialdatawarehouse.repository.DataSourceRepository;
import com.example.financialdatawarehouse.repository.TimeSeriesDataRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class WarehouseService {
    private final AssetRepository assetRepository;
    private final DataSourceRepository dataSourceRepository;
    private final TimeSeriesDataRepository timeSeriesDataRepository;
    private final MongoTemplate mongoTemplate;

    public WarehouseService(
            AssetRepository assetRepository,
            DataSourceRepository dataSourceRepository,
            TimeSeriesDataRepository timeSeriesDataRepository,
            MongoTemplate mongoTemplate
    ) {
        this.assetRepository = assetRepository;
        this.dataSourceRepository = dataSourceRepository;
        this.timeSeriesDataRepository = timeSeriesDataRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public PageResponse<AssetSummary> listAssets(int offset, int limit) {
        List<Asset> latestAssets = latestAssets();
        List<AssetSummary> summaries = latestAssets.stream()
                .map(this::toAssetSummary)
                .toList();
        return page(summaries, offset, limit);
    }

    public Optional<Asset> latestAsset(String assetId) {
        return assetRepository.findFirstByAssetIdOrderBySystemDateDesc(assetId)
                .filter(asset -> !asset.isDeletedMarker());
    }

    public List<Asset> assetHistory(String assetId) {
        return assetRepository.findByAssetIdOrderBySystemDateDesc(assetId);
    }

    public PageResponse<DataSourceSummary> listDataSources(int offset, int limit) {
        List<DataSource> latestSources = latestDataSources();
        List<DataSourceSummary> summaries = latestSources.stream()
                .map(this::toDataSourceSummary)
                .toList();
        return page(summaries, offset, limit);
    }

    public Optional<DataSource> latestDataSource(String dataSourceId) {
        return dataSourceRepository.findFirstByDataSourceIdOrderBySystemDateDesc(dataSourceId)
                .filter(source -> !source.isDeletedMarker());
    }

    public List<DataSource> dataSourceHistory(String dataSourceId) {
        return dataSourceRepository.findByDataSourceIdOrderBySystemDateDesc(dataSourceId);
    }

    public TimeSeriesResponse getTimeSeries(
            String assetId,
            String dataSourceId,
            LocalDate startBusinessDate,
            LocalDate endBusinessDate,
            boolean includeAttributes,
            Instant asOfSystemTime,
            int offset,
            int limit
    ) {
        List<TimeSeriesData> latestRecords = findLatestTimeSeries(
                assetId,
                dataSourceId,
                startBusinessDate,
                endBusinessDate,
                asOfSystemTime
        );
        List<TimeSeriesData> window = slice(latestRecords, offset, limit);
        List<TimeSeriesRecordResponse> responseRecords = window.stream()
                .map(record -> new TimeSeriesRecordResponse(
                        record.getBusinessDate(),
                        record.getSystemDate(),
                        record.getValues()
                ))
                .toList();
        Set<String> attributes = includeAttributes ? collectAttributes(latestRecords) : Set.of();
        return new TimeSeriesResponse(assetId, dataSourceId, responseRecords, attributes, responseRecords.size());
    }

    public List<TimeSeriesData> findLatestTimeSeries(
            String assetId,
            String dataSourceId,
            LocalDate startBusinessDate,
            LocalDate endBusinessDate,
            Instant asOfSystemTime
    ) {
        LocalDate start = startBusinessDate == null ? LocalDate.of(1900, 1, 1) : startBusinessDate;
        LocalDate end = endBusinessDate == null ? LocalDate.now().plusDays(1) : endBusinessDate;

        Criteria criteria = Criteria.where("assetId").is(assetId)
                .and("dataSourceId").is(dataSourceId)
                .and("businessDate").gte(start).lt(end);
        if (asOfSystemTime != null) {
            criteria = criteria.and("systemDate").lte(asOfSystemTime);
        }

        Query query = new Query(criteria)
                .with(Sort.by(
                        Sort.Order.desc("businessDate"),
                        Sort.Order.desc("systemDate")
                ));
        List<TimeSeriesData> versions = mongoTemplate.find(query, TimeSeriesData.class);
        return TemporalSelectors.latestPerBusinessDate(versions);
    }

    public Asset saveAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    public DataSource saveDataSource(DataSource dataSource) {
        return dataSourceRepository.save(dataSource);
    }

    public TimeSeriesData saveTimeSeries(TimeSeriesData record) {
        return timeSeriesDataRepository.save(record);
    }

    public boolean assetVersionExists(String assetId, String versionHash) {
        return assetRepository.existsByAssetIdAndVersionHash(assetId, versionHash);
    }

    public boolean dataSourceVersionExists(String dataSourceId, String versionHash) {
        return dataSourceRepository.existsByDataSourceIdAndVersionHash(dataSourceId, versionHash);
    }

    public boolean timeSeriesVersionExists(String assetId, String dataSourceId, LocalDate businessDate, String versionHash) {
        return timeSeriesDataRepository.existsByAssetIdAndDataSourceIdAndBusinessDateAndVersionHash(
                assetId,
                dataSourceId,
                businessDate,
                versionHash
        );
    }

    private List<Asset> latestAssets() {
        List<Asset> versions = assetRepository.findAll(Sort.by(
                Sort.Order.asc("assetId"),
                Sort.Order.desc("systemDate")
        ));
        Map<String, Asset> latestById = new LinkedHashMap<>();
        for (Asset asset : versions) {
            latestById.putIfAbsent(asset.getAssetId(), asset);
        }
        return latestById.values().stream()
                .filter(asset -> !asset.isDeletedMarker())
                .toList();
    }

    private List<DataSource> latestDataSources() {
        List<DataSource> versions = dataSourceRepository.findAll(Sort.by(
                Sort.Order.asc("dataSourceId"),
                Sort.Order.desc("systemDate")
        ));
        Map<String, DataSource> latestById = new LinkedHashMap<>();
        for (DataSource source : versions) {
            latestById.putIfAbsent(source.getDataSourceId(), source);
        }
        return latestById.values().stream()
                .filter(source -> !source.isDeletedMarker())
                .toList();
    }

    private AssetSummary toAssetSummary(Asset asset) {
        return new AssetSummary(
                asset.getAssetId(),
                asset.getSymbol(),
                asset.getAssetClass(),
                asset.getRegion(),
                asset.getName()
        );
    }

    private DataSourceSummary toDataSourceSummary(DataSource source) {
        return new DataSourceSummary(
                source.getDataSourceId(),
                source.getProvider(),
                source.getName(),
                source.getDescription()
        );
    }

    private <T> PageResponse<T> page(List<T> allItems, int offset, int limit) {
        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.max(limit, 1);
        List<T> items = slice(allItems, normalizedOffset, normalizedLimit);
        return new PageResponse<>(items, allItems.size(), normalizedOffset, normalizedLimit);
    }

    private <T> List<T> slice(List<T> allItems, int offset, int limit) {
        if (allItems.isEmpty()) {
            return List.of();
        }
        int from = Math.min(Math.max(offset, 0), allItems.size());
        int to = Math.min(from + Math.max(limit, 1), allItems.size());
        return new ArrayList<>(allItems.subList(from, to));
    }

    private Set<String> collectAttributes(List<TimeSeriesData> records) {
        Set<String> attributes = new LinkedHashSet<>();
        for (TimeSeriesData record : records) {
            if (record.getValues() != null) {
                attributes.addAll(record.getValues().keySet());
            }
        }
        return attributes;
    }
}
