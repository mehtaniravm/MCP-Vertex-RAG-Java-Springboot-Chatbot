import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { z } from 'zod';
import { getPool } from '../db/client.js';

export function registerPostgresTools(server: McpServer) {

  // Tool 1: Execute read-only SQL
  server.tool(
    'query_postgres',
    'Execute a read-only SELECT query against the PostgreSQL database. Only SELECT statements allowed.',
    { sql: z.string().describe('A SELECT SQL statement. No INSERT/UPDATE/DELETE/DROP allowed.') },
    async ({ sql }) => {
      const normalized = sql.trim().toLowerCase();
      if (!normalized.startsWith('select')) {
        return { content: [{ type: 'text', text: 'Error: Only SELECT queries are permitted.' }] };
      }
      try {
        const pool   = await getPool();
        const result = await pool.query(sql);
        return { content: [{ type: 'text', text: JSON.stringify(result.rows, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Query error: ${err.message}` }] };
      }
    }
  );

  // Tool 2: List all tables + columns
  server.tool(
    'list_tables',
    'List all tables in the database with their column names and data types.',
    {},
    async () => {
      try {
        const pool   = await getPool();
        const result = await pool.query(`
          SELECT
            t.table_name,
            c.column_name,
            c.data_type,
            c.is_nullable,
            c.column_default
          FROM information_schema.tables t
          JOIN information_schema.columns c
            ON t.table_name = c.table_name AND t.table_schema = c.table_schema
          WHERE t.table_schema = 'public'
            AND t.table_type  = 'BASE TABLE'
          ORDER BY t.table_name, c.ordinal_position
        `);
        // Group by table
        const tables: Record<string, any[]> = {};
        for (const row of result.rows) {
          if (!tables[row.table_name]) tables[row.table_name] = [];
          tables[row.table_name].push({
            column:   row.column_name,
            type:     row.data_type,
            nullable: row.is_nullable === 'YES',
            default:  row.column_default,
          });
        }
        return { content: [{ type: 'text', text: JSON.stringify(tables, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Error listing tables: ${err.message}` }] };
      }
    }
  );

  // Tool 3: Search records (safe parameterized query)
  server.tool(
    'search_records',
    'Search records in a specific table by column value. Safe parameterized query — no raw SQL needed.',
    {
      table:  z.string().describe('Table name to search'),
      column: z.string().describe('Column name to filter on'),
      value:  z.string().describe('Value to search for (case-insensitive partial match)'),
      limit:  z.number().default(20).describe('Max rows to return (default 20, max 100)'),
    },
    async ({ table, column, value, limit }) => {
      // Whitelist table/column names to prevent injection
      const allowed = /^[a-zA-Z_][a-zA-Z0-9_]*$/;
      if (!allowed.test(table) || !allowed.test(column)) {
        return { content: [{ type: 'text', text: 'Error: Invalid table or column name.' }] };
      }
      const safeLimit = Math.min(limit, 100);
      try {
        const pool   = await getPool();
        const result = await pool.query(
          `SELECT * FROM ${table} WHERE LOWER(${column}::text) LIKE LOWER($1) LIMIT $2`,
          [`%${value}%`, safeLimit]
        );
        return { content: [{ type: 'text', text: JSON.stringify(result.rows, null, 2) }] };
      } catch (err: any) {
        return { content: [{ type: 'text', text: `Search error: ${err.message}` }] };
      }
    }
  );
}
