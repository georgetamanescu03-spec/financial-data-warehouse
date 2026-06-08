package com.example.financialdatawarehouse.repository;

import com.example.financialdatawarehouse.model.TimeSeriesData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSeriesDataRepository extends MongoRepository<TimeSeriesData, String> {
    List<TimeSeriesData> findByAssetIdAndDataSourceIdOrderByBusinessDateDescSystemDateDesc(String assetId, String dataSourceId);

    boolean existsByAssetIdAndDataSourceIdAndBusinessDateAndVersionHash(
            String assetId,
            String dataSourceId,
            java.time.LocalDate businessDate,
            String versionHash
    );
}
