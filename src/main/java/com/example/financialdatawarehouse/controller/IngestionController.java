package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.dto.IngestionResult;
import com.example.financialdatawarehouse.ingestion.IngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {
    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/sample")
    public IngestionResult ingestSampleData() {
        return ingestionService.importBundledSample();
    }
}
