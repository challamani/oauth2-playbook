#!/usr/bin/env bash
# generate-access-token.sh
#
# Gets an access token from Keycloak using Authorization Code + PKCE,
# then updates client-side MCP config only (never reconfigures the MCP server).

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/../.." && pwd)

AUTHORIZATION_ENDPOINT="${AUTHORIZATION_ENDPOINT:-https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/auth}"
TOKEN_ENDPOINT="${TOKEN_ENDPOINT:-https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/token}"
CLIENT_ID="${CLIENT_ID:-oauth2-playbook-mcp-agent}"
REDIRECT_URI="${REDIRECT_URI:-http://localhost:8086/callback}"
MCP_SERVER_URL="${MCP_SERVER_URL:-http://localhost:8085/mcp}"
SCOPES="${SCOPES:-openid mcp:events:read mcp:tickets:read mcp:reservations:write}"

MCP_JSON_DEFAULT="$REPO_ROOT/mcp.json"
CLINE_SETTINGS_DEFAULT="$HOME/Library/Application Support/Code/User/globalStorage/saoudrizwan.claude-dev/settings/cline_mcp_settings.json"
COPILOT_MCP_JSON_DEFAULT="$HOME/.config/github-copilot/intellij/mcp.json"

TARGET="mcp-json"
TARGET_FILE="$MCP_JSON_DEFAULT"
ACCESS_TOKEN=""
FILE_EXPLICIT=false

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  --access-token TOKEN      Skip browser auth and use this token directly.
  --target mcp-json|cline|copilot   Where to write client MCP auth (default: mcp-json).
  --file PATH               Custom target file path.
  -h, --help                Show this help.

Defaults:
  mcp-json target: $MCP_JSON_DEFAULT
  cline target:    $CLINE_SETTINGS_DEFAULT
  copilot target: $COPILOT_MCP_JSON_DEFAULT
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --access-token)
      [[ $# -ge 2 ]] || { echo "ERROR: --access-token requires a value" >&2; exit 1; }
      ACCESS_TOKEN="$2"
      shift 2
      ;;
    --target)
      [[ $# -ge 2 ]] || { echo "ERROR: --target requires a value" >&2; exit 1; }
      TARGET="$2"
      shift 2
      ;;
    --file)
      [[ $# -ge 2 ]] || { echo "ERROR: --file requires a value" >&2; exit 1; }
      TARGET_FILE="$2"
      FILE_EXPLICIT=true
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "ERROR: Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ "$TARGET" != "mcp-json" && "$TARGET" != "cline" && "$TARGET" != "copilot" ]]; then
  echo "ERROR: Unsupported target '$TARGET'. Use mcp-json, cline, or copilot." >&2
  exit 1
fi

if [[ "$FILE_EXPLICIT" == false && "$TARGET_FILE" == "$MCP_JSON_DEFAULT" && "$TARGET" == "cline" ]]; then
  TARGET_FILE="$CLINE_SETTINGS_DEFAULT"
fi

if [[ "$FILE_EXPLICIT" == false && "$TARGET_FILE" == "$MCP_JSON_DEFAULT" && "$TARGET" == "copilot" ]]; then
  TARGET_FILE="$COPILOT_MCP_JSON_DEFAULT"
fi

if [[ ! -f "$TARGET_FILE" ]]; then
  echo "ERROR: Target file not found: $TARGET_FILE" >&2
  exit 1
fi

if [[ -z "$ACCESS_TOKEN" ]]; then
  CODE_VERIFIER=$(python3 -c 'import secrets; print(secrets.token_urlsafe(96)[:128])')
  CODE_CHALLENGE=$(python3 - "$CODE_VERIFIER" <<'PY'
import base64
import hashlib
import sys

verifier = sys.argv[1].encode("utf-8")
challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier).digest()).decode("utf-8").rstrip("=")
print(challenge)
PY
)

  AUTH_URL=$(python3 - "$AUTHORIZATION_ENDPOINT" "$CLIENT_ID" "$REDIRECT_URI" "$SCOPES" "$CODE_CHALLENGE" <<'PY'
import sys
import urllib.parse

endpoint, client_id, redirect_uri, scopes, challenge = sys.argv[1:6]
params = {
    "response_type": "code",
    "client_id": client_id,
    "redirect_uri": redirect_uri,
    "scope": scopes,
    "code_challenge": challenge,
    "code_challenge_method": "S256",
}
print(endpoint + "?" + urllib.parse.urlencode(params))
PY
)

  echo "==> Initiating Authorization Code Flow with PKCE"
  echo "==> Opening browser for authentication"
  open "$AUTH_URL"

  echo "==> Waiting for redirect on $REDIRECT_URI"
  AUTH_CODE=$(python3 - "$REDIRECT_URI" <<'PY'
import http.server
import threading
import urllib.parse
import sys

redirect_uri = sys.argv[1]
parts = urllib.parse.urlparse(redirect_uri)
host = parts.hostname or "localhost"
port = parts.port or 8086
path = parts.path or "/callback"

result = {"code": ""}

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        if parsed.path != path:
            self.send_response(404)
            self.end_headers()
            return

        params = urllib.parse.parse_qs(parsed.query)
        result["code"] = params.get("code", [""])[0]

        self.send_response(200)
        self.send_header("Content-Type", "text/plain; charset=utf-8")
        self.end_headers()
        if result["code"]:
            self.wfile.write(b"Authentication complete. You can close this tab.")
        else:
            self.wfile.write(b"No authorization code found. You can close this tab.")

        threading.Thread(target=self.server.shutdown, daemon=True).start()

    def log_message(self, format, *args):
        return

server = http.server.ThreadingHTTPServer((host, port), Handler)
try:
    server.serve_forever()
finally:
    server.server_close()

print(result["code"])
PY
)

  if [[ -z "$AUTH_CODE" ]]; then
    echo "ERROR: Failed to capture authorization code from callback." >&2
    exit 1
  fi

  echo "==> Exchanging authorization code for access token"
  TOKEN_RESPONSE=$(curl -sk \
    -X POST "$TOKEN_ENDPOINT" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=authorization_code" \
    -d "client_id=$CLIENT_ID" \
    -d "code=$AUTH_CODE" \
    -d "redirect_uri=$REDIRECT_URI" \
    -d "code_verifier=$CODE_VERIFIER")

  ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | python3 -c 'import json,sys; print(json.load(sys.stdin).get("access_token", ""))' 2>/dev/null || true)

  if [[ -z "$ACCESS_TOKEN" ]]; then
    echo "ERROR: Failed to obtain access token. Is Keycloak running at https://localhost:9443?" >&2
    echo "Response: $TOKEN_RESPONSE" >&2
    exit 1
  fi
fi

echo "==> Updating inventory-tools auth in $TARGET_FILE"
python3 - "$TARGET_FILE" "$MCP_SERVER_URL" "$ACCESS_TOKEN" "$TARGET" <<'PY'
import json
import sys
import urllib.error
import urllib.request

settings_path, mcp_url, token, target = sys.argv[1:5]

def strip_jsonc_comments(text):
    # Supports // and /* */ comments while preserving string contents.
    result = []
    i = 0
    in_string = False
    in_line_comment = False
    in_block_comment = False
    escaped = False

    while i < len(text):
        c = text[i]
        n = text[i + 1] if i + 1 < len(text) else ""

        if in_line_comment:
            if c == "\n":
                in_line_comment = False
                result.append(c)
            i += 1
            continue

        if in_block_comment:
            if c == "*" and n == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue

        if in_string:
            result.append(c)
            if escaped:
                escaped = False
            elif c == "\\":
                escaped = True
            elif c == '"':
                in_string = False
            i += 1
            continue

        if c == '"':
            in_string = True
            result.append(c)
            i += 1
            continue

        if c == "/" and n == "/":
            in_line_comment = True
            i += 2
            continue

        if c == "/" and n == "*":
            in_block_comment = True
            i += 2
            continue

        result.append(c)
        i += 1

    return "".join(result)

with open(settings_path, "r", encoding="utf-8") as f:
    raw_text = f.read()

settings = json.loads(strip_jsonc_comments(raw_text))

# If the file already uses the Copilot schema, avoid writing mcpServers into it.
effective_target = target
if target == "mcp-json" and isinstance(settings.get("servers"), dict) and "mcpServers" not in settings:
    effective_target = "copilot"

if effective_target == "copilot":
    existing_server = settings.setdefault("servers", {}).get("inventory-tools", {})
    existing_request_init = existing_server.get("requestInit", {}) if isinstance(existing_server, dict) else {}
    existing_headers = existing_request_init.get("headers", {}) if isinstance(existing_request_init, dict) else {}
    settings["servers"]["inventory-tools"] = {
        "url": mcp_url,
        "requestInit": {
            "headers": {
                **existing_headers,
                "Authorization": f"Bearer {token}"
            }
        }
    }
else:
    existing_server = settings.setdefault("mcpServers", {}).get("inventory-tools", {})
    existing_auto_approve = existing_server.get("autoApprove", [])
    settings["mcpServers"]["inventory-tools"] = {
        "disabled": False,
        "timeout": 60,
        "type": "streamable-http",
        "url": mcp_url,
        "headers": {
            "Authorization": f"Bearer {token}"
        },
        # Keep existing values if live discovery fails later.
        "autoApprove": existing_auto_approve
    }

def parse_jsonrpc_tools(payload):
    if not isinstance(payload, dict):
        return []
    result = payload.get("result", {})
    tools = result.get("tools", [])
    names = []
    for tool in tools:
        if isinstance(tool, dict) and isinstance(tool.get("name"), str):
            names.append(tool["name"])
    return names

def post_jsonrpc(method, params=None):
    body = json.dumps({
        "jsonrpc": "2.0",
        "id": 1,
        "method": method,
        "params": params or {}
    }).encode("utf-8")
    req = urllib.request.Request(
        mcp_url,
        data=body,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json, text/event-stream",
            "MCP-Protocol-Version": "2025-03-26"
        },
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=8) as response:
        session_id = response.headers.get("Mcp-Session-Id", "")
        content = response.read().decode("utf-8")
    return json.loads(content), session_id

def discover_tools():
    # Some MCP servers require initialize/session before tools/list.
    init_payload, session_id = post_jsonrpc("initialize", {
        "protocolVersion": "2025-03-26",
        "capabilities": {},
        "clientInfo": {
            "name": "generate-access-token",
            "version": "1.0"
        }
    })

    # Best effort: send initialized notification if a session was created.
    if session_id:
        body = json.dumps({
            "jsonrpc": "2.0",
            "method": "notifications/initialized"
        }).encode("utf-8")
        req = urllib.request.Request(
            mcp_url,
            data=body,
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json",
                "Accept": "application/json, text/event-stream",
                "MCP-Protocol-Version": "2025-03-26",
                "Mcp-Session-Id": session_id
            },
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=8):
            pass

    list_body = json.dumps({
        "jsonrpc": "2.0",
        "id": 2,
        "method": "tools/list",
        "params": {}
    }).encode("utf-8")
    list_headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
        "Accept": "application/json, text/event-stream",
        "MCP-Protocol-Version": "2025-03-26"
    }
    if session_id:
        list_headers["Mcp-Session-Id"] = session_id

    list_req = urllib.request.Request(mcp_url, data=list_body, headers=list_headers, method="POST")
    with urllib.request.urlopen(list_req, timeout=8) as response:
        list_payload = json.loads(response.read().decode("utf-8"))
    return parse_jsonrpc_tools(list_payload)

discovered_tools = []
discovery_error = ""
try:
    discovered_tools = discover_tools()
except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, ValueError) as ex:
    discovery_error = str(ex)

if discovered_tools:
    if effective_target == "copilot":
        # Copilot discovers tools from the server; no autoApprove field is used here.
        pass
    else:
        settings["mcpServers"]["inventory-tools"]["autoApprove"] = discovered_tools

with open(settings_path, "w", encoding="utf-8") as f:
    json.dump(settings, f, indent=2)
    f.write("\n")

print("Target settings updated.")
if discovered_tools:
    print("Available tools:")
    for name in discovered_tools:
        print(f"  - {name}")
else:
    print("Warning: Could not discover tools from MCP server.")
    if effective_target != "copilot":
        print("Kept existing autoApprove values.")
    if discovery_error:
        print(f"Discovery error: {discovery_error}")

if effective_target != target and target == "mcp-json":
    print("Detected Copilot-style config (servers). Updated servers.inventory-tools only.")
PY

echo "Done."


