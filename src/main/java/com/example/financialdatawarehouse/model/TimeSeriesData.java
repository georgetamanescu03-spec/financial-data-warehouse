package com.example.financialdatawarehouse.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

@Data
@Document(collection = "time_series")
@CompoundIndex(name = "series_partition_idx", def = "{'assetId': 1, 'dataSourceId': 1, 'businessDate': -1, 'systemDate': -1}")
public class TimeSeriesData {
    @Id
    private String documentId;

    @Indexed
    private String assetId;
    @Indexed
    private String dataSourceId;

    private LocalDate businessDate;
    private Integer businessYear;
    private Instant systemDate;

    private String ingestionJobId;
    private String sourceReference;

    @Indexed
    private String versionHash;

    private Map<String, Object> values;
    private boolean deletedMarker;
}
