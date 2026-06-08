package com.example.financialdatawarehouse.dto;

import java.util.Map;

public record AssistantRequest(
        String message,
        String toolName,
        Map<String, Object> arguments
) {
}
