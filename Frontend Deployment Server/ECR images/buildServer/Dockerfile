FROM node:20-alpine

# Install system tools needed for the build processes
RUN apk add --no-cache \
    git \
    python3 \
    make \
    g++ \
    aws-cli \
    bash

WORKDIR /app

# Copy package files first to leverage Docker cache
COPY package.json .

# Install the script dependencies (ioredis)
RUN npm install

# Copy the build script
COPY build.js .

# Make sure we have permissions (optional but good practice)
RUN chmod +x build.js

# Run Node
ENTRYPOINT ["node", "build.js"]