#!/bin/bash

# Define directories
NGINX_CONF_DIR="/tmp/nginx/conf.d"
MAIN_CONF="/tmp/nginx/nginx.conf"

# Create directories if they don't exist
mkdir -p "$NGINX_CONF_DIR"

# Create main nginx.conf if it doesn't exist
if [ ! -f "$MAIN_CONF" ]; then
    cat > "$MAIN_CONF" << 'EOF'
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/conf.d/*.conf;
    
    # Default server
    server {
        listen 8080 default_server;
        server_name _;
        
        location / {
            return 200 'BuildBox Nginx Router is Running!\n';
            add_header Content-Type text/plain;
        }
    }
}
EOF
fi

# Remove existing container if it exists
docker rm -f buildbox-nginx 2>/dev/null || true

# Start Nginx container
echo "🚀 Starting Nginx Router on port 8080..."
docker run -d --name buildbox-nginx \
  -p 8080:8080 \
  -v "$MAIN_CONF":/etc/nginx/nginx.conf:ro \
  -v "$NGINX_CONF_DIR":/etc/nginx/conf.d:ro \
  --restart always \
  nginx:alpine

echo "✅ Nginx Router started!"
echo "👉 http://localhost:8080"
