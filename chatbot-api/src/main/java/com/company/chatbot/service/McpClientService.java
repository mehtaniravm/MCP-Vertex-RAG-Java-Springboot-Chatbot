package com.company.chatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;

/**
 * Calls the MCP Server to fetch context for the AI prompt.
 * The MCP Server exposes an HTTP endpoint that proxies tool calls.
 */
@Slf4j
@Service
public class McpClientService {

    private final WebClient webClient;

    public McpClientService(@Value("${mcp.server-url}") String mcpUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(mcpUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    /**
     * Call a named MCP tool and get its text result.
     * The MCP Server handles routing to postgres, jargon, or biz-data tools.
     */
    public String callTool(String toolName, Map<String, Object> params) {
        try {
            Map<String, Object> body = Map.of("name", toolName, "arguments", params);
            var result = webClient.post()
                .uri("/tools/call")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            if (result != null && result.containsKey("content")) {
                var content = (java.util.List<?>) result.get("content");
                if (!content.isEmpty()) {
                    var first = (Map<?, ?>) content.get(0);
                    return (String) first.getOrDefault("text", "");
                }
            }
            return "";
        } catch (Exception e) {
            log.warn("MCP tool call failed [{}]: {}", toolName, e.getMessage());
            return "";
        }
    }

    public String getJargonContext(String message) {
        return callTool("get_jargon_dictionary", Map.of());
    }

    public String getRelevantData(String message) {
        // Fetch active events and SLA status as context
        String events = callTool("get_active_events", Map.of("lastHours", 24));
        String sla    = callTool("get_sla_status",    Map.of());
        return "ACTIVE EVENTS:\n" + events + "\n\nSLA STATUS:\n" + sla;
    }
}
