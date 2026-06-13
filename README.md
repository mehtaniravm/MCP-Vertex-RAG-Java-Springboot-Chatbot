<div align="center">

# 🤖 MCP-Vertex-RAG-Java-Springboot-Chatbot

### AI-Powered Internal Chatbot — Google Cloud Platform

**MCP Server · Vertex AI (Gemini) · RAG · PostgreSQL · Spring Boot · Embeddable Web Widget**

---

*Designed by Curiosity. Built by Discipline. Delivered by Nirav Mehta.*

[![GCP](https://img.shields.io/badge/Google_Cloud-4285F4?style=flat&logo=google-cloud&logoColor=white)](https://cloud.google.com)
[![Java](https://img.shields.io/badge/Java_17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io)
[![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?style=flat&logo=typescript&logoColor=white)](https://typescriptlang.org)
[![Python](https://img.shields.io/badge/Python_3.11-3776AB?style=flat&logo=python&logoColor=white)](https://python.org)

</div>

---

## 📋 Table of Contents

1. [What This Project Does](#-what-this-project-does)
2. [Business Value](#-business-value)
3. [Architecture Overview](#-architecture-overview)
4. [Technology Concepts Explained](#-technology-concepts-explained)
5. [Repository Structure](#-repository-structure)
6. [Component Summary](#-component-summary)
7. [Data Flow](#-data-flow)
8. [Security Design](#-security-design)
9. [MCP Integration Options](#-mcp-integration-options)
10. [Key Design Decisions](#-key-design-decisions)
11. [Roadmap](#-roadmap)

---

## 🎯 What This Project Does

This system is an **AI-powered internal chatbot** that answers questions about your organization's data — grounded in your actual PostgreSQL database, not generic internet knowledge.

### Core Capabilities

| Capability | Description |
|-----------|-------------|
| **Jargon Translation** | Automatically expands acronyms (SLA → Service Level Agreement) using a live dictionary maintained in your database |
| **Data-Grounded Answers** | Queries your PostgreSQL database in real time — no hallucination |
| **Business Logic Layer** | Surfaces computed insights (health scores, SLA breach risk, KPI aggregations) not possible with raw SQL |
| **Embeddable Widget** | Single `<script>` tag drops a floating chat button into any existing web application |
| **Zero Lock-in** | Framework-agnostic widget works in React, Angular, Vue, or plain HTML apps |

### Example Questions the Chatbot Answers

```
"What does SLA mean in our context?"
"Show me all critical events from the last 24 hours"
"What is the KPI report for Q1 2026?"
"Which entities are at risk of SLA breach?"
"Explain RBAC as we use it in our systems"
"What is the current health of the Alpha Service?"
```

---

## 💼 Business Value

### Problems Solved

| Before | After |
|--------|-------|
| New employees spend weeks learning internal acronyms | Instant jargon lookup with domain context |
| Operational data siloed across multiple dashboards | Single conversational interface to all data |
| Repeated questions to senior team members | Self-service AI answers with audit trail |
| Copy-pasting queries from wiki to SQL client | Natural language queries, no SQL knowledge needed |

### ROI Indicators

- **Onboarding time reduction** — new team members become self-sufficient faster
- **Incident response acceleration** — operational data accessible conversationally during outages
- **Knowledge democratization** — business stakeholders query data without engineering dependency
- **Reusable infrastructure** — widget embeds into any existing application with one line of code

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         WEB WIDGET (Vanilla JS)                         │
│              Embeds via single <script> tag in any web app              │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ HTTPS POST /api/chat
┌────────────────────────────▼────────────────────────────────────────────┐
│                    CHATBOT API (Spring Boot · Cloud Run)                 │
│           Orchestrates: MCP context fetch → Vertex AI generation        │
└──────────┬──────────────────────────────────────┬───────────────────────┘
           │ HTTP (internal)                       │ gRPC (Vertex AI SDK)
┌──────────▼───────────────┐          ┌────────────▼────────────────────────┐
│  MCP SERVER (Node.js     │          │   VERTEX AI · GEMINI (Managed GCP)  │
│   · Cloud Run)           │          │   + RAG Corpus (text-embedding-004)  │
│                          │          └─────────────────────────────────────┘
│  Tools:                  │
│  ├─ query_postgres       │──┐  Direct SQL (read-only)
│  ├─ lookup_jargon        │  └─► CLOUD SQL (PostgreSQL)
│  ├─ list_tables          │       ├── jargon_dictionary
│  ├─ search_records       │       ├── entities
│  │                       │       ├── entity_metrics
│  └─ [biz data tools] ───┤       └── events
│    get_entity_summary     │
│    get_kpi_report         │  HTTP (internal, optional API key)
│    get_active_events      ├─►┌──────────────────────────────────────────┐
│    get_sla_status         │  │  BUSINESS DATA API (Spring Boot · Cloud  │
│    business_search        │  │  Run) — Business logic layer             │
└──────────────────────────┘  │  Computes: health scores, SLA risk,      │
                               │  KPI aggregations, breach detection      │
                               └──────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                      NIGHTLY RAG SYNC (Cloud Scheduler)                  │
│              PostgreSQL jargon_dictionary → GCS → Vertex AI RAG Corpus   │
└─────────────────────────────────────────────────────────────────────────┘
```

### GCP Services Used

| Service | Role |
|---------|------|
| **Cloud Run** | Serverless containers for all 3 APIs (auto-scales to zero) |
| **Cloud SQL** | Managed PostgreSQL — your existing database |
| **Vertex AI (Gemini)** | LLM for natural language generation |
| **Vertex AI RAG** | Vector search + embedding index for grounded retrieval |
| **Secret Manager** | Stores DB passwords and API keys — never in env vars |
| **Cloud Scheduler** | Triggers nightly RAG corpus sync |
| **Cloud Storage** | Hosts web widget JS + RAG document staging |
| **Artifact Registry** | Docker image storage for all services |
| **Cloud Build** | CI/CD — builds and deploys on push |
| **Cloud IAM** | Least-privilege service accounts per component |

---

## 🧠 Technology Concepts Explained

### What is Vertex AI?
Google Cloud's managed AI platform. Contains Gemini — a large language model that reads and generates human text. You call it via API; it answers questions. You pay per request, not for infrastructure.

### What is RAG (Retrieval Augmented Generation)?
A pattern where you first **retrieve** relevant information from YOUR data (jargon dictionary, database records), then feed that retrieved context to the AI so it answers based on YOUR knowledge — not generic internet training data. Prevents hallucination.

### What is MCP Server (Model Context Protocol)?
An open protocol (created by Anthropic, adopted industry-wide) that lets AI models call external tools in a standardized way. Your MCP Server exposes capabilities like `query_postgres`, `lookup_jargon`, `get_entity_summary` — and any MCP-compatible AI can call those tools. It's the glue between AI reasoning and your actual systems.

---

## 📁 Repository Structure

```
MCP-Vertex-RAG-Java-Springboot-Chatbot/
│
├── chatbot-api/              Spring Boot — Chat orchestration API
│   ├── src/main/java/com/company/chatbot/
│   │   ├── controller/       ChatController.java         REST endpoint
│   │   ├── service/          VertexAiService.java        Gemini integration
│   │   │                     McpClientService.java       MCP tool caller
│   │   ├── config/           VertexAiConfig.java         GCP auth (ADC)
│   │   └── model/            ChatRequest/Response DTOs
│   ├── Dockerfile
│   └── pom.xml
│
├── business-data-api/        Spring Boot — Business logic data layer
│   ├── src/main/java/com/company/bizapi/
│   │   ├── controller/       BusinessDataController.java REST endpoints
│   │   ├── service/          BusinessDataService.java    Business logic
│   │   ├── repository/       JPA repositories
│   │   ├── model/entity/     JPA entities (EntityRecord, EntityMetric, EventRecord)
│   │   ├── model/dto/        Response DTOs (EntitySummaryDto, KpiReportDto, etc.)
│   │   ├── config/           ApiKeyFilter.java           Auth filter (toggleable)
│   │   └── mcp/              McpServerConfig.java        Option B MCP endpoint
│   ├── Dockerfile
│   └── pom.xml
│
├── mcp-server/               Node.js/TypeScript MCP Server
│   ├── src/
│   │   ├── index.ts          Entry point — creates MCP server
│   │   ├── tools/
│   │   │   ├── postgresTools.ts  query_postgres, list_tables, search_records
│   │   │   ├── jargonTools.ts    lookup_jargon, get_jargon_dictionary
│   │   │   └── bizDataTools.ts   HTTP tools → Business Data API (Option A)
│   │   ├── db/client.ts      PostgreSQL pool (Secret Manager password fetch)
│   │   └── clients/bizApiClient.ts  Dual-auth HTTP client
│   ├── Dockerfile
│   ├── package.json
│   └── tsconfig.json
│
├── web-widget/               Embeddable chatbot UI
│   ├── chatbot-widget.js     Self-contained, zero-dependency plugin
│   └── index.html            Demo page
│
├── rag-sync/                 Nightly RAG corpus sync
│   ├── sync_to_rag.py        PostgreSQL → GCS → Vertex AI RAG
│   ├── requirements.txt
│   └── Dockerfile
│
├── db/                       Database
│   ├── 01-schema.sql         Tables + read-only user
│   └── 02-seed-data.sql      Jargon dictionary (20 terms) + sample data
│
├── infra/                    GCP setup automation
│   ├── 01-enable-apis.sh
│   ├── 02-service-accounts.sh
│   ├── 03-secrets.sh
│   └── 04-deploy-all.sh
│
└── docs/
    ├── LOCAL-DEV-GUIDE.md
    └── OPTION-B-MCP-SETUP.md
```

---

## 🔧 Component Summary

### chatbot-api (Spring Boot)
The entry point for all user messages. Receives chat requests, fetches context from the MCP Server, builds a RAG-enhanced prompt, and calls Vertex AI (Gemini) for the final answer.

**Key classes:**
- `ChatController` — `POST /api/chat` with `@CrossOrigin` for widget embedding
- `VertexAiService` — Gemini API call with temperature 0.2 (factual, not creative)
- `McpClientService` — Calls MCP tools to retrieve context

### business-data-api (Spring Boot)
Exposes business-logic-enriched data. Computes health scores, SLA breach risk, and KPI aggregations that would be complex or brittle as raw SQL.

**Key endpoints:**
```
GET  /api/business/entities/{code}/summary  → Health score + alerts
GET  /api/business/kpi/report               → Aggregated metrics
GET  /api/business/events/active            → Open events with urgency
GET  /api/business/sla/status               → SLA compliance + risk
POST /api/business/search                   → Business search
```

**Auth:** `ApiKeyFilter` — `API_KEY_ENABLED=true` enforces `X-API-Key` header. Set `false` for no-auth internal-only mode.

### mcp-server (Node.js/TypeScript)
The MCP Server exposing 8 tools to the AI:

| Tool | Source | Description |
|------|--------|-------------|
| `query_postgres` | Direct SQL | Read-only SELECT on any table |
| `list_tables` | Direct SQL | Schema introspection |
| `search_records` | Direct SQL | Parameterized table search |
| `lookup_jargon` | Direct SQL | Single term lookup |
| `get_jargon_dictionary` | Direct SQL | Full dictionary (RAG context) |
| `get_entity_summary` | Biz API | Health score + alerts |
| `get_kpi_report` | Biz API | Date-range KPI aggregation |
| `get_active_events` | Biz API | Open events with urgency |
| `get_sla_status` | Biz API | Compliance + breach risk |
| `business_search` | Biz API | Cross-entity search |

### web-widget (Vanilla JS)
Zero-dependency embeddable plugin. Single `<script>` tag. No npm install, no framework required.

```html
<script
  src="https://storage.googleapis.com/YOUR-BUCKET/chatbot-widget.js"
  data-api-url="https://chatbot-api-xxx.run.app/api/chat"
  data-title="AI Assistant"
  data-theme="#1A73E8"
  defer
></script>
```

### rag-sync (Python)
Nightly Cloud Scheduler job that exports the jargon dictionary from PostgreSQL, uploads to GCS, and imports into the Vertex AI RAG corpus. Keeps AI context current without real-time embedding cost.

---

## 🔄 Data Flow

### Request Flow (per user message)

```
1. User types: "What entities have SLA breaches?"
   └── Widget sends POST /api/chat {"message": "..."}

2. Chatbot API receives request
   └── Calls MCP tool: get_jargon_dictionary()
   └── Calls MCP tool: get_sla_status()

3. MCP Server handles get_sla_status()
   └── HTTP GET → Business Data API /api/business/sla/status
   └── Business logic: computes breach risk score per entity
   └── Returns JSON with complianceStatus, breachRiskScore, recommendations

4. Chatbot API builds augmented prompt:
   [JARGON DICTIONARY] + [SLA STATUS DATA] + [USER QUESTION]

5. Vertex AI (Gemini) generates grounded answer
   └── Temperature 0.2 → factual, not creative
   └── System prompt: "Only answer from provided context"

6. Response streams back to widget
   └── "Based on current data, ENT-002 (Beta Platform) is BREACHED
       with 98.5% uptime vs 99.9% SLA threshold. Recommended action:
       Immediate escalation. ENT-005 is AT_RISK."
```

### RAG Sync Flow (nightly)

```
Cloud Scheduler (11 PM UTC)
  └── Triggers rag-sync container
      └── Queries PostgreSQL: SELECT * FROM jargon_dictionary
      └── Formats as text documents
      └── Uploads to gs://PROJECT-rag-corpus/jargon/
      └── Calls Vertex AI RAG: import_files() with chunk_size=512
      └── RAG corpus updated — live for next morning's queries
```

---

## 🔐 Security Design

### Principle of Least Privilege

| Service | Service Account | Permissions |
|---------|----------------|-------------|
| MCP Server | `mcp-server-sa` | `cloudsql.client` (read), `aiplatform.user`, `secretmanager.secretAccessor` |
| Business Data API | `biz-api-sa` | `cloudsql.client` (read), `secretmanager.secretAccessor` |
| Chatbot API | `chatbot-api-sa` | `aiplatform.user`, `secretmanager.secretAccessor` |
| RAG Sync | `rag-sync-sa` | `storage.objectAdmin`, `aiplatform.user`, `cloudsql.client` |

### Auth Layers

1. **Database** — Read-only PostgreSQL user (`app_readonly`) — cannot mutate data even if MCP Server is compromised
2. **MCP → Business API** — API key in `X-API-Key` header OR no-auth with `--ingress internal` Cloud Run
3. **Cloud Run** — Internal services not publicly accessible (`--no-allow-unauthenticated --ingress internal`)
4. **Secrets** — All passwords and keys in GCP Secret Manager — never in env vars or code
5. **SQL Injection** — MCP Server validates: only `SELECT` allowed, parameterized queries for all filters

---

## 🔌 MCP Integration Options

### Option A (Default — `main` branch)
MCP Server (Node.js) calls Business Data API via HTTP REST.

```
AI → MCP Server → HTTP GET/POST → Business Data API → PostgreSQL
```

**Pros:** Lower complexity, Business Data API reusable by anything

### Option B (Advanced — `feature/option-b-mcp` branch)
Spring Boot exposes MCP protocol endpoint directly.

```
AI → Spring Boot /mcp (SSE, JSON-RPC 2.0) → Business logic → PostgreSQL
```

**Pros:** Eliminates Node.js layer for business data path  
**Setup:** See `docs/OPTION-B-MCP-SETUP.md`

---

## 🎯 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| LLM temperature | 0.2 | Low = factual, deterministic answers. Not 0.7 (creative/hallucination-prone) |
| RAG sync frequency | Nightly | Real-time embedding per query costs 10x more. Jargon changes daily at most |
| MCP Server language | Node.js/TypeScript | MCP SDK is most mature on Node.js. Java SDK is newer |
| DB user | Read-only | MCP Server can never mutate data. Defense in depth |
| Widget | Vanilla JS | Zero-dependency = plugs into any existing app without npm install |
| Auth toggle | Env var flag | `API_KEY_ENABLED=false` for internal-only, no code change needed |
| Secrets | Secret Manager | Never in env vars, never in code, automatic rotation support |

---

## 🗺️ Roadmap

### Phase 1 — Foundation ✅
- [x] MCP Server with PostgreSQL + Jargon tools
- [x] Business Data API with business logic layer
- [x] Spring Boot Chat API with Vertex AI integration
- [x] Embeddable web widget
- [x] RAG corpus nightly sync
- [x] GCP deployment automation

### Phase 2 — Enhancement
- [ ] Streaming responses (Server-Sent Events for word-by-word display)
- [ ] Conversation memory (Firestore for cross-session history)
- [ ] Feedback loop (thumbs up/down → BigQuery for quality tracking)
- [ ] Multi-corpus RAG (Confluence, Jira, runbooks as additional sources)

### Phase 3 — Enterprise
- [ ] OAuth2/OIDC auth on widget (GCP Identity-Aware Proxy integration)
- [ ] Analytics dashboard (Cloud Logging + Looker Studio)
- [ ] Write tools with approval gates (create Jira tickets from chat)
- [ ] Multi-language support

---

## 📄 License

MIT — see [LICENSE](./LICENSE)

---

<div align="center">

**Designed by Curiosity. Built by Discipline. Delivered by Nirav Mehta.**

</div>
