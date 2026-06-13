# Complete Setup Guide

> **MCP-Vertex-RAG-Java-Springboot-Chatbot**  
> Local Development · Testing · GCP Deployment

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Clone & Project Structure](#2-clone--project-structure)
3. [Database Setup](#3-database-setup)
4. [Local Development — Step by Step](#4-local-development--step-by-step)
   - 4a. Business Data API
   - 4b. MCP Server
   - 4c. Chatbot API
   - 4d. Web Widget
5. [Testing Locally](#5-testing-locally)
6. [GCP Prerequisites & Bootstrap](#6-gcp-prerequisites--bootstrap)
7. [Deploy: Business Data API](#7-deploy-business-data-api)
8. [Deploy: MCP Server](#8-deploy-mcp-server)
9. [Deploy: Chatbot API](#9-deploy-chatbot-api)
10. [Deploy: RAG Sync](#10-deploy-rag-sync)
11. [Deploy: Web Widget](#11-deploy-web-widget)
12. [Verify End-to-End](#12-verify-end-to-end)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Prerequisites

### Local Tools Required

| Tool | Version | Install |
|------|---------|---------|
| Java | 17+ | `sdk install java 17.0.9-tem` or [temurin](https://adoptium.net) |
| Maven | 3.9+ | `sdk install maven` or [maven.apache.org](https://maven.apache.org) |
| Node.js | 20+ | [nodejs.org](https://nodejs.org) or `nvm install 20` |
| npm | 10+ | Bundled with Node.js |
| Python | 3.11+ | [python.org](https://python.org) |
| PostgreSQL | 14+ | `brew install postgresql` or Docker |
| gcloud CLI | Latest | [cloud.google.com/sdk](https://cloud.google.com/sdk/docs/install) |
| Cloud SQL Auth Proxy | Latest | [cloud.google.com/sql/docs/postgres/auth-proxy](https://cloud.google.com/sql/docs/postgres/auth-proxy) |
| Docker | Latest | Optional — only needed for containerized local testing |
| Git | 2+ | Pre-installed on most systems |

### Verify Installations

```bash
java --version        # openjdk 17.x.x
mvn --version         # Apache Maven 3.9.x
node --version        # v20.x.x
npm --version         # 10.x.x
python3 --version     # Python 3.11.x
gcloud --version      # Google Cloud SDK xxx.x.x
psql --version        # psql (PostgreSQL) 14.x
```

---

## 2. Clone & Project Structure

```bash
git clone https://github.com/mehtaniravm/MCP-Vertex-RAG-Java-Springboot-Chatbot.git
cd MCP-Vertex-RAG-Java-Springboot-Chatbot

# View structure
ls -la
# chatbot-api/       business-data-api/    mcp-server/
# web-widget/        rag-sync/             db/     infra/     docs/
```

---

## 3. Database Setup

### Option A — Local PostgreSQL

```bash
# Create database and user
psql -U postgres << 'SQL'
CREATE DATABASE app_db;
CREATE USER app_user WITH PASSWORD 'localpassword';
GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
SQL

# Run schema and seed
psql -U app_user -d app_db -f db/01-schema.sql
psql -U app_user -d app_db -f db/02-seed-data.sql

# Verify
psql -U app_user -d app_db -c "SELECT COUNT(*) FROM jargon_dictionary;"
# Should return: 20
```

### Option B — Docker PostgreSQL

```bash
docker run -d \
  --name chatbot-postgres \
  -e POSTGRES_DB=app_db \
  -e POSTGRES_USER=app_user \
  -e POSTGRES_PASSWORD=localpassword \
  -p 5432:5432 \
  postgres:15-alpine

sleep 3   # wait for startup

psql postgresql://app_user:localpassword@localhost:5432/app_db -f db/01-schema.sql
psql postgresql://app_user:localpassword@localhost:5432/app_db -f db/02-seed-data.sql
```

### Option C — Cloud SQL Auth Proxy (GCP database, local dev)

```bash
# Download Cloud SQL Auth Proxy
curl -o cloud-sql-proxy https://storage.googleapis.com/cloud-sql-connectors/cloud-sql-proxy/v2.8.0/cloud-sql-proxy.linux.amd64
chmod +x cloud-sql-proxy

# Authenticate
gcloud auth application-default login

# Start proxy (background)
./cloud-sql-proxy $CLOUD_SQL_CONNECTION_NAME --port=5432 &

# Now connect as if it's localhost
psql -h localhost -U app_user -d app_db
```

---

## 4. Local Development — Step by Step

> **Start services in this order.** Each depends on the previous.

### Open 4 terminal windows before starting.

---

### 4a. Business Data API (Terminal 1)

```bash
cd business-data-api

# Create local environment file
cat > .env.local << 'EOF'
SPRING_PROFILES_ACTIVE=local
DB_NAME=app_db
DB_USER=app_user
DB_PASS=localpassword
DB_HOST=localhost
DB_PORT=5432
API_KEY_ENABLED=false
GCP_PROJECT_ID=your-gcp-project-id
EOF

# Export and run on port 8081 (avoids conflict with chatbot-api on 8080)
export $(cat .env.local | xargs)
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

# Expected output:
# Started BizApiApplication in X.XXX seconds
# Tomcat started on port(s): 8081
```

**Verify:**
```bash
curl http://localhost:8081/api/business/health
# {"status":"UP","service":"business-data-api"}

curl http://localhost:8081/api/business/sla/status
# [...SLA status JSON...]
```

---

### 4b. MCP Server (Terminal 2)

```bash
cd mcp-server

# Install dependencies
npm install

# Create .env from example
cp .env.example .env

# Edit .env with your values:
cat > .env << 'EOF'
NODE_ENV=development
GCP_PROJECT_ID=your-gcp-project-id
DB_HOST=localhost
DB_PORT=5432
DB_USER=app_user
DB_PASS=localpassword
DB_NAME=app_db
BIZ_API_URL=http://localhost:8081
BIZ_API_KEY=
EOF

# Run in dev mode (tsx for TypeScript hot-reload)
npm run dev

# Expected output:
# MCP Server started — tools registered: postgres, jargon, business-data
```

**Verify with MCP Inspector** (optional):
```bash
# In a new terminal:
npx @modelcontextprotocol/inspector node dist/index.js
# Opens http://localhost:5173 — connect and test tools interactively
```

---

### 4c. Chatbot API (Terminal 3)

```bash
cd chatbot-api

# Authenticate with GCP for Vertex AI (uses Application Default Credentials)
gcloud auth application-default login

# Set environment
export GCP_PROJECT_ID=your-gcp-project-id
export MCP_SERVER_URL=http://localhost:3000
export VERTEX_MODEL=gemini-1.5-flash
export GCP_REGION=us-central1

# Run
mvn spring-boot:run

# Expected output:
# Started ChatbotApplication in X.XXX seconds
# Tomcat started on port(s): 8080
```

**Verify:**
```bash
curl http://localhost:8080/api/chat/health
# {"status":"UP","service":"chatbot-api"}
```

---

### 4d. Web Widget (Terminal 4 / Browser)

```bash
# Simply open the demo page in your browser
# No server needed — it's static HTML

# macOS
open web-widget/index.html

# Linux
xdg-open web-widget/index.html

# Or serve it:
cd web-widget && python3 -m http.server 8090
# Open http://localhost:8090
```

The widget is pre-configured to call `http://localhost:8080/api/chat`.

---

## 5. Testing Locally

### Manual API Tests

```bash
# ── Test 1: Health checks ─────────────────────────────────────────────
curl http://localhost:8081/api/business/health
curl http://localhost:8080/api/chat/health

# ── Test 2: Jargon lookup (via Business API indirectly) ───────────────
curl "http://localhost:8081/api/business/sla/status"

# ── Test 3: Chat API end-to-end ───────────────────────────────────────
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What does SLA mean?"}' | python3 -m json.tool

# ── Test 4: Chat with context ─────────────────────────────────────────
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Show me all critical events from the last 24 hours"}' \
  | python3 -m json.tool

# ── Test 5: Business Data API endpoints ──────────────────────────────
curl "http://localhost:8081/api/business/entities/ENT-001/summary"
curl "http://localhost:8081/api/business/events/active?severity=CRITICAL&lastHours=48"
curl "http://localhost:8081/api/business/kpi/report?from=2026-01-01&to=2026-06-30"

# ── Test 6: Multi-turn conversation ──────────────────────────────────
curl -s -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Which entities are at risk?",
    "history": [
      {"role": "user", "text": "What does SLA mean?"},
      {"role": "assistant", "text": "SLA stands for Service Level Agreement..."}
    ]
  }' | python3 -m json.tool
```

### Expected Test Results

| Test | Expected Response Contains |
|------|--------------------------|
| SLA meaning | "Service Level Agreement" |
| Critical events | JSON array of events with `severity: CRITICAL` |
| ENT-001 summary | `healthScore`, `healthLabel`, `uptimePercent` |
| KPI report | `avgUptimePercent`, `slaCompliancePercent`, `overallHealthLabel` |

---

## 6. GCP Prerequisites & Bootstrap

### 6.1 Create GCP Project & Set Variables

```bash
# Set your project ID
export PROJECT_ID="your-unique-project-id"
export REGION="us-central1"
export CLOUD_SQL_INSTANCE="chatbot-db"
export CLOUD_SQL_CONNECTION_NAME="${PROJECT_ID}:${REGION}:${CLOUD_SQL_INSTANCE}"
export DB_NAME="app_db"
export DB_USER="app_user"

# Authenticate
gcloud auth login
gcloud config set project $PROJECT_ID
```

### 6.2 Enable APIs

```bash
cd infra
export PROJECT_ID=your-project-id
chmod +x *.sh
./01-enable-apis.sh

# Manually verify:
gcloud services list --enabled --filter="name:(run OR sql OR aiplatform)"
```

### 6.3 Create Service Accounts

```bash
./02-service-accounts.sh

# Verify:
gcloud iam service-accounts list --filter="email:*sa@*"
```

### 6.4 Create Cloud SQL Instance

```bash
# Create PostgreSQL instance (takes ~5 minutes)
gcloud sql instances create $CLOUD_SQL_INSTANCE \
  --database-version=POSTGRES_15 \
  --tier=db-f1-micro \
  --region=$REGION \
  --storage-type=SSD \
  --storage-size=10GB

# Create database
gcloud sql databases create $DB_NAME \
  --instance=$CLOUD_SQL_INSTANCE

# Create app user
gcloud sql users create $DB_USER \
  --instance=$CLOUD_SQL_INSTANCE \
  --password="$(openssl rand -hex 16)"

# Run schema via Cloud SQL Auth Proxy
./cloud-sql-proxy $CLOUD_SQL_CONNECTION_NAME --port=5433 &
sleep 5
PGPASSWORD="your-set-password" psql -h localhost -p 5433 -U $DB_USER -d $DB_NAME \
  -f ../db/01-schema.sql
PGPASSWORD="your-set-password" psql -h localhost -p 5433 -U $DB_USER -d $DB_NAME \
  -f ../db/02-seed-data.sql
```

### 6.5 Create Secrets

```bash
export DB_PASSWORD="your-cloud-sql-user-password"
./03-secrets.sh

# Verify secrets exist:
gcloud secrets list
# postgres-password
# biz-api-key
```

### 6.6 Set Up Vertex AI RAG Corpus

```bash
# Authenticate Python SDK
gcloud auth application-default login

# Install Python dependencies
cd ../rag-sync
pip install -r requirements.txt

# Create GCS bucket for RAG documents
gsutil mb -p $PROJECT_ID -l $REGION gs://${PROJECT_ID}-rag-corpus

# Create RAG corpus (Python)
python3 - << 'PY'
import os
import vertexai
from vertexai.preview import rag

vertexai.init(project=os.environ['PROJECT_ID'], location='us-central1')

corpus = rag.create_corpus(
    display_name='Chatbot Knowledge Base',
    embedding_model_config=rag.EmbeddingModelConfig(
        publisher_model='publishers/google/models/text-embedding-004'
    )
)
print("RAG Corpus created:", corpus.name)
print("Save this value as RAG_CORPUS_NAME:", corpus.name)
PY

# Save the corpus name
export RAG_CORPUS_NAME="projects/YOUR_PROJECT/locations/us-central1/ragCorpora/YOUR_ID"

# Run initial sync
export GCS_BUCKET="${PROJECT_ID}-rag-corpus"
python3 sync_to_rag.py
```

---

## 7. Deploy: Business Data API

```bash
cd business-data-api

# Build JAR
mvn clean package -DskipTests

# Build and push Docker image
gcloud builds submit \
  --tag gcr.io/$PROJECT_ID/biz-data-api:latest .

# Deploy to Cloud Run
gcloud run deploy biz-data-api \
  --image gcr.io/$PROJECT_ID/biz-data-api:latest \
  --region $REGION \
  --service-account biz-api-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --add-cloudsql-instances $CLOUD_SQL_CONNECTION_NAME \
  --no-allow-unauthenticated \
  --ingress internal \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID}" \
  --set-env-vars "DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-env-vars "CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME}" \
  --set-env-vars "API_KEY_ENABLED=true" \
  --min-instances 1 \
  --max-instances 5 \
  --memory 512Mi \
  --cpu 1

# Get URL
export BIZ_API_URL=$(gcloud run services describe biz-data-api \
  --region=$REGION --format='value(status.url)')
echo "Business Data API deployed: $BIZ_API_URL"
```

**Verify deployment:**
```bash
# From Cloud Shell (internal network)
curl ${BIZ_API_URL}/api/business/health
# {"status":"UP","service":"business-data-api"}
```

---

## 8. Deploy: MCP Server

```bash
cd mcp-server

# Build
npm run build

# Get biz API key from Secret Manager
export BIZ_API_KEY=$(gcloud secrets versions access latest \
  --secret=biz-api-key --project=$PROJECT_ID)

# Build and push Docker image
gcloud builds submit \
  --tag gcr.io/$PROJECT_ID/mcp-server:latest .

# Deploy
gcloud run deploy mcp-server \
  --image gcr.io/$PROJECT_ID/mcp-server:latest \
  --region $REGION \
  --service-account mcp-server-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --add-cloudsql-instances $CLOUD_SQL_CONNECTION_NAME \
  --no-allow-unauthenticated \
  --ingress internal \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID}" \
  --set-env-vars "DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-env-vars "CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME}" \
  --set-env-vars "BIZ_API_URL=${BIZ_API_URL}" \
  --set-env-vars "BIZ_API_KEY=${BIZ_API_KEY}" \
  --min-instances 0 \
  --max-instances 3 \
  --memory 512Mi

export MCP_SERVER_URL=$(gcloud run services describe mcp-server \
  --region=$REGION --format='value(status.url)')
echo "MCP Server deployed: $MCP_SERVER_URL"
```

---

## 9. Deploy: Chatbot API

```bash
cd chatbot-api

mvn clean package -DskipTests

gcloud builds submit \
  --tag gcr.io/$PROJECT_ID/chatbot-api:latest .

gcloud run deploy chatbot-api \
  --image gcr.io/$PROJECT_ID/chatbot-api:latest \
  --region $REGION \
  --service-account chatbot-api-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --allow-unauthenticated \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID}" \
  --set-env-vars "MCP_SERVER_URL=${MCP_SERVER_URL}" \
  --set-env-vars "VERTEX_MODEL=gemini-1.5-flash" \
  --set-env-vars "GCP_REGION=${REGION}" \
  --min-instances 1 \
  --max-instances 10 \
  --memory 1Gi \
  --cpu 1

export CHATBOT_URL=$(gcloud run services describe chatbot-api \
  --region=$REGION --format='value(status.url)')
echo "Chatbot API deployed: $CHATBOT_URL"

# Test live:
curl -s -X POST ${CHATBOT_URL}/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What does SLA mean?"}' | python3 -m json.tool
```

---

## 10. Deploy: RAG Sync

```bash
cd rag-sync

# Build and push
gcloud builds submit \
  --tag gcr.io/$PROJECT_ID/rag-sync:latest .

# Create Cloud Scheduler job for nightly sync (11 PM UTC)
gcloud scheduler jobs create http rag-nightly-sync \
  --location=$REGION \
  --schedule="0 23 * * *" \
  --uri="https://us-central1-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${PROJECT_ID}/jobs/rag-sync:run" \
  --message-body='{}' \
  --oauth-service-account-email="rag-sync-sa@${PROJECT_ID}.iam.gserviceaccount.com"

# Or trigger manually:
gcloud run jobs create rag-sync \
  --image gcr.io/$PROJECT_ID/rag-sync:latest \
  --region $REGION \
  --service-account rag-sync-sa@${PROJECT_ID}.iam.gserviceaccount.com \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID},RAG_CORPUS_NAME=${RAG_CORPUS_NAME}" \
  --set-env-vars "DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-env-vars "CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME}" \
  --set-env-vars "GCS_BUCKET=${PROJECT_ID}-rag-corpus"

gcloud run jobs execute rag-sync --region=$REGION
```

---

## 11. Deploy: Web Widget

```bash
cd web-widget

# Create public GCS bucket for hosting
export WIDGET_BUCKET="${PROJECT_ID}-widget"
gsutil mb -p $PROJECT_ID gs://$WIDGET_BUCKET

# Make publicly readable
gsutil iam ch allUsers:objectViewer gs://$WIDGET_BUCKET

# Update widget to point to your production API
sed -i "s|http://localhost:8080|${CHATBOT_URL}|g" index.html

# Upload with caching headers
gsutil -h "Cache-Control:public,max-age=3600" \
  cp chatbot-widget.js gs://$WIDGET_BUCKET/

gsutil cp index.html gs://$WIDGET_BUCKET/

export WIDGET_URL="https://storage.googleapis.com/${WIDGET_BUCKET}/chatbot-widget.js"
echo "Widget URL: $WIDGET_URL"

# Embed in any web app:
echo '<script src="'$WIDGET_URL'" data-api-url="'$CHATBOT_URL'/api/chat" defer></script>'
```

---

## 12. Verify End-to-End

```bash
echo "=== Final Deployment Summary ==="
echo "Business Data API : $BIZ_API_URL"
echo "MCP Server        : $MCP_SERVER_URL"
echo "Chatbot API       : $CHATBOT_URL"
echo "Web Widget JS     : $WIDGET_URL"
echo ""
echo "=== Live End-to-End Test ==="

# Test 1: Jargon question
curl -s -X POST ${CHATBOT_URL}/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"What does RAG mean in our AI system?"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('ANSWER:', d['answer'][:200])"

# Test 2: Operational question
curl -s -X POST ${CHATBOT_URL}/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Show me entities with SLA risk"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('ANSWER:', d['answer'][:200])"

echo ""
echo "=== Embed widget with: ==="
echo '<script src="'$WIDGET_URL'" data-api-url="'$CHATBOT_URL'/api/chat" data-title="AI Assistant" defer></script>'
```

---

## 13. Troubleshooting

### Common Issues & Fixes

| Issue | Symptom | Fix |
|-------|---------|-----|
| Vertex AI 403 | `PERMISSION_DENIED` | Confirm service account has `roles/aiplatform.user`. Run `gcloud auth application-default login` locally |
| DB connection refused | `Connection refused on port 5432` | Start Cloud SQL Auth Proxy or local PostgreSQL |
| MCP tool not found | Empty context in chat response | Verify MCP Server is running and `MCP_SERVER_URL` env var is correct |
| API key rejected | `401 Invalid or missing API key` | Set `API_KEY_ENABLED=false` for local dev OR pass correct key in `X-API-Key` header |
| Cloud Run 503 | Service unavailable | Check `gcloud run services describe SERVICE --region=REGION` for deployment errors |
| RAG empty results | Generic answers without data context | Run `rag-sync` manually and verify corpus import completed |
| Widget CORS error | Browser console `CORS policy` | Confirm `@CrossOrigin(origins = "*")` on `ChatController` and chatbot-api is deployed with `--allow-unauthenticated` |

### Useful Debug Commands

```bash
# View Cloud Run logs
gcloud run services logs read chatbot-api --region=$REGION --limit=50
gcloud run services logs read biz-data-api --region=$REGION --limit=50
gcloud run services logs read mcp-server --region=$REGION --limit=50

# Check service status
gcloud run services list --region=$REGION

# Verify secret exists
gcloud secrets versions access latest --secret=biz-api-key

# Test internal service (from Cloud Shell)
curl -H "X-API-Key: $(gcloud secrets versions access latest --secret=biz-api-key)" \
  ${BIZ_API_URL}/api/business/health
```

---

*For Option B MCP setup, see: `docs/OPTION-B-MCP-SETUP.md`*  
*For local dev quickstart, see: `docs/LOCAL-DEV-GUIDE.md`*
