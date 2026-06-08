package com.example.financialdatawarehouse.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record TimeSeriesRecordResponse(
        LocalDate businessDate,
        Instant systemDate,
        Map<String, Object> values
) {
}
