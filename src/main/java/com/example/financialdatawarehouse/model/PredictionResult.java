package com.example.financialdatawarehouse.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Document(collection = "analytics_predictions")
@CompoundIndex(name = "prediction_lookup_idx", def = "{'assetId': 1, 'dataSourceId': 1, 'computedAt': -1}")
public class PredictionResult {
    @Id
    private String documentId;

    private String assetId;
    private String dataSourceId;
    private LocalDate predictedBusinessDate;
    private Double predictedClose;
    private String method;
    private Instant computedAt;
}
