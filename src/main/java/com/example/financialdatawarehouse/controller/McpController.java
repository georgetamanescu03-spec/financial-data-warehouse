package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.assistant.AssistantService;
import com.example.financialdatawarehouse.dto.McpJsonRpcRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class McpController {
    private final AssistantService assistantService;

    public McpController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/mcp")
    public Map<String, Object> handle(@RequestBody McpJsonRpcRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", request.jsonrpc() == null ? "2.0" : request.jsonrpc());
        response.put("id", request.id());

        if ("tools/list".equals(request.method())) {
            response.put("result", Map.of("tools", assistantService.listTools()));
            return response;
        }

        if ("tools/call".equals(request.method())) {
            String toolName = request.params() == null ? "" : String.valueOf(request.params().get("name"));
            Map<String, Object> arguments = mapArg(request.params() == null ? null : request.params().get("arguments"));
            response.put("result", assistantService.callTool(toolName, arguments));
            return response;
        }

        response.put("error", Map.of("message", "Unsupported MCP method: " + request.method()));
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapArg(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }
}
