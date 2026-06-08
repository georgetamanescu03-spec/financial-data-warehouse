package com.example.financialdatawarehouse.ingestion;

import com.example.financialdatawarehouse.dto.IngestionResult;
import com.example.financialdatawarehouse.model.Asset;
import com.example.financialdatawarehouse.model.DataSource;
import com.example.financialdatawarehouse.model.TimeSeriesData;
import com.example.financialdatawarehouse.service.Hashing;
import com.example.financialdatawarehouse.service.WarehouseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class IngestionService {
    private static final String SAMPLE_DATASET = "classpath:data/sample-market-data.json";

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final WarehouseService warehouseService;

    public IngestionService(ObjectMapper objectMapper, ResourceLoader resourceLoader, WarehouseService warehouseService) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.warehouseService = warehouseService;
    }

    public IngestionResult importBundledSample() {
        Instant startedAt = Instant.now();
        String jobId = "sample-" + UUID.randomUUID();
        List<String> failures = new ArrayList<>();
        int fetchedRecords = 0;
        int transformedRecords = 0;
        int storedAssets = 0;
        int storedDataSources = 0;
        int storedTimeSeries = 0;
        int skippedDuplicates = 0;

        try {
            SampleMarketDataset dataset = extractSampleDataset();
            fetchedRecords = dataset.assets().stream()
                    .mapToInt(asset -> asset.points() == null ? 0 : asset.points().size())
                    .sum();

            Set<String> attributes = collectProviderAttributes(dataset);
            String sourceVersionHash = hashDataSource(dataset, attributes);
            if (warehouseService.dataSourceVersionExists(dataset.dataSourceId(), sourceVersionHash)) {
                skippedDuplicates++;
            } else {
                warehouseService.saveDataSource(toDataSource(dataset, attributes, sourceVersionHash));
                storedDataSources++;
            }

            for (SampleMarketDataset.SampleAsset sampleAsset : dataset.assets()) {
                try {
                    String assetVersionHash = hashAsset(sampleAsset);
                    if (warehouseService.assetVersionExists(sampleAsset.assetId(), assetVersionHash)) {
                        skippedDuplicates++;
                    } else {
                        warehouseService.saveAsset(toAsset(sampleAsset, assetVersionHash));
                        storedAssets++;
                    }

                    for (SampleMarketDataset.SamplePoint point : sampleAsset.points()) {
                        transformedRecords++;
                        String pointVersionHash = hashPoint(dataset.dataSourceId(), sampleAsset.assetId(), point);
                        if (warehouseService.timeSeriesVersionExists(
                                sampleAsset.assetId(),
                                dataset.dataSourceId(),
                                point.businessDate(),
                                pointVersionHash
                        )) {
                            skippedDuplicates++;
                            continue;
                        }
                        warehouseService.saveTimeSeries(toTimeSeries(
                                dataset.dataSourceId(),
                                sampleAsset.assetId(),
                                point,
                                jobId,
                                pointVersionHash
                        ));
                        storedTimeSeries++;
                    }
                } catch (RuntimeException exception) {
                    failures.add(sampleAsset.assetId() + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            failures.add("Could not read bundled sample data: " + exception.getMessage());
        }

        return new IngestionResult(
                jobId,
                startedAt,
                Instant.now(),
                fetchedRecords,
                transformedRecords,
                storedAssets,
                storedDataSources,
                storedTimeSeries,
                skippedDuplicates,
                failures
        );
    }

    private SampleMarketDataset extractSampleDataset() throws IOException {
        Resource resource = resourceLoader.getResource(SAMPLE_DATASET);
        try (InputStream inputStream = resource.getInputStream()) {
            return objectMapper.readValue(inputStream, SampleMarketDataset.class);
        }
    }

    private DataSource toDataSource(SampleMarketDataset dataset, Set<String> attributes, String versionHash) {
        DataSource source = new DataSource();
        source.setDocumentId(dataset.dataSourceId() + "::" + Hashing.shortHash(versionHash));
        source.setDataSourceId(dataset.dataSourceId());
        source.setProvider(dataset.provider());
        source.setName(dataset.name());
        source.setDescription(dataset.description());
        source.setBaseUrl(dataset.baseUrl());
        source.setValidFrom(LocalDate.now());
        source.setSystemDate(Instant.now());
        source.setDeletedMarker(false);
        source.setVersionHash(versionHash);
        source.setProvenance(dataset.provenance() == null ? Map.of() : new LinkedHashMap<>(dataset.provenance()));
        source.setAttributes(new LinkedHashSet<>(attributes));
        return source;
    }

    private Asset toAsset(SampleMarketDataset.SampleAsset sampleAsset, String versionHash) {
        Asset asset = new Asset();
        asset.setDocumentId(sampleAsset.assetId() + "::" + Hashing.shortHash(versionHash));
        asset.setAssetId(sampleAsset.assetId());
        asset.setSymbol(sampleAsset.symbol());
        asset.setAssetClass(sampleAsset.assetClass());
        asset.setRegion(sampleAsset.region());
        asset.setName(sampleAsset.name());
        asset.setDescription(sampleAsset.description());
        asset.setValidFrom(LocalDate.now());
        asset.setSystemDate(Instant.now());
        asset.setDeletedMarker(false);
        asset.setVersionHash(versionHash);
        asset.setAttributes(sampleAsset.attributes() == null ? Map.of() : new LinkedHashMap<>(sampleAsset.attributes()));
        return asset;
    }

    private TimeSeriesData toTimeSeries(
            String dataSourceId,
            String assetId,
            SampleMarketDataset.SamplePoint point,
            String jobId,
            String versionHash
    ) {
        TimeSeriesData record = new TimeSeriesData();
        record.setDocumentId(assetId + "::" + dataSourceId + "::" + point.businessDate() + "::" + Hashing.shortHash(versionHash));
        record.setAssetId(assetId);
        record.setDataSourceId(dataSourceId);
        record.setBusinessDate(point.businessDate());
        record.setBusinessYear(point.businessDate().getYear());
        record.setSystemDate(Instant.now());
        record.setIngestionJobId(jobId);
        record.setSourceReference(point.sourceReference());
        record.setVersionHash(versionHash);
        record.setValues(point.values() == null ? Map.of() : new LinkedHashMap<>(point.values()));
        record.setDeletedMarker(false);
        return record;
    }

    private Set<String> collectProviderAttributes(SampleMarketDataset dataset) {
        Set<String> attributes = new TreeSet<>();
        for (SampleMarketDataset.SampleAsset asset : dataset.assets()) {
            if (asset.points() == null) {
                continue;
            }
            for (SampleMarketDataset.SamplePoint point : asset.points()) {
                if (point.values() != null) {
                    attributes.addAll(point.values().keySet());
                }
            }
        }
        return attributes;
    }

    private String hashDataSource(SampleMarketDataset dataset, Set<String> attributes) {
        return Hashing.sha256(dataset.dataSourceId()
                + "|" + dataset.provider()
                + "|" + dataset.name()
                + "|" + dataset.description()
                + "|" + new TreeSet<>(attributes)
                + "|" + sortedMap(dataset.provenance()));
    }

    private String hashAsset(SampleMarketDataset.SampleAsset asset) {
        return Hashing.sha256(asset.assetId()
                + "|" + asset.symbol()
                + "|" + asset.assetClass()
                + "|" + asset.region()
                + "|" + asset.name()
                + "|" + asset.description()
                + "|" + sortedMap(asset.attributes()));
    }

    private String hashPoint(String dataSourceId, String assetId, SampleMarketDataset.SamplePoint point) {
        return Hashing.sha256(dataSourceId
                + "|" + assetId
                + "|" + point.businessDate()
                + "|" + point.sourceReference()
                + "|" + sortedMap(point.values()));
    }

    private Map<String, Object> sortedMap(Map<String, Object> values) {
        return values == null ? Map.of() : new TreeMap<>(values);
    }
}
