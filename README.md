# MCP-Vertex-RAG-Java-Springboot-Chatbot

> **Designed by Curiosity. Built by Discipline. Delivered by Nirav Mehta.**

An AI-powered internal chatbot that combines **MCP Server**, **Vertex AI (Gemini)**, and **RAG** to answer questions grounded in your PostgreSQL data — with an embeddable web widget that plugs into any existing application.

---

## Architecture

```
Web Widget (vanilla JS)
    └── Spring Boot Chat API          [chatbot-api/]
            ├── MCP Server (Node.js)  [mcp-server/]
            │       ├── Tool: query_postgres      → Cloud SQL (direct)
            │       ├── Tool: lookup_jargon       → Cloud SQL (direct)
            │       └── Tool: get_business_data   → Business Data API (HTTP)
            ├── Business Data API     [business-data-api/]
            │       └── Spring Boot REST + business logic → Cloud SQL
            └── Vertex AI (Gemini)    [GCP managed]
                    └── RAG Corpus    [rag-sync/]
```

---

## Repository Structure

| Module | Description | Language |
|--------|-------------|----------|
| [`chatbot-api/`](./chatbot-api) | Spring Boot orchestration API — receives chat, calls MCP, calls Vertex AI | Java 17 |
| [`mcp-server/`](./mcp-server) | MCP Server — exposes DB + business data tools to AI | TypeScript/Node.js |
| [`business-data-api/`](./business-data-api) | Spring Boot Business Data API — business logic layer over PostgreSQL | Java 17 |
| [`web-widget/`](./web-widget) | Embeddable chatbot widget — single `<script>` tag embed | Vanilla JS |
| [`rag-sync/`](./rag-sync) | Nightly sync script — PostgreSQL jargon → Vertex AI RAG corpus | Python |
| [`infra/`](./infra) | GCP setup scripts — service accounts, IAM, Cloud Run deploy | bash |
| [`db/`](./db) | PostgreSQL schema migrations — jargon dictionary + sample data | SQL |

---

## Quick Start

### Prerequisites
- GCP project with billing enabled
- Java 17+, Node.js 20+, Python 3.11+, Maven 3.9+
- `gcloud` CLI authenticated

### 1. GCP Bootstrap
```bash
cd infra
export PROJECT_ID=your-gcp-project-id
export REGION=us-central1
chmod +x *.sh
./01-enable-apis.sh
./02-service-accounts.sh
./03-secrets.sh
```

### 2. Database Setup
```bash
cd db
psql -h localhost -U postgres -d your_db -f 01-schema.sql
psql -h localhost -U postgres -d your_db -f 02-seed-data.sql
```

### 3. Build & Run Locally
```bash
# Terminal 1 — Business Data API
cd business-data-api
export API_KEY_ENABLED=false
mvn spring-boot:run

# Terminal 2 — MCP Server
cd mcp-server
npm install && npm run dev

# Terminal 3 — Chat API
cd chatbot-api
mvn spring-boot:run

# Terminal 4 — Open web-widget/index.html in browser
```

### 4. Deploy to GCP
```bash
cd infra
./04-deploy-all.sh
```

---

## MCP Integration Options

This repo implements **both** options — use whichever fits your team:

| | Option A (default branch) | Option B (`feature/option-b-mcp` branch) |
|-|--------------------------|------------------------------------------|
| **Pattern** | MCP Node.js calls REST API | Spring Boot exposes MCP protocol endpoint |
| **Complexity** | Lower | Higher |
| **Best for** | Most teams | Teams eliminating Node.js layer |

Switch: `git checkout feature/option-b-mcp`

---

## Auth Modes

Both the MCP Server → Business Data API call support two auth modes:

| Mode | How | When |
|------|-----|------|
| **API Key** | `X-API-Key` header, key in Secret Manager | Default |
| **No Auth** | `API_KEY_ENABLED=false` env var | Internal-only Cloud Run |

---

## Environment Variables

See [`.env.example`](./.env.example) for all required variables per service.

---

## License

MIT — see [LICENSE](./LICENSE)
