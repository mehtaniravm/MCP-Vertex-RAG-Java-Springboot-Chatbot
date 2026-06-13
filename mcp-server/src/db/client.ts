import { Pool } from 'pg';
import { SecretManagerServiceClient } from '@google-cloud/secret-manager';

async function getSecret(secretName: string): Promise<string> {
  const projectId = process.env.GCP_PROJECT_ID;
  if (!projectId) throw new Error('GCP_PROJECT_ID env var required');
  const client = new SecretManagerServiceClient();
  const [version] = await client.accessSecretVersion({
    name: `projects/${projectId}/secrets/${secretName}/versions/latest`,
  });
  return version.payload!.data!.toString().trim();
}

async function createPool(): Promise<Pool> {
  let password: string;
  if (process.env.NODE_ENV === 'development' && process.env.DB_PASS) {
    password = process.env.DB_PASS;
  } else {
    password = await getSecret(process.env.DB_PASSWORD_SECRET || 'postgres-password');
  }
  const isCloudRun = !!process.env.CLOUD_SQL_CONNECTION_NAME;
  return new Pool({
    host: isCloudRun
      ? `/cloudsql/${process.env.CLOUD_SQL_CONNECTION_NAME}`
      : (process.env.DB_HOST || 'localhost'),
    port: isCloudRun ? undefined : parseInt(process.env.DB_PORT || '5432'),
    user:     process.env.DB_USER || 'app_readonly',
    password,
    database: process.env.DB_NAME || 'app_db',
    max: 5,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000,
  });
}

let _pool: Pool | null = null;

export async function getPool(): Promise<Pool> {
  if (!_pool) _pool = await createPool();
  return _pool;
}
