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

# Start backend
CMD ["node", "server.js"]
