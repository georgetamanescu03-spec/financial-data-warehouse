package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.dto.DataSourceSummary;
import com.example.financialdatawarehouse.dto.PageResponse;
import com.example.financialdatawarehouse.model.DataSource;
import com.example.financialdatawarehouse.service.WarehouseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class DataSourceController {
    private final WarehouseService warehouseService;

    public DataSourceController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/data-sources")
    public PageResponse<DataSourceSummary> listDataSources(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return warehouseService.listDataSources(offset, Math.min(limit, 500));
    }

    @GetMapping("/data-sources/**")
    public DataSource getDataSource(HttpServletRequest request) {
        String dataSourceId = RequestPathUtils.trailingPath(request, "/api/v1/data-sources/");
        return warehouseService.latestDataSource(dataSourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Data source not found: " + dataSourceId));
    }

    @GetMapping("/data-source-history/**")
    public List<DataSource> getDataSourceHistory(HttpServletRequest request) {
        String dataSourceId = RequestPathUtils.trailingPath(request, "/api/v1/data-source-history/");
        return warehouseService.dataSourceHistory(dataSourceId);
    }
}
