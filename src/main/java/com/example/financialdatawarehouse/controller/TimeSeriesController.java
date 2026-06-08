package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.dto.TimeSeriesResponse;
import com.example.financialdatawarehouse.service.WarehouseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class TimeSeriesController {
    private final WarehouseService warehouseService;

    public TimeSeriesController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/data")
    public TimeSeriesResponse getTimeSeries(
            @RequestParam String assetId,
            @RequestParam String dataSourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startBusinessDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endBusinessDate,
            @RequestParam(defaultValue = "false") boolean includeAttributes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant asOfSystemTime,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit
    ) {
        return warehouseService.getTimeSeries(
                assetId,
                dataSourceId,
                startBusinessDate,
                endBusinessDate,
                includeAttributes,
                asOfSystemTime,
                offset,
                Math.min(limit, 1000)
        );
    }
}
