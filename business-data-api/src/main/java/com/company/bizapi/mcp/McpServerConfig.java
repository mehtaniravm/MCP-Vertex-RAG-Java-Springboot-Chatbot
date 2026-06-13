package com.company.bizapi.mcp;

/**
 * OPTION B: Spring Boot as MCP Server
 * ======================================
 * This class wires Spring Boot to expose the MCP protocol endpoint directly.
 * 
 * To activate Option B:
 *   1. Add io.modelcontextprotocol.sdk:mcp-spring-webmvc to pom.xml
 *   2. Set ENABLE_MCP_ENDPOINT=true env var
 *   3. The /mcp SSE endpoint becomes available
 *
 * Option A (default) is recommended for most teams.
 * Option B removes the Node.js MCP layer for the business data path.
 *
 * See: docs/OPTION-B-MCP-SETUP.md for full setup guide.
 */

/*
 * Uncomment and add dependency to pom.xml to activate:
 *
 * <dependency>
 *   <groupId>io.modelcontextprotocol.sdk</groupId>
 *   <artifactId>mcp-spring-webmvc</artifactId>
 *   <version>0.9.0</version>
 * </dependency>
 *
 * @Configuration
 * @ConditionalOnProperty(name = "app.mcp.enabled", havingValue = "true")
 * public class McpServerConfig {
 *
 *     @Autowired private BusinessDataService service;
 *
 *     @Bean
 *     public McpSyncServer mcpSyncServer() {
 *         return McpSyncServer.builder()
 *             .serverInfo(new Implementation("biz-data-mcp", "1.0.0"))
 *             .capabilities(ServerCapabilities.builder().tools(true).build())
 *             .tools(buildTools())
 *             .build();
 *     }
 *
 *     @Bean
 *     public SseServerTransportProvider sseTransportProvider(ObjectMapper mapper, McpSyncServer server) {
 *         return new SseServerTransportProvider(mapper, "/mcp");
 *     }
 *
 *     @Bean
 *     public RouterFunction<ServerResponse> mcpRoutes(SseServerTransportProvider provider) {
 *         return provider.getRouterFunction();
 *     }
 *
 *     private List<SyncToolRegistration> buildTools() {
 *         return List.of(buildEntitySummaryTool(), buildKpiReportTool(), buildActiveEventsTool());
 *     }
 *
 *     private SyncToolRegistration buildEntitySummaryTool() {
 *         var schema = new McpSchema.JsonSchema("object",
 *             Map.of("entityCode", Map.of("type", "string", "description", "Entity code e.g. ENT-001")),
 *             List.of("entityCode"));
 *         var toolDef = new Tool("get_entity_summary",
 *             "Get enriched entity summary with computed health score", schema);
 *         return new SyncToolRegistration(toolDef, args -> {
 *             String code   = (String) args.get("entityCode");
 *             var    result = service.getEntitySummary(code);
 *             String json   = new ObjectMapper().writeValueAsString(result);
 *             return new CallToolResult(List.of(new TextContent(json)), false);
 *         });
 *     }
 *     // Add buildKpiReportTool(), buildActiveEventsTool() similarly...
 * }
 */
public class McpServerConfig {
    // Placeholder — see comments above to activate Option B
}
