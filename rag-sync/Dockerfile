FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY sync_to_rag.py .
CMD ["python", "sync_to_rag.py"]
