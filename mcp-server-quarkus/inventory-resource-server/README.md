# inventory-resource-server

Quarkus REST resource server that serves as the **downstream API** for the `inventory` MCP server.

## Endpoints

| Method | Path | Required scope | Description |
|--------|------|----------------|-------------|
| `GET`  | `/api/inventory/events` | `api:inventory:events:read` | List conference events, filtered by `?city=` and sorted by date (max `?limit=` results) |
| `GET`  | `/api/inventory/events/{eventId}/tickets` | `api:inventory:tickets:read` | Return available ticket count and price for a specific event |
| `POST` | `/api/inventory/reservations` | `api:inventory:reservations:write` | Reserve tickets (body: `{ eventId, quantity, username }`) |

All endpoints require:
1. A valid **Bearer JWT** issued by Keycloak (validated by Quarkus OIDC).
2. A **scope claim** in the JWT matching the endpoint's `@ScopesAllowed` annotation (enforced by `AccessController`).

## Authorization flow

```
MCP Client
  |-- Bearer JWT --> MCP Server (port 8085)
                          |-- token exchange --> Keycloak
                          |<- scoped token -----
                          |-- Bearer <scoped token> --> inventory-resource-server (port 9080)
                                                              |-- OIDC JWT validation
                                                              |-- Scope check (@ScopesAllowed)
                                                              `-- Business logic
```

## Security architecture

| Component | Role |
|-----------|------|
| `AccessController` | JAX-RS `ContainerRequestFilter` - reads `scope` claim from JWT and enforces `@ScopesAllowed` |
| `ScopesAllowed` | Method-level annotation declaring required scopes |
| Quarkus OIDC | Validates JWT signature, issuer, and audience (`oauth2-playbook-inventory-resource-server`) |

## Build and run

```sh
./mvnw package
java -jar target/inventory-resource-server-1.0.0-SNAPSHOT-runner.jar
```

Or in dev mode:
```sh
./mvnw quarkus:dev
```

## Local startup order

```sh
# 1) start Keycloak/provider first (existing project script)
cd /Users/mani/workspace/oauth2-playbook/mcp-server-quarkus/inventory/oauth2-provider
./start.sh
```

```sh
# 2) start inventory resource server (port 9080)
cd /Users/mani/workspace/oauth2-playbook/mcp-server-quarkus/inventory-resource-server
./mvnw quarkus:dev
```

```sh
# 3) package and run inventory MCP server (port 8085), then register with Claude MCP
cd /Users/mani/workspace/oauth2-playbook/mcp-server-quarkus/inventory
./mvnw package -Dquarkus.package.jar.type=uber-jar
claude mcp add inventory-tools --scope local -- java -jar /Users/mani/workspace/oauth2-playbook/mcp-server-quarkus/inventory/target/inventory-1.0.0-SNAPSHOT-runner.jar
```

## Keycloak setup

1. Create client `oauth2-playbook-inventory-resource-server` in realm `oauth2-playbook`.
2. Enable **Token Exchange** for the MCP server client (`oauth2-playbook-mcp-inventory`) to exchange tokens for this resource server.
3. Add API scopes `api:inventory:events:read`, `api:inventory:tickets:read`, `api:inventory:reservations:write` to the realm and assign them to token-exchange + resource-server clients.

## Configuration

Key `application.properties` values:

| Property | Default | Purpose |
|----------|---------|---------|
| `quarkus.http.port` | `9080` | Matches `quarkus.rest-client.inventory-api.url` in the MCP server |
| `quarkus.oidc.auth-server-url` | Keycloak realm URL | JWT validation issuer |
| `quarkus.oidc.client-id` | `oauth2-playbook-inventory-resource-server` | Audience check |

