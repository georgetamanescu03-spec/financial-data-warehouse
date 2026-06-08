package com.example.financialdatawarehouse.repository;

import com.example.financialdatawarehouse.model.AnalyticsSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalyticsSummaryRepository extends MongoRepository<AnalyticsSummary, String> {
    List<AnalyticsSummary> findByAssetIdAndDataSourceIdOrderByBusinessYearDescComputedAtDesc(String assetId, String dataSourceId);
}
