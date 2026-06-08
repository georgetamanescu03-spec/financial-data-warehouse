package com.example.financialdatawarehouse.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        long total,
        int offset,
        int limit
) {
}
