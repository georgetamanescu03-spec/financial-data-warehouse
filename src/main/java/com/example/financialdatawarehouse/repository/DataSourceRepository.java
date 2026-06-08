package com.example.financialdatawarehouse.repository;

import com.example.financialdatawarehouse.model.DataSource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataSourceRepository extends MongoRepository<DataSource, String> {
    List<DataSource> findByDataSourceIdOrderBySystemDateDesc(String dataSourceId);

    Optional<DataSource> findFirstByDataSourceIdOrderBySystemDateDesc(String dataSourceId);

    boolean existsByDataSourceIdAndVersionHash(String dataSourceId, String versionHash);
}
