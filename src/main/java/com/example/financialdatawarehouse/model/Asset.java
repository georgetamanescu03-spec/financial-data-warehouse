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
@Document(collection = "assets")
@CompoundIndex(name = "asset_latest_idx", def = "{'assetId': 1, 'systemDate': -1}")
public class Asset {
    @Id
    private String documentId;

    @Indexed
    private String assetId;

    private String symbol;
    private String assetClass;
    private String region;
    private String name;
    private String description;

    private LocalDate validFrom;
    private Instant systemDate;
    private boolean deletedMarker;

    @Indexed
    private String versionHash;

    private Map<String, Object> attributes;
}
