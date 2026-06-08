package com.example.financialdatawarehouse.config;

import com.example.financialdatawarehouse.ingestion.IngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataWarehouseConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataWarehouseConfig.class);

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    CommandLineRunner seedSampleData(
            IngestionService ingestionService,
            @Value("${warehouse.seed-sample-data:true}") boolean seedSampleData
    ) {
        return args -> {
            if (!seedSampleData) {
                return;
            }
            LOGGER.info("Seeding bundled financial market sample data");
            LOGGER.info("Ingestion result: {}", ingestionService.importBundledSample());
        };
    }
}
