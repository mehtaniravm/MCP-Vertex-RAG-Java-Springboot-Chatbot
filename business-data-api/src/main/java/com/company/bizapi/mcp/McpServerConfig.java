package com.company.bizapi.mcp;

import com.company.bizapi.model.dto.*;
import com.company.bizapi.service.BusinessDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.SseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;
import java.util.Map;

/**
 * OPTION B: Spring Boot as MCP Server
 * =====================================================================
 * Exposes a /mcp SSE endpoint that speaks the MCP protocol (JSON-RPC 2.0).
 * The AI client connects here directly — no Node.js MCP layer for biz data.
 *
 * Activate: set env var ENABLE_MCP_ENDPOINT=true (app.mcp.enabled=true)
 * Auth:     API_KEY_ENABLED=true  → ApiKeyFilter guards /mcp
 *           API_KEY_ENABLED=false → open (safe with --ingress internal)
 *
 * Required pom.xml dependency:
 *   <groupId>io.modelcontextprotocol.sdk</groupId>
 *   <artifactId>mcp-spring-webmvc</artifactId>
 *   <version>0.9.0</version>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mcp.enabled", havingValue = "true")
public class McpServerConfig {

    private final BusinessDataService service;
    private final ObjectMapper        mapper;

    @Bean
    public McpSyncServer mcpSyncServer() {
        log.info("MCP Server endpoint ENABLED at /mcp");
        return McpSyncServer.builder()
            .serverInfo(new Implementation("biz-data-mcp", "1.0.0"))
            .capabilities(ServerCapabilities.builder().tools(true).build())
            .tools(buildTools())
            .build();
    }

    @Bean
    public SseServerTransportProvider sseTransportProvider(McpSyncServer server) {
        return new SseServerTransportProvider(mapper, "/mcp");
    }

    @Bean
    public RouterFunction<ServerResponse> mcpRoutes(SseServerTransportProvider provider) {
        return provider.getRouterFunction();
    }

    private List<SyncToolRegistration> buildTools() {
        return List.of(
            buildEntitySummaryTool(),
            buildKpiReportTool(),
            buildActiveEventsTool(),
            buildSlaStatusTool(),
            buildBusinessSearchTool()
        );
    }

    private SyncToolRegistration buildEntitySummaryTool() {
        var schema = new McpSchema.JsonSchema("object",
            Map.of("entityCode", Map.of("type", "string",
                "description", "Entity code e.g. ENT-001")),
            List.of("entityCode"));
        var toolDef = new Tool("get_entity_summary",
            "Get enriched entity summary with computed health score and active alerts", schema);
        return new SyncToolRegistration(toolDef, args -> {
            try {
                String code   = (String) args.get("entityCode");
                EntitySummaryDto result = service.getEntitySummary(code);
                return new CallToolResult(List.of(new TextContent(mapper.writeValueAsString(result))), false);
            } catch (Exception e) {
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }

    private SyncToolRegistration buildKpiReportTool() {
        var schema = new McpSchema.JsonSchema("object",
            Map.of(
                "from",     Map.of("type", "string", "description", "Start date YYYY-MM-DD"),
                "to",       Map.of("type", "string", "description", "End date YYYY-MM-DD"),
                "category", Map.of("type", "string", "description", "Entity category (optional)")
            ),
            List.of("from", "to"));
        var toolDef = new Tool("get_kpi_report",
            "Get aggregated KPI metrics for a date range", schema);
        return new SyncToolRegistration(toolDef, args -> {
            try {
                String from     = (String) args.get("from");
                String to       = (String) args.get("to");
                String category = (String) args.getOrDefault("category", null);
                KpiReportDto result = service.getKpiReport(from, to, category);
                return new CallToolResult(List.of(new TextContent(mapper.writeValueAsString(result))), false);
            } catch (Exception e) {
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }

    private SyncToolRegistration buildActiveEventsTool() {
        var schema = new McpSchema.JsonSchema("object",
            Map.of(
                "severity",  Map.of("type", "string", "description", "INFO, WARNING, or CRITICAL (optional)"),
                "lastHours", Map.of("type", "integer", "description", "Hours to look back (default 24)")
            ),
            List.of());
        var toolDef = new Tool("get_active_events",
            "Get open events with urgency classification", schema);
        return new SyncToolRegistration(toolDef, args -> {
            try {
                String severity  = (String) args.getOrDefault("severity", null);
                int    lastHours = ((Number) args.getOrDefault("lastHours", 24)).intValue();
                List<EventDto> result = service.getActiveEvents(severity, lastHours);
                return new CallToolResult(List.of(new TextContent(mapper.writeValueAsString(result))), false);
            } catch (Exception e) {
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }

    private SyncToolRegistration buildSlaStatusTool() {
        var schema = new McpSchema.JsonSchema("object",
            Map.of("entityCode", Map.of("type", "string", "description", "Specific entity code (optional, omit for all)")),
            List.of());
        var toolDef = new Tool("get_sla_status",
            "Get SLA compliance status with breach risk score", schema);
        return new SyncToolRegistration(toolDef, args -> {
            try {
                String code   = (String) args.getOrDefault("entityCode", null);
                List<SlaStatusDto> result = service.getSlaStatus(code);
                return new CallToolResult(List.of(new TextContent(mapper.writeValueAsString(result))), false);
            } catch (Exception e) {
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }

    private SyncToolRegistration buildBusinessSearchTool() {
        var schema = new McpSchema.JsonSchema("object",
            Map.of(
                "query",      Map.of("type", "string", "description", "Search query"),
                "entityType", Map.of("type", "string", "description", "Type filter (optional)")
            ),
            List.of("query"));
        var toolDef = new Tool("business_search",
            "Search across business entities with domain rules applied", schema);
        return new SyncToolRegistration(toolDef, args -> {
            try {
                var req = new com.company.bizapi.model.dto.SearchRequestDto();
                req.setQuery((String) args.get("query"));
                req.setEntityType((String) args.getOrDefault("entityType", null));
                SearchResultDto result = service.businessSearch(req);
                return new CallToolResult(List.of(new TextContent(mapper.writeValueAsString(result))), false);
            } catch (Exception e) {
                return new CallToolResult(List.of(new TextContent("Error: " + e.getMessage())), true);
            }
        });
    }
}
