#!/bin/sh
set -e

echo "Cloning repo..."
git clone "$GIT_URL" repo
cd repo

cd "$FRONTENT_DIR"

echo "Installing deps..."
npm install

echo "Building..."
npm run build -- --base=./

echo "Uploading to S3..."
aws s3 sync dist/ s3://buildbox-frontend/"$USER_ID"/"$PROJECT_NAME"/Frontend --delete

echo "Frontend Complete !!"

cd ..

cd "$BACKEND_DIR"

echo "Installing backend node modules"
npm install

cd ..

echo "Uploading backend"
aws s3 sync $BACKEND_DIR/ s3://buildbox-frontend/$USER_ID/$PROJECT_NAME/Backend --delete

echo "Backend Complete !!!"
