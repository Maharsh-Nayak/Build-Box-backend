#!/bin/bash

# Configuration
LOG_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Help message
show_help() {
    echo "BuildBox Log Viewer"
    echo "Usage: ./view_logs.sh [service_name]"
    echo ""
    echo "Services:"
    echo "  networking  - View Networking Server logs (Port 8000)"
    echo "  frontend    - View Frontend Deployment Server logs (Port 9000)"
    echo "  analytics   - View Log Analytics Server logs (Port 9012)"
    echo "  build       - View BuildServer logs (Port 9191)"
    echo "  ui          - View Testing Frontend logs (Port 8080)"
    echo "  all         - Tail all logs simultaneously"
    echo ""
    echo "Example: ./view_logs.sh build"
}

# Check argument
case $1 in
    "networking")
        tail -f "$LOG_DIR/networking.log"
        ;;
    "frontend")
        tail -f "$LOG_DIR/frontend.log"
        ;;
    "analytics")
        tail -f "$LOG_DIR/log_analytics.log"
        ;;
    "build")
        tail -f "$LOG_DIR/build_server.log"
        ;;
    "ui")
        tail -f "$LOG_DIR/testing_ui.log"
        ;;
    "all")
        tail -f "$LOG_DIR/"*.log
        ;;
    *)
        show_help
        ;;
esac
