#!/bin/bash
set -e

CURRENT_STEP="INIT"

push_to_redis() {
  redis-cli \
    --tls \
    --cacert /etc/ssl/certs/ca-certificates.crt \
    -h "$REDIS_HOST" \
    -p "$REDIS_PORT" \
    -a "$REDIS_PASSWORD" \
    XADD "logs:$BUILD_ID" * log "$1" > /dev/null 2>&1 || true
}

on_error() {
  exit_code=$?
  push_to_redis "❌ BUILD FAILED"
  push_to_redis "❌ FAILED AT STEP: $CURRENT_STEP"
  push_to_redis "❌ EXIT CODE: $exit_code"
  push_to_redis "__BUILD_STATUS__:FAILED"
  exit $exit_code
}

trap on_error ERR

push_step() {
  # Escape double quotes to prevent syntax errors in redis-cli
  SAFE_MSG=$(echo "$1" | sed 's/"/\\"/g')
  redis-cli \
    --tls \
    --cacert /etc/ssl/certs/ca-certificates.crt \
    -h "$REDIS_HOST" \
    -p "$REDIS_PORT" \
    -a "$REDIS_PASSWORD" \
    XADD "logs:$BUILD_ID" * log "$SAFE_MSG" > /dev/null 2>&1 || true
}

step() {
  CURRENT_STEP="$1"
  push_step "➡️ STEP START: $1"
}

main_build() {
  step "CLONE_REPO"
  git clone "$GIT_URL" repo
  cd repo

  step "INSTALL_FRONTEND_DEPS"
  cd "$FRONTENT_DIR"
  npm install

  step "BUILD_FRONTEND"
  npm run build -- --base=./

  step "UPLOAD_FRONTEND"
  aws s3 sync dist/ s3://buildbox-frontend/"$USER_ID"/"$PROJECT_NAME"/Frontend --delete

  cd ..

  step "INSTALL_BACKEND_DEPS"
  cd "$BACKEND_DIR"
  npm install

  cd ..

  step "UPLOAD_BACKEND"
  aws s3 sync "$BACKEND_DIR"/ s3://buildbox-frontend/"$USER_ID"/"$PROJECT_NAME"/Backend --delete
}

main_build 2>&1 | tee /tmp/build.log

# Redirect all output to console + file
exec > >(tee /tmp/build.log) 2>&1

main_build

# Push logs line-by-line to Redis
while IFS= read -r line; do
  push_to_redis "$line"
done < /tmp/build.log

push_to_redis "✅ BUILD SUCCESS"
push_to_redis "__BUILD_STATUS__:SUCCESS"
