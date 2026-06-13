/**
 * MCP tools that call the Spring Boot Business Data API (Option A).
 * The AI calls these tools exactly like postgres tools — 
 * the HTTP call to the API is transparent.
 */
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { bizGet, bizPost } from '../clients/bizApiClient.js';

export function registerBizDataTools(server: McpServer) {

  server.tool(
    'get_entity_summary',
    'Get enriched entity summary including computed health score, SLA compliance, and recent metrics. Uses business logic from the Java API.',
    { entityCode: z.string().describe('The entity code, e.g. ENT-001') },
    async ({ entityCode }) => {
      try {
        const data = await bizGet(`/api/business/entities/${encodeURIComponent(entityCode)}/summary`);
        return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
      }
    }
  );

  server.tool(
    'get_kpi_report',
    'Get aggregated KPI metrics for a date range. Optionally filter by category. Business logic computed server-side.',
    {
      from:     z.string().describe('Start date in YYYY-MM-DD format'),
      to:       z.string().describe('End date in YYYY-MM-DD format'),
      category: z.string().optional().describe('Optional entity category filter, e.g. SERVICE, PLATFORM'),
    },
    async ({ from, to, category }) => {
      try {
        const params = new URLSearchParams({ from, to });
        if (category) params.append('category', category);
        const data = await bizGet(`/api/business/kpi/report?${params.toString()}`);
        return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
      }
    }
  );

  server.tool(
    'get_active_events',
    'Get open events and alerts with enriched context and severity classification.',
    {
      severity:  z.enum(['INFO', 'WARNING', 'CRITICAL']).optional().describe('Filter by severity level'),
      lastHours: z.number().default(24).describe('Look back window in hours (default 24)'),
    },
    async ({ severity, lastHours }) => {
      try {
        const params = new URLSearchParams({ lastHours: lastHours.toString() });
        if (severity) params.append('severity', severity);
        const data = await bizGet(`/api/business/events/active?${params.toString()}`);
        return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
      }
    }
  );

  server.tool(
    'get_sla_status',
    'Get SLA compliance status for all entities or a specific entity. Includes breach risk calculation.',
    {
      entityCode: z.string().optional().describe('Optional: specific entity code. Omit for all entities.'),
    },
    async ({ entityCode }) => {
      try {
        const path = entityCode
          ? `/api/business/sla/status?entityCode=${encodeURIComponent(entityCode)}`
          : `/api/business/sla/status`;
        const data = await bizGet(path);
        return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
      }
    }
  );

  server.tool(
    'business_search',
    'Search across business entities with domain-specific filtering rules and enriched results.',
    {
      query:      z.string().describe('Natural language or keyword search'),
      entityType: z.string().optional().describe('Entity type filter: entity, event, metric'),
      filters:    z.record(z.string()).optional().describe('Additional key-value filters'),
    },
    async ({ query, entityType, filters }) => {
      try {
        const data = await bizPost('/api/business/search', { query, entityType, filters });
        return { content: [{ type: 'text', text: JSON.stringify(data, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error: ${err.message}` }] };
      }
    }
  );
}
