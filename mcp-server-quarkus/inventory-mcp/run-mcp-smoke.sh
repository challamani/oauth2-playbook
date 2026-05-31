#!/usr/bin/env bash
set -euo pipefail

MCP_URL="${MCP_URL:-http://localhost:8085/mcp}"
CONFIG_FILE="${CONFIG_FILE:-/Users/mani/workspace/oauth2-playbook/mcp.json}"
CITY="${CITY:-London}"

if [[ ! -f "$CONFIG_FILE" ]]; then
  echo "ERROR: Config file not found: $CONFIG_FILE" >&2
  exit 1
fi

token_line=$(grep -m1 '"Authorization"' "$CONFIG_FILE" || true)
if [[ -z "$token_line" ]]; then
  echo "ERROR: Authorization header not found in $CONFIG_FILE" >&2
  exit 1
fi

token=${token_line#*Bearer }
token=${token%%\"*}

if [[ "$token" == *"<generated-at-runtime"* || -z "$token" ]]; then
  echo "ERROR: Bearer token placeholder found. Run generate-access-token.sh first." >&2
  exit 1
fi

init_payload='{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"smoke","version":"1.0"}}}'
call_payload=$(printf '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"listTopEventsByCity","arguments":{"city":"%s"}}}' "$CITY")

headers_file=$(mktemp -t mcp-init-headers)

curl -sS -D "$headers_file" -o /dev/null -X POST "$MCP_URL" \
  -H "Authorization: Bearer $token" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-03-26' \
  --data-binary "$init_payload"

session_id=$(awk 'BEGIN{IGNORECASE=1} /^Mcp-Session-Id:/ {gsub("\r", "", $2); print $2}' "$headers_file")
if [[ -z "$session_id" ]]; then
  echo "ERROR: MCP session id missing from initialize response." >&2
  exit 1
fi

curl -sS -X POST "$MCP_URL" \
  -H "Authorization: Bearer $token" \
  -H "Mcp-Session-Id: $session_id" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-03-26' \
  --data-binary '{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}' >/dev/null

curl -sS -X POST "$MCP_URL" \
  -H "Authorization: Bearer $token" \
  -H "Mcp-Session-Id: $session_id" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json, text/event-stream' \
  -H 'MCP-Protocol-Version: 2025-03-26' \
  --data-binary "$call_payload"

