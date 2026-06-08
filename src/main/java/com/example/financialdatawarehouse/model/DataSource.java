package com.example.financialdatawarehouse.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Data
@Document(collection = "data_sources")
@CompoundIndex(name = "source_latest_idx", def = "{'dataSourceId': 1, 'systemDate': -1}")
public class DataSource {
    @Id
    private String documentId;

    @Indexed
    private String dataSourceId;

    private String name;
    private String provider;
    private String description;
    private String baseUrl;

    private LocalDate validFrom;
    private Instant systemDate;
    private boolean deletedMarker;

    @Indexed
    private String versionHash;

    private Map<String, Object> provenance;
    private Set<String> attributes;
}
