package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.dto.AssetSummary;
import com.example.financialdatawarehouse.dto.PageResponse;
import com.example.financialdatawarehouse.model.Asset;
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
public class AssetController {
    private final WarehouseService warehouseService;

    public AssetController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping("/assets")
    public PageResponse<AssetSummary> listAssets(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return warehouseService.listAssets(offset, Math.min(limit, 500));
    }

    @GetMapping("/assets/**")
    public Asset getAsset(HttpServletRequest request) {
        String assetId = RequestPathUtils.trailingPath(request, "/api/v1/assets/");
        return warehouseService.latestAsset(assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + assetId));
    }

    @GetMapping("/asset-history/**")
    public List<Asset> getAssetHistory(HttpServletRequest request) {
        String assetId = RequestPathUtils.trailingPath(request, "/api/v1/asset-history/");
        return warehouseService.assetHistory(assetId);
    }
}
