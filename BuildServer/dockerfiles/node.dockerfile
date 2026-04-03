FROM node:18-alpine

# Set working directory
WORKDIR /app

# Copy only dependency files first (better caching)
COPY package*.json ./

# Install only production dependencies
RUN npm install --production

# Copy rest of the application
COPY . .

# Platform-controlled environment
ENV PORT=3000

EXPOSE 3000

# Start backend with fallback logic
CMD ["sh", "-c", "if [ -f package.json ] && grep -q '\"start\"' package.json; then npm start; elif [ -f server.js ]; then node server.js; else node index.js; fi"]
