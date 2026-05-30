# inventory

Security-first Quarkus MCP server for conference inventory tools.

This module exposes three MCP tools and acts only as a gateway:

1. `listTopEventsByCity`
2. `availableTicketsAndPrice`
3. `reserveEvent`

Business logic stays in a downstream resource server. This MCP server validates JWT, validates tool input against OpenAPI contracts, performs token exchange with Keycloak, and forwards the call.

## Security and contract flow

Each tool request uses this sequence:

1. Validate inbound JWT from MCP HTTP request context.
2. Validate required MCP tool scope (`mcp:*`) for the selected tool.
3. Validate tool payload against `src/main/resources/openapi/inventory-mcp.yaml`.
4. Exchange token with Keycloak for downstream API scope (`api:inventory:*`).
5. Invoke downstream inventory resource API.

The OAuth metadata endpoint is public and does not require bearer token:

`GET /.well-known/oauth-authorization-server`

## Build and test

```sh
./mvnw clean verify
```

`verify` enforces JaCoCo gates at 100% line and 100% branch coverage.

## Run

```sh
./mvnw quarkus:dev
```

Package runner JAR:

```sh
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

Run packaged app:

```sh
java -jar target/inventory-1.0.0-SNAPSHOT-runner.jar
```

## Configuration highlights

Key values are in `src/main/resources/application.properties`:

1. OIDC/JWT validation settings.
2. MCP tool scope checks for inbound tokens (`inventory.security.tool-scope.*`).
3. Token exchange client credentials and downstream API scopes (`inventory.token-exchange.scope.*`).
4. Downstream inventory API base URL.
5. OAuth metadata endpoint values.

## Client token script and authentication flow

`generate-access-token.sh` generates an access token (Authorization Code + PKCE) and writes it to a **client config file only**.
They do **not** reconfigure the running MCP server.

```sh
./generate-access-token.sh --target copilot --file /Users/mani/workspace/oauth2-playbook/mcp.json
```

```sh
./generate-access-token.sh --target cline
```

### Client ID Configuration Reference

The MCP server authentication uses a **two-client architecture**:

#### Client IDs:
- **Agent Client**: `oauth2-playbook-mcp-agent` (public, PKCE-enabled)
  - Used by: Copilot, Claude, Cline, Cursor
  - Gets token via: Authorization Code + PKCE flow
  - Configured in: `generate-access-token.sh` (line 13)
  
- **Server Client**: `oauth2-playbook-mcp-inventory` (confidential, with client secret)
  - Used by: MCP server for token exchange
  - Gets token via: Client Credentials grant
  - Configured in: `application.properties` (inventory.token-exchange.client-id)

#### Configuration Flow:

```
┌──────────────────────────────────────────────────────────────┐
│ Step 1: Agent Obtains Token                                  │
├──────────────────────────────────────────────────────────────┤
│ generate-access-token.sh:                                    │
│   CLIENT_ID=oauth2-playbook-mcp-agent                        │
│   SCOPES="openid mcp:events:read mcp:tickets:read ..."       │
│                                                              │
│ ↓ Authorization Code + PKCE Flow ↓                           │
│                                                              │
│ Keycloak issues JWT containing:                              │
│   - azp (authorized party): oauth2-playbook-mcp-agent        │
│   - aud (audience): oauth2-playbook-mcp-inventory            │
│   - scope: mcp:events:read mcp:tickets:read mcp:...          │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 2: Token Stored in mcp.json                             │
├──────────────────────────────────────────────────────────────┤
│ mcp.json (inventory-tools):                                  │
│   "url": "http://localhost:8085/mcp"                         │
│   "Authorization": "Bearer eyJhbGc..."                       │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 3: Copilot Sends Request to MCP Server                  │
├──────────────────────────────────────────────────────────────┤
│ HTTP POST /mcp                                               │
│ Authorization: Bearer <JWT_TOKEN>                            │
│ Body: { tool: "listTopEventsByCity", ... }                  │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 4: MCP Server Validates Token                           │
├──────────────────────────────────────────────────────────────┤
│ application.properties:                                      │
│   quarkus.oidc.client-id=oauth2-playbook-mcp-agent           │
│                                                              │
│ Validation checklist:                                        │
│   ✓ JWT signature valid (using JWKS endpoint)               │
│   ✓ Token not expired (iat, exp claims)                     │
│   ✓ Issuer matches: https://localhost:9443/realms/...      │
│   ✓ Audience includes: oauth2-playbook-mcp-inventory        │
│   ✓ Required scopes present: mcp:events:read, etc.          │
└──────────────────────────────────────────────────────────────┘
                          ↓
┌──────────────────────────────────────────────────────────────┐
│ Step 5: MCP Server Exchanges Token (if needed)               │
├──────────────────────────────────────────────────────────────┤
│ For downstream API calls, MCP server:                        │
│   Client: oauth2-playbook-mcp-inventory                      │
│   Secret: secret (from application.properties)              │
│   ↓ Token Exchange Grant ↓                                   │
│   Keycloak issues NEW token with scopes:                    │
│   - api:inventory:events:read                                │
│   - api:inventory:tickets:read                               │
│   - api:inventory:reservations:write                         │
└──────────────────────────────────────────────────────────────┘
```

#### Key Points:
1. **Agent always uses**: `oauth2-playbook-mcp-agent` (public client, no secret needed)
2. **Server always uses**: `oauth2-playbook-mcp-inventory` (secret client, has secret)
3. **Token validation**: MCP server uses JWT validation (not introspection) to avoid Keycloak permission issues
4. **Audience claim**: Agent's token has `aud: oauth2-playbook-mcp-inventory` so the server accepts it
5. **Scopes**: Agent gets `mcp:*` scopes; server exchanges them for `api:inventory:*` scopes for downstream calls


