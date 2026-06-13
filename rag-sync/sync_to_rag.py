"""
RAG Corpus Sync Script
Exports jargon dictionary from PostgreSQL and syncs to Vertex AI RAG corpus.
Run nightly via Cloud Scheduler.

Usage:
  python sync_to_rag.py

Environment variables:
  GCP_PROJECT_ID          - GCP project ID
  CLOUD_SQL_CONNECTION_NAME - Cloud SQL instance connection name
  DB_NAME                 - PostgreSQL database name
  DB_USER                 - Database username
  RAG_CORPUS_NAME         - Full Vertex AI RAG corpus resource name
  GCS_BUCKET              - GCS bucket name for corpus documents
"""
import os
import json
import logging
from datetime import datetime

import psycopg2
import vertexai
from vertexai.preview import rag
from google.cloud import storage, secretmanager_v1

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
log = logging.getLogger(__name__)


def get_secret(project_id: str, secret_name: str) -> str:
    """Fetch a secret value from GCP Secret Manager."""
    client = secretmanager_v1.SecretManagerServiceClient()
    name   = f"projects/{project_id}/secrets/{secret_name}/versions/latest"
    resp   = client.access_secret_version(request={"name": name})
    return resp.payload.data.decode("utf-8").strip()


def get_db_connection(project_id: str):
    """Create a PostgreSQL connection using Cloud SQL socket or local host."""
    cloud_sql_conn = os.environ.get("CLOUD_SQL_CONNECTION_NAME")
    db_pass = get_secret(project_id, os.environ.get("DB_PASSWORD_SECRET", "postgres-password"))

    if cloud_sql_conn:
        # Cloud Run / Cloud SQL socket
        host = f"/cloudsql/{cloud_sql_conn}"
        conn = psycopg2.connect(
            host=host,
            dbname=os.environ["DB_NAME"],
            user=os.environ["DB_USER"],
            password=db_pass,
        )
    else:
        # Local dev
        conn = psycopg2.connect(
            host=os.environ.get("DB_HOST", "localhost"),
            port=int(os.environ.get("DB_PORT", "5432")),
            dbname=os.environ["DB_NAME"],
            user=os.environ["DB_USER"],
            password=db_pass,
        )
    return conn


def export_jargon_to_text(conn) -> str:
    """Fetch jargon dictionary and format as plain text for RAG indexing."""
    cursor = conn.cursor()
    cursor.execute("""
        SELECT short_form, full_form, description, domain, example_usage
        FROM jargon_dictionary
        ORDER BY domain, short_form
    """)
    rows = cursor.fetchall()
    log.info("Fetched %d jargon terms from database", len(rows))

    documents = []
    for short_form, full_form, description, domain, example in rows:
        doc = f"""Term: {short_form}
Full Form: {full_form}
Domain: {domain or 'General'}
Description: {description or 'No description available.'}
Example: {example or 'N/A'}"""
        documents.append(doc)

    return "\n\n---\n\n".join(documents)


def upload_to_gcs(content: str, bucket_name: str, blob_name: str) -> str:
    """Upload text content to GCS and return the gs:// URI."""
    client = storage.Client()
    bucket = client.bucket(bucket_name)
    blob   = bucket.blob(blob_name)
    blob.upload_from_string(content, content_type="text/plain")
    uri = f"gs://{bucket_name}/{blob_name}"
    log.info("Uploaded to %s", uri)
    return uri


def sync_to_rag_corpus(project_id: str, corpus_name: str, gcs_uri: str):
    """Import the GCS file into the Vertex AI RAG corpus."""
    vertexai.init(project=project_id, location="us-central1")
    log.info("Importing into RAG corpus: %s", corpus_name)
    rag.import_files(
        corpus_name=corpus_name,
        paths=[gcs_uri],
        chunk_size=512,
        chunk_overlap=50,
    )
    log.info("RAG corpus sync complete")


def main():
    project_id   = os.environ["GCP_PROJECT_ID"]
    corpus_name  = os.environ["RAG_CORPUS_NAME"]
    bucket_name  = os.environ.get("GCS_BUCKET", f"{project_id}-rag-corpus")
    timestamp    = datetime.utcnow().strftime("%Y%m%d_%H%M%S")
    blob_name    = f"jargon/jargon_dictionary_{timestamp}.txt"

    log.info("Starting RAG sync — project: %s", project_id)

    with get_db_connection(project_id) as conn:
        jargon_text = export_jargon_to_text(conn)

    gcs_uri = upload_to_gcs(jargon_text, bucket_name, blob_name)
    sync_to_rag_corpus(project_id, corpus_name, gcs_uri)

    log.info("RAG sync finished successfully")


if __name__ == "__main__":
    main()
