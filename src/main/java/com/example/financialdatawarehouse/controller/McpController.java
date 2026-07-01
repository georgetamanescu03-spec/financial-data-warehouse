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
        if (request == null) {
            return error(null, -32600, "Invalid JSON-RPC request body.");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", request.jsonrpc() == null ? "2.0" : request.jsonrpc());
        response.put("id", request.id());

        if (request.jsonrpc() != null && !"2.0".equals(request.jsonrpc())) {
            return error(request.id(), -32600, "Only JSON-RPC 2.0 requests are supported.");
        }

        if (request.method() == null || request.method().isBlank()) {
            return error(request.id(), -32600, "Missing required JSON-RPC method.");
        }

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
            if (toolName.isBlank() || "null".equals(toolName)) {
                return error(request.id(), -32602, "Missing required MCP tool name.");
            }
            if (!assistantService.hasTool(toolName)) {
                return error(request.id(), -32602, "Unknown MCP tool name: " + toolName);
            }
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

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("error", Map.of(
                "code", code,
                "message", message
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
