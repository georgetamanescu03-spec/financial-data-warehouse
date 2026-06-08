package com.example.financialdatawarehouse.dto;

import java.util.List;
import java.util.Set;

public record TimeSeriesResponse(
        String assetId,
        String dataSourceId,
        List<TimeSeriesRecordResponse> records,
        Set<String> attributes,
        int count
) {
}
