#!/usr/bin/env bash
# start.sh – boots the full inventory MCP stack in the correct order:
#   1. Keycloak (OAuth2 provider) via Docker
#   2. inventory-resource-server (REST API, port 9080)
#   3. inventory MCP server (HTTP/SSE, port 8085)
#   4. Generates a fresh token via PKCE and updates MCP config
#
# Usage:
#   ./start.sh              # runs everything and configures MCP auth
#   ./start.sh --no-config  # skip MCP auth configuration

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RESOURCE_SERVER_DIR="$PROJECT_ROOT/inventory-resource-server"
PROVIDER_DIR="$PROJECT_ROOT/oauth2-provider"

CONFIGURE_MCP=true
if [[ "${1:-}" == "--no-config" || "${1:-}" == "--no-cline" ]]; then
    CONFIGURE_MCP=false
fi

# ── Step 1: Start Keycloak ────────────────────────────────────────────────────
echo ""
echo "==> [1/4] Starting Keycloak..."
cd "$PROVIDER_DIR"
bash start.sh &
KEYCLOAK_PID=$!

echo "    Waiting for Keycloak to be ready (up to 90s)..."
for i in $(seq 1 90); do
    if curl -sk https://localhost:9443/realms/oauth2-playbook/.well-known/openid-configuration > /dev/null 2>&1; then
        echo "    Keycloak is ready."
        break
    fi
    sleep 1
    if [ "$i" -eq 90 ]; then
        echo "ERROR: Keycloak did not start in time."
        exit 1
    fi
done

# ── Step 2: Start inventory resource server (port 9080) ──────────────────────
echo ""
echo "==> [2/4] Starting inventory-resource-server on port 9080..."
cd "$RESOURCE_SERVER_DIR"

if [ ! -f target/inventory-resource-server-1.0.0-SNAPSHOT-runner.jar ]; then
    echo "    Building inventory-resource-server..."
    ./mvnw package -DskipTests -q
fi

java -jar target/inventory-resource-server-1.0.0-SNAPSHOT-runner.jar &
RESOURCE_SERVER_PID=$!

echo "    Waiting for resource server..."
for i in $(seq 1 30); do
    if curl -s http://localhost:9080/q/health > /dev/null 2>&1; then
        echo "    Resource server is ready."
        break
    fi
    sleep 1
done

# ── Step 3: Start inventory MCP server (port 8085) ───────────────────────────
echo ""
echo "==> [3/4] Starting inventory MCP server on port 8085..."
cd "$SCRIPT_DIR"

if [ ! -f target/inventory-1.0.0-SNAPSHOT-runner.jar ]; then
    echo "    Building inventory MCP server..."
    ./mvnw package -DskipTests -q
fi

java -jar target/inventory-1.0.0-SNAPSHOT-runner.jar &
MCP_PID=$!

echo "    Waiting for MCP server..."
for i in $(seq 1 30); do
    if curl -s http://localhost:8085/.well-known/oauth-authorization-server > /dev/null 2>&1; then
        echo "    MCP server is ready."
        break
    fi
    sleep 1
done

# ── Step 4: Configure MCP auth via PKCE ───────────────────────────────────────
if [ "$CONFIGURE_MCP" = true ]; then
    echo ""
    echo "==> [4/4] Generating access token and updating MCP config..."
    bash "$SCRIPT_DIR/generate-access-token.sh"
fi

echo ""
echo "======================================================"
echo "  Stack is running:"
echo "  - Keycloak:               https://localhost:9443"
echo "  - Inventory Resource API: http://localhost:9080/api/inventory"
echo "  - Inventory MCP Server:   http://localhost:8085/mcp"
echo "  - OAuth Metadata:         http://localhost:8085/.well-known/oauth-authorization-server"
echo ""
echo "  PIDs: Keycloak=$KEYCLOAK_PID  ResourceServer=$RESOURCE_SERVER_PID  MCP=$MCP_PID"
echo "======================================================"
echo ""
echo "  Press Ctrl+C to stop all services."

# Wait for all background processes; stop all on Ctrl+C
trap "echo 'Stopping...'; kill $KEYCLOAK_PID $RESOURCE_SERVER_PID $MCP_PID 2>/dev/null; exit 0" INT TERM
wait

