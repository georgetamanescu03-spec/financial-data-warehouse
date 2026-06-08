package com.example.financialdatawarehouse.controller;

import com.example.financialdatawarehouse.assistant.AssistantService;
import com.example.financialdatawarehouse.assistant.ToolSpec;
import com.example.financialdatawarehouse.dto.AssistantRequest;
import com.example.financialdatawarehouse.dto.AssistantResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @GetMapping("/tools")
    public List<ToolSpec> listTools() {
        return assistantService.listTools();
    }

    @PostMapping("/chat")
    public AssistantResponse chat(@RequestBody AssistantRequest request) {
        if (request.toolName() != null && !request.toolName().isBlank()) {
            Object data = assistantService.callTool(request.toolName(), request.arguments());
            return new AssistantResponse("Tool executed: " + request.toolName(), data);
        }
        return assistantService.answer(request.message());
    }
}
