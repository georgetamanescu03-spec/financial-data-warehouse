package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.assistant.AssistantService;
import com.example.financialdatawarehouse.dto.McpJsonRpcRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class McpController {
    private final AssistantService assistantService;
    private final ObjectMapper objectMapper;

    public McpController(AssistantService assistantService, ObjectMapper objectMapper) {
        this.assistantService = assistantService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/mcp")
    public Map<String, Object> handle(@RequestBody McpJsonRpcRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", request.jsonrpc() == null ? "2.0" : request.jsonrpc());
        response.put("id", request.id());

        if ("initialize".equals(request.method())) {
            response.put("result", Map.of(
                    "protocolVersion", "2024-11-05",
                    "serverInfo", Map.of(
                            "name", "financial-data-warehouse-mcp",
                            "version", "1.0.0"
                    ),
                    "capabilities", Map.of(
                            "tools", Map.of("listChanged", false)
                    )
            ));
            return response;
        }

        if ("ping".equals(request.method())) {
            response.put("result", Map.of());
            return response;
        }

        if ("notifications/initialized".equals(request.method())) {
            response.put("result", Map.of("status", "initialized"));
            return response;
        }

        if ("tools/list".equals(request.method())) {
            response.put("result", Map.of("tools", assistantService.listTools()));
            return response;
        }

        if ("tools/call".equals(request.method())) {
            String toolName = request.params() == null ? "" : String.valueOf(request.params().get("name"));
            Map<String, Object> arguments = mapArg(request.params() == null ? null : request.params().get("arguments"));
            Object toolResult = assistantService.callTool(toolName, arguments);
            response.put("result", mcpToolResult(toolResult));
            return response;
        }

        response.put("error", Map.of(
                "code", -32601,
                "message", "Unsupported MCP method: " + request.method()
        ));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArg(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Map<String, Object> mcpToolResult(Object toolResult) {
        String text = toJson(toolResult);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of(Map.of(
                "type", "text",
                "text", text
        )));
        result.put("structuredContent", toolResult);
        result.put("isError", false);
        return result;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }
}
