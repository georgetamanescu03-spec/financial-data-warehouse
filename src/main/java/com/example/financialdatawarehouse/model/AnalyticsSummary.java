package com.example.financialdatawarehouse.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "analytics_yearly_summaries")
@CompoundIndex(name = "summary_lookup_idx", def = "{'assetId': 1, 'dataSourceId': 1, 'businessYear': -1, 'computedAt': -1}")
public class AnalyticsSummary {
    @Id
    private String documentId;

    private String assetId;
    private String dataSourceId;
    private Integer businessYear;
    private long recordCount;
    private Double minClose;
    private Double maxClose;
    private Double averageClose;
    private Instant computedAt;
}
