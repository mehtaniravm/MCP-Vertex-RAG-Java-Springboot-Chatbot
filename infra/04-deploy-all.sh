#!/bin/bash
# Step 4: Build and deploy all services to Cloud Run
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID env var}"
: "${REGION:=us-central1}"
: "${CLOUD_SQL_CONNECTION_NAME:?Set CLOUD_SQL_CONNECTION_NAME}"
: "${DB_NAME:?Set DB_NAME}"
: "${DB_USER:?Set DB_USER}"

REPO="gcr.io/${PROJECT_ID}"

echo "==> Building and deploying all services"

# ── 1. Business Data API ──────────────────────────────────────────────────────
echo "[1/4] Deploying business-data-api..."
cd "$(dirname "$0")/../business-data-api"
gcloud builds submit --tag "${REPO}/biz-data-api:latest" .
gcloud run deploy biz-data-api \
  --image "${REPO}/biz-data-api:latest" \
  --region "$REGION" \
  --service-account "biz-api-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --add-cloudsql-instances "$CLOUD_SQL_CONNECTION_NAME" \
  --no-allow-unauthenticated \
  --ingress internal \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-env-vars "CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME},API_KEY_ENABLED=true" \
  --min-instances 1 --max-instances 5

BIZ_API_URL=$(gcloud run services describe biz-data-api \
  --region="$REGION" --format='value(status.url)')
echo "Business Data API: $BIZ_API_URL"

# ── 2. MCP Server ─────────────────────────────────────────────────────────────
echo "[2/4] Deploying mcp-server..."
cd "$(dirname "$0")/../mcp-server"
gcloud builds submit --tag "${REPO}/mcp-server:latest" .
BIZ_API_KEY=$(gcloud secrets versions access latest \
  --secret=biz-api-key --project="$PROJECT_ID")
gcloud run deploy mcp-server \
  --image "${REPO}/mcp-server:latest" \
  --region "$REGION" \
  --service-account "mcp-server-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --add-cloudsql-instances "$CLOUD_SQL_CONNECTION_NAME" \
  --no-allow-unauthenticated \
  --ingress internal \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-env-vars "CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME}" \
  --set-env-vars "BIZ_API_URL=${BIZ_API_URL},BIZ_API_KEY=${BIZ_API_KEY}" \
  --min-instances 0 --max-instances 3

MCP_SERVER_URL=$(gcloud run services describe mcp-server \
  --region="$REGION" --format='value(status.url)')
echo "MCP Server: $MCP_SERVER_URL"

# ── 3. Chatbot API ────────────────────────────────────────────────────────────
echo "[3/4] Deploying chatbot-api..."
cd "$(dirname "$0")/../chatbot-api"
gcloud builds submit --tag "${REPO}/chatbot-api:latest" .
gcloud run deploy chatbot-api \
  --image "${REPO}/chatbot-api:latest" \
  --region "$REGION" \
  --service-account "chatbot-api-sa@${PROJECT_ID}.iam.gserviceaccount.com" \
  --allow-unauthenticated \
  --set-env-vars "GCP_PROJECT_ID=${PROJECT_ID},MCP_SERVER_URL=${MCP_SERVER_URL}" \
  --min-instances 1 --max-instances 10

CHATBOT_URL=$(gcloud run services describe chatbot-api \
  --region="$REGION" --format='value(status.url)')
echo "Chatbot API: $CHATBOT_URL"

# ── 4. Web Widget — upload to GCS ─────────────────────────────────────────────
echo "[4/4] Hosting web widget on GCS..."
cd "$(dirname "$0")/../web-widget"
BUCKET="${PROJECT_ID}-widget"
gsutil mb -p "$PROJECT_ID" "gs://${BUCKET}" 2>/dev/null || true
gsutil iam ch allUsers:objectViewer "gs://${BUCKET}"
gsutil -h "Cache-Control:public,max-age=3600" cp chatbot-widget.js "gs://${BUCKET}/"
WIDGET_URL="https://storage.googleapis.com/${BUCKET}/chatbot-widget.js"

echo ""
echo "==> ALL SERVICES DEPLOYED"
echo "Business Data API : $BIZ_API_URL"
echo "MCP Server        : $MCP_SERVER_URL"
echo "Chatbot API       : $CHATBOT_URL"
echo "Web Widget JS     : $WIDGET_URL"
echo ""
echo "Embed in any app:"
echo "  <script src=\"${WIDGET_URL}\" data-api-url=\"${CHATBOT_URL}/api/chat\" defer></script>"
