FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

ENV PORT=5000

EXPOSE 5000

# Start Flask app with gunicorn (production) or flask directly
CMD ["sh", "-c", "if command -v gunicorn > /dev/null; then gunicorn --bind 0.0.0.0:$PORT app:app; else python app.py; fi"]
