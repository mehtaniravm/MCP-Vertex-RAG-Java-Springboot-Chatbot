/**
 * HTTP client for the Spring Boot Business Data API.
 * Auth mode controlled by BIZ_API_KEY env var:
 *   - Set     => sends X-API-Key header (API key auth mode)
 *   - Not set => no auth header (no-auth / internal-only mode)
 */

const BIZ_API_URL    = process.env.BIZ_API_URL;
const BIZ_API_KEY    = process.env.BIZ_API_KEY;
const API_KEY_HEADER = 'X-API-Key';

function buildHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    'Accept':       'application/json',
  };
  if (BIZ_API_KEY) {
    headers[API_KEY_HEADER] = BIZ_API_KEY;
  }
  return headers;
}

function assertBaseUrl(): string {
  if (!BIZ_API_URL) {
    throw new Error('BIZ_API_URL env var is required for Business Data API tools');
  }
  return BIZ_API_URL;
}

export async function bizGet<T>(path: string): Promise<T> {
  const url = `${assertBaseUrl()}${path}`;
  const res  = await fetch(url, { method: 'GET', headers: buildHeaders() });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(`Business API ${res.status} on GET ${path}: ${body}`);
  }
  return res.json() as Promise<T>;
}

export async function bizPost<T>(path: string, body: unknown): Promise<T> {
  const url = `${assertBaseUrl()}${path}`;
  const res  = await fetch(url, {
    method:  'POST',
    headers: buildHeaders(),
    body:    JSON.stringify(body),
  });
  if (!res.ok) {
    const errBody = await res.text();
    throw new Error(`Business API ${res.status} on POST ${path}: ${errBody}`);
  }
  return res.json() as Promise<T>;
}
