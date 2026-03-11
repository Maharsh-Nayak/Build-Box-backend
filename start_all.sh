#!/bin/bash

# Function to kill all background jobs on script exit
cleanup() {
    echo "Stopping all servers..."
    kill $(jobs -p) 2>/dev/null
    exit
}

trap cleanup SIGINT SIGTERM

echo "Starting Build-Box Services..."

# 1. Networking Server (Port 8000)
echo "🚀 Starting Networking Server (Port 8000)..."
(cd "Build-Box-backend/Networking Server" && exec node index.js > ../networking.log 2>&1) &
PID_NET=$!

# 2. Frontend Deployment Server (Port 9000)
echo "🚀 Starting Frontend Deployment Server (Port 9000)..."
(cd "Build-Box-backend/Frontend Deployment Server" && exec ./mvnw spring-boot:run > ../frontend.log 2>&1) &
PID_FRONT=$!

# 3. Log Analytics Server (Port 9012)
echo "🚀 Starting Log Analytics Server (Port 9012)..."
(cd "Build-Box-backend/Log_Analytics_Server" && exec ./mvnw spring-boot:run > ../log_analytics.log 2>&1) &
PID_LOG=$!

# 4. BuildServer (Port 9191)
echo "🚀 Starting BuildServer (Port 9191)..."
(cd "Build-Box-backend/BuildServer" && exec ./mvnw spring-boot:run > ../build_server.log 2>&1) &
PID_BUILD=$!

# 5. Testing Frontend (Port 8080)
echo "🚀 Starting Testing Frontend (Port 8080)..."
exec npx -y serve -l 8080 test-deployment-ui > Build-Box-backend/testing_ui.log 2>&1 &
PID_TEST=$!

echo "✅ All servers started in background!"
echo "---------------------------------------------------"
echo "Networking Server PID: $PID_NET"
echo "Frontend Server PID:   $PID_FRONT"
echo "Log Analytics PID:     $PID_LOG"
echo "BuildServer PID:       $PID_BUILD"
echo "Testing UI PID:        $PID_TEST"
echo "---------------------------------------------------"
echo "Logs are being written to:"
echo "  - Build-Box-backend/networking.log"
echo "  - Build-Box-backend/frontend.log"
echo "  - Build-Box-backend/log_analytics.log"
echo "  - Build-Box-backend/build_server.log"
echo "  - Build-Box-backend/testing_ui.log"
echo "---------------------------------------------------"
echo "Press Ctrl+C to stop all servers."

wait
