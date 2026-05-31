# MCP Inventory Server — OAuth 2.1 Secured

A Quarkus-based MCP server secured with **OAuth 2.1 Authorization Code + PKCE** and **Token Exchange**, integrated with GitHub Copilot.

> For the full story — the analogy, design decisions, and deep-dive — see **[blog.md](./blog.md)**

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              AI Agent (GitHub Copilot)                  │
│         Authorization Code + PKCE Flow                  │
└────────────────────────┬────────────────────────────────┘
                         │ Bearer Token (mcp:* scopes)
                         ▼
┌─────────────────────────────────────────────────────────┐
│              MCP Inventory Server  :8085                │
│  ✅ Validates inbound token (iss, aud, scopes)           │
│  🔄 Token Exchange  →  api:inventory:* scopes           │
└────────────────────────┬────────────────────────────────┘
                         │ Exchanged Token (api:inventory:* scopes)
                         ▼
┌─────────────────────────────────────────────────────────┐
│           Inventory Resource Server  :9080              │
│     🔒 Scope-based access control per endpoint          │
└────────────────────────┬────────────────────────────────┘
                         │ Issues & Validates Tokens
┌─────────────────────────────────────────────────────────┐
│            Keycloak (OAuth 2.1 Provider)  :9443         │
└─────────────────────────────────────────────────────────┘
```

---

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

---

## Local Setup

### 1. Start Keycloak

```bash
cd oauth2-provider
./start.sh
```

Keycloak starts at `https://localhost:9443`. Admin console: `https://localhost:9443/admin` (admin/admin).

### 2. Start the Resource Server

```bash
cd inventory-resource-server
./mvnw quarkus:dev
```

Runs at `http://localhost:9080`.

### 3. Start the MCP Server

```bash
cd inventory-mcp
./mvnw quarkus:dev
```

Runs at `http://localhost:8085`.

### 4. Configure GitHub Copilot

Add to `~/.config/github-copilot/intellij/mcp.json`:

```json
{
  "servers": {
    "inventory-tools": {
      "url": "http://localhost:8085/mcp"
    }
  }
}
```

GitHub Copilot auto-discovers the OAuth endpoints via:
```
http://localhost:8085/.well-known/oauth-authorization-server
```

No token configuration needed — Copilot initiates the Authorization Code + PKCE flow automatically on first use.

---

## Available MCP Tools

| Tool | Required Scope | Description |
|------|---------------|-------------|
| `listTopEventsByCity` | `mcp:events:read` | List top 10 upcoming tech conferences in a city |
| `availableTicketsAndPrice` | `mcp:tickets:read` | Check ticket availability and price for an event |
| `reserveEvent` | `mcp:reservations:write` | Reserve tickets — restricted to full-access users |

---

## Test Users

| User | Password | Scopes |
|------|----------|--------|
| `demo` | `demo` | `mcp:events:read`, `mcp:tickets:read`, `mcp:reservations:write` |
| `demo-readonly` | `demo` | `mcp:events:read`, `mcp:tickets:read` |

`demo-readonly` will receive a **403 Forbidden** when calling `reserveEvent`.

---

## Switching Users

Keycloak maintains browser SSO sessions — simply closing the tab is not enough. To force a fresh login:

**Step 1 — Clear the Keycloak SSO session:**

```bash
open "https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/logout?post_logout_redirect_uri=http://localhost&client_id=oauth2-playbook-mcp-agent"
```

**Step 2 — Re-authenticate in Copilot:**

1. Open the **Tools** panel in Copilot Chat (🔨 hammer icon)
2. **Stop** the `inventory-tools` MCP server
3. **Start** it again — a browser login prompt will appear
4. Sign in with the desired user

---

## Keycloak Clients

| Client ID | Type | Used By |
|-----------|------|---------|
| `oauth2-playbook-mcp-agent` | Public (PKCE) | AI Agent — GitHub Copilot |
| `oauth2-playbook-mcp-inventory` | Confidential | MCP Server — Token Exchange |
| `oauth2-playbook-inventory` | Confidential | Resource Server — token introspection |

---

## Scopes Reference

| MCP Scope | API Scope (after exchange) | Endpoint |
|-----------|--------------------------|---------|
| `mcp:events:read` | `api:inventory:events:read` | `GET /api/inventory/events` |
| `mcp:tickets:read` | `api:inventory:tickets:read` | `GET /api/inventory/events/{id}/tickets` |
| `mcp:reservations:write` | `api:inventory:reservations:write` | `POST /api/inventory/reservations` |

---

## Security Notes

> ⚠️ **Redirect URI:** The Keycloak client `oauth2-playbook-mcp-agent` is configured with `"redirectUris": ["*"]`. This is required in development because GitHub Copilot uses a dynamic localhost port for its PKCE callback. **Never use wildcard redirect URIs in production** — register exact URIs instead. See [blog.md](./blog.md) for details.

---

## Project Structure

```
mcp-server-quarkus/
├── inventory-mcp/                   # MCP Server (Quarkus, :8085)
│   └── src/main/java/dev/mcpserver/inventory/
│       ├── InventoryTools.java                        # Tool definitions + scope checks
│       ├── service/SecurityGatewayService             # Token validation
│       ├── service/InventoryProxyService              # Token exchange + API calls
│       └── resource/OAuthAuthorizationServerMetadata  # /.well-known endpoint
├── inventory-resource-server/       # Protected REST API (Spring Boot, :9080)
├── oauth2-provider/                 # Keycloak (Docker, :9443)
│   └── imports/realm.json           # Realm, clients, users, scopes — pre-configured
└── weather-mcp/                     # Reference STDIO MCP example
```


