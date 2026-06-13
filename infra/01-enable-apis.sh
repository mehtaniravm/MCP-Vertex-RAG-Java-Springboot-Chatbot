#!/bin/bash
# Step 1: Enable all required GCP APIs
set -euo pipefail

: "${PROJECT_ID:?Set PROJECT_ID env var}"
: "${REGION:=us-central1}"

echo "==> Enabling GCP APIs for project: $PROJECT_ID"

gcloud config set project "$PROJECT_ID"

gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  aiplatform.googleapis.com \
  discoveryengine.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  vpcaccess.googleapis.com \
  cloudscheduler.googleapis.com \
  storage.googleapis.com

echo "==> Creating Artifact Registry repository"
gcloud artifacts repositories create ai-chatbot \
  --repository-format=docker \
  --location="$REGION" \
  --description="AI Chatbot container images" \
  2>/dev/null || echo "Repository already exists"

echo "==> APIs enabled successfully"
