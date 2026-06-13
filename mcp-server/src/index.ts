import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { registerPostgresTools } from './tools/postgresTools.js';
import { registerJargonTools }   from './tools/jargonTools.js';
import { registerBizDataTools }  from './tools/bizDataTools.js';

async function main() {
  const server = new McpServer({
    name:    'mcp-chatbot-server',
    version: '1.0.0',
  });

  // Register all tool groups
  registerPostgresTools(server);   // Direct DB: query_postgres, list_tables, search_records
  registerJargonTools(server);     // Jargon: lookup_jargon, get_jargon_dictionary
  registerBizDataTools(server);    // Business API (Option A): entity_summary, kpi_report, events, sla, search

  const transport = new StdioServerTransport();
  await server.connect(transport);

  console.error('MCP Server started — tools registered: postgres, jargon, business-data');
}

main().catch((err) => {
  console.error('MCP Server fatal error:', err);
  process.exit(1);
});
