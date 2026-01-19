#!/bin/sh

set -o pipefail

CURRENT_STEP="INIT"

push_to_redis() {
  redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" \
    XADD "logs:$BUILD_ID" * log "$1" > /dev/null 2>&1
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

step() {
  CURRENT_STEP="$1"
  push_to_redis "➡️ STEP START: $1"
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

main_build 2>&1 | while IFS= read -r line; do
  echo "$line"
  push_to_redis "$line"
done

push_to_redis "✅ BUILD SUCCESS"
push_to_redis "__BUILD_STATUS__:SUCCESS"