#!/bin/bash
# Step 3: Create secrets in Secret Manager
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID env var}"
: "${DB_PASSWORD:?Set DB_PASSWORD env var}"

create_secret() {
  local NAME=$1 VALUE=$2
  echo -n "$VALUE" | gcloud secrets create "$NAME" \
    --data-file=- --project="$PROJECT_ID" \
    2>/dev/null || \
  echo -n "$VALUE" | gcloud secrets versions add "$NAME" \
    --data-file=- --project="$PROJECT_ID"
  echo "Secret created/updated: $NAME"
}

echo "==> Creating secrets"

create_secret "postgres-password" "$DB_PASSWORD"

# Generate API key for Business Data API
BIZ_API_KEY=$(openssl rand -hex 32)
create_secret "biz-api-key" "$BIZ_API_KEY"
echo "==> Business API key generated (save this): $BIZ_API_KEY"

# Grant access to secrets for each SA
grant_secret() {
  local SECRET=$1 SA=$2
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --member="serviceAccount:${SA}@${PROJECT_ID}.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor" \
    --project="$PROJECT_ID" --quiet
}

grant_secret "postgres-password" "mcp-server-sa"
grant_secret "postgres-password" "biz-api-sa"
grant_secret "postgres-password" "rag-sync-sa"
grant_secret "biz-api-key"       "mcp-server-sa"
grant_secret "biz-api-key"       "biz-api-sa"

echo "==> Secrets configured"
