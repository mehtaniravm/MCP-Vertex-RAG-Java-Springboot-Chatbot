# Local Development Guide

## Prerequisites

- Java 17+, Maven 3.9+
- Node.js 20+, npm
- Python 3.11+
- Docker (optional)
- `gcloud` CLI: `gcloud auth application-default login`
- Cloud SQL Auth Proxy (for local DB connection)

## Start Order

Always start in this order — each service depends on the previous.

```
1. PostgreSQL (local or via Cloud SQL Auth Proxy)
2. Business Data API  (port 8081)
3. MCP Server         (port 3000)
4. Chatbot API        (port 8080)
5. Open web-widget/index.html in browser
```

## Step-by-Step

### 1. Database

Local PostgreSQL:
```bash
psql -U postgres -c "CREATE DATABASE app_db;"
psql -U postgres -d app_db -f db/01-schema.sql
psql -U postgres -d app_db -f db/02-seed-data.sql
```

Or Cloud SQL Auth Proxy:
```bash
cloud-sql-proxy $CLOUD_SQL_CONNECTION_NAME --port=5432 &
```

### 2. Business Data API

```bash
cd business-data-api
export SPRING_PROFILES_ACTIVE=local
export DB_NAME=app_db
export DB_USER=postgres
export DB_PASS=your_local_password
export API_KEY_ENABLED=false    # no auth locally
mvn spring-boot:run -Dserver.port=8081
# Test: curl http://localhost:8081/api/business/health
```

### 3. MCP Server

```bash
cd mcp-server
cp .env.example .env
# Edit .env: set DB_HOST=localhost, DB_PASS=your_local_password, BIZ_API_URL=http://localhost:8081
npm install
npm run dev
```

### 4. Chatbot API

```bash
cd chatbot-api
export GCP_PROJECT_ID=your-project
export MCP_SERVER_URL=http://localhost:3000
# Local Vertex AI: requires gcloud auth application-default login
mvn spring-boot:run
# Test: curl -X POST http://localhost:8080/api/chat -H 'Content-Type: application/json' -d '{"message":"What does SLA mean?"}'
```

### 5. Web Widget

Open `web-widget/index.html` directly in your browser.  
The demo page points to `http://localhost:8080/api/chat`.

## Common Issues

| Issue | Fix |
|-------|-----|
| `GCP_PROJECT_ID not set` | Export the env var or set `SPRING_PROFILES_ACTIVE=local` |
| DB connection refused | Start Cloud SQL Auth Proxy or local PostgreSQL |
| Vertex AI 401 | Run `gcloud auth application-default login` |
| MCP tool not found | Check MCP Server is running on port 3000 |
| CORS error in widget | Ensure `@CrossOrigin` is on ChatController |
