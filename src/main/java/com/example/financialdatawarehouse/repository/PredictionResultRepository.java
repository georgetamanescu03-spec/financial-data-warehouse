package com.example.financialdatawarehouse.repository;

import com.example.financialdatawarehouse.model.PredictionResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PredictionResultRepository extends MongoRepository<PredictionResult, String> {
    List<PredictionResult> findByAssetIdAndDataSourceIdOrderByComputedAtDesc(String assetId, String dataSourceId);
}
