import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { getPool } from '../db/client.js';

export function registerJargonTools(server: McpServer) {

  // Tool: Look up a single jargon term
  server.tool(
    'lookup_jargon',
    'Expand an acronym or short form to its full human-readable meaning from the jargon dictionary.',
    { term: z.string().describe('The acronym or short form to look up, e.g. SLA, KPI, RBAC') },
    async ({ term }) => {
      try {
        const pool   = await getPool();
        const result = await pool.query(
          `SELECT short_form, full_form, description, domain, example_usage
           FROM jargon_dictionary
           WHERE LOWER(short_form) = LOWER($1)`,
          [term]
        );
        if (result.rows.length === 0) {
          return { content: [{ type: 'text', text: `Term "${term}" not found in jargon dictionary.` }] };
        }
        return { content: [{ type: 'text', text: JSON.stringify(result.rows[0], null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Jargon lookup error: ${err.message}` }] };
      }
    }
  );

  // Tool: Get full jargon dictionary (for RAG context)
  server.tool(
    'get_jargon_dictionary',
    'Return the full jargon dictionary. Use to provide comprehensive acronym context to the AI.',
    {
      domain: z.string().optional().describe('Optional: filter by domain, e.g. Operations, Security, AI'),
    },
    async ({ domain }) => {
      try {
        const pool = await getPool();
        const query = domain
          ? `SELECT short_form, full_form, description, domain FROM jargon_dictionary WHERE LOWER(domain) = LOWER($1) ORDER BY short_form`
          : `SELECT short_form, full_form, description, domain FROM jargon_dictionary ORDER BY short_form`;
        const result = await pool.query(query, domain ? [domain] : []);
        return { content: [{ type: 'text', text: JSON.stringify(result.rows, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Dictionary fetch error: ${err.message}` }] };
      }
    }
  );
}
