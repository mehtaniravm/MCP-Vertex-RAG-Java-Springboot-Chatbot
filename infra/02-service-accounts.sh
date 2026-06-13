#!/bin/bash
# Step 2: Create service accounts with least-privilege IAM roles
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID env var}"

create_sa() {
  local NAME=$1 DISPLAY=$2
  gcloud iam service-accounts create "$NAME" \
    --display-name="$DISPLAY" \
    --project="$PROJECT_ID" \
    2>/dev/null || echo "SA $NAME already exists"
}

bind_role() {
  local SA=$1 ROLE=$2
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="$ROLE" --quiet
}

echo "==> Creating service accounts"

# MCP Server — read-only DB + Vertex AI caller
create_sa "mcp-server-sa" "MCP Server - Read Only"
bind_role "mcp-server-sa" "roles/cloudsql.client"
bind_role "mcp-server-sa" "roles/aiplatform.user"
bind_role "mcp-server-sa" "roles/secretmanager.secretAccessor"

# Business Data API — read-only DB
create_sa "biz-api-sa" "Business Data API"
bind_role "biz-api-sa" "roles/cloudsql.client"
bind_role "biz-api-sa" "roles/secretmanager.secretAccessor"

# Chat API — Vertex AI + Secret Manager
create_sa "chatbot-api-sa" "Chatbot Orchestration API"
bind_role "chatbot-api-sa" "roles/aiplatform.user"
bind_role "chatbot-api-sa" "roles/secretmanager.secretAccessor"

# RAG Sync — Storage + Vertex AI
create_sa "rag-sync-sa" "RAG Sync Job"
bind_role "rag-sync-sa" "roles/storage.objectAdmin"
bind_role "rag-sync-sa" "roles/aiplatform.user"
bind_role "rag-sync-sa" "roles/cloudsql.client"
bind_role "rag-sync-sa" "roles/secretmanager.secretAccessor"

echo "==> Service accounts created"
