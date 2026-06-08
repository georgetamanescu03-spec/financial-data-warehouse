package com.example.financialdatawarehouse.repository;

import com.example.financialdatawarehouse.model.Asset;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends MongoRepository<Asset, String> {
    List<Asset> findByAssetIdOrderBySystemDateDesc(String assetId);

    Optional<Asset> findFirstByAssetIdOrderBySystemDateDesc(String assetId);

    boolean existsByAssetIdAndVersionHash(String assetId, String versionHash);
}
