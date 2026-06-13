package com.company.chatbot.controller;

import com.company.chatbot.model.ChatRequest;
import com.company.chatbot.model.ChatResponse;
import com.company.chatbot.service.McpClientService;
import com.company.chatbot.service.VertexAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final VertexAiService  vertexAiService;
    private final McpClientService mcpClientService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(ChatResponse.error("Message cannot be empty"));
        }

        log.info("Chat request: {}", request.getMessage().substring(0, Math.min(80, request.getMessage().length())));

        // 1. Fetch context via MCP tools
        String jargonContext = mcpClientService.getJargonContext(request.getMessage());
        String dataContext   = mcpClientService.getRelevantData(request.getMessage());

        // 2. Build RAG-enhanced system prompt
        String systemPrompt = buildSystemPrompt(jargonContext, dataContext);

        // 3. Call Vertex AI (Gemini)
        String answer = vertexAiService.generate(systemPrompt, request.getMessage(), request.getHistory());

        return ResponseEntity.ok(ChatResponse.ok(answer));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "chatbot-api"));
    }

    private String buildSystemPrompt(String jargonContext, String dataContext) {
        return """
            You are a helpful AI assistant for our internal systems.

            INSTRUCTIONS:
            - Always expand acronyms and jargon terms using the jargon dictionary provided below.
            - Answer only based on the provided context data. Do not hallucinate.
            - If you cannot find an answer in the context, say "I don't have enough information to answer that."
            - Be concise and factual. Cite the relevant data when applicable.

            JARGON DICTIONARY:
            %s

            SYSTEM CONTEXT:
            %s
            """.formatted(jargonContext, dataContext);
    }
}
