# Securing AI Agents with OAuth 2.1: Authorization Code + PKCE and Token Exchange

*A practical deep-dive using GitHub Copilot, Quarkus MCP, and Keycloak*

---

## The Problem: AI Agents Need Identity

AI agents are no longer just chat interfaces. They call APIs, query databases, and take real-world actions like booking conference tickets. This raises a critical question:

> **Who is the AI agent acting on behalf of? And what is it allowed to do?**

Without proper identity and authorization, an AI agent is essentially an unauthenticated bot with unlimited access to your backend.

This post walks through how we solved this using a **GitHub Copilot MCP server** that lets developers query tech conferences and reserve seats — based on their role.

---

## The Scenario: Conference Tickets for Your Team

> Your company sends engineers to tech conferences — QCon, Devoxx, JavaOne, and even free community events like the [Manchester Java Community Unconference](http://jmanc.org/). Every developer wants to know what events are coming up and whether tickets are available. But only the **team lead** has the corporate credit card. Only they can reserve tickets — though for the free ones, everyone can just grab a seat!

This maps directly to OAuth 2.1 scopes:

- `mcp:events:read` — browse events — every developer
- `mcp:tickets:read` — check availability — every developer
- `mcp:reservations:write` — reserve tickets — **team lead only**

---

## Why OAuth 2.1 Authorization Code + PKCE?

PKCE (Proof Key for Code Exchange) prevents authorization code interception attacks. It is mandatory for all public clients in OAuth 2.1.

How it works:

1. Client generates a random **code verifier**
2. Hashes it to produce a **code challenge** (SHA-256)
3. The challenge is sent with the authorization request
4. The verifier is sent when exchanging the code for a token
5. Keycloak validates: `SHA256(verifier) == challenge`

This means even if someone intercepts the authorization code, they cannot exchange it without the original verifier — which never leaves the client.

Enforced in Keycloak: `"pkce.code.challenge.method": "S256"`

---

## Why Token Exchange?

The AI agent receives a token with `mcp:*` scopes — the vocabulary of the MCP protocol layer. The resource server speaks a different language: `api:inventory:*` scopes. These are different trust boundaries with different audiences.

The MCP server performs a **Token Exchange (RFC 8693)** — it presents the agent's token to Keycloak and receives a narrowly scoped token for the backend API:

```
Agent Token                         Exchanged Token
+---------------------------+       +----------------------------------+
| aud: mcp-agent            | ----> | aud: mcp-inventory               |
| scope: mcp:events:read    |       | scope: api:inventory:events:read |
+---------------------------+       +----------------------------------+
```

The raw agent token never reaches the resource server. This is the **principle of least privilege** applied across service boundaries.

Keycloak validates during exchange:
- The incoming token is valid and not expired
- The MCP server client (`oauth2-playbook-mcp-inventory`) is permitted to perform token exchange
- The requested scopes are a subset of what the user actually has

### Token Exchange Cannot Escalate Privileges

This is a critical security guarantee of RFC 8693: **token exchange can only narrow scopes, never widen them.**

The scopes available in the exchanged token are strictly bounded by what the authenticated user has in their original agent token — which in turn is bounded by what Keycloak has mapped to that user's role:

```
Keycloak Role Mapping
┌──────────────────────────────────┬──────────────────────────────────────────────────────┐
│ Role                             │ Scopes granted at login                              │
├──────────────────────────────────┼──────────────────────────────────────────────────────┤
│ oauth2-playbook-inventory-full   │ mcp:events:read, mcp:tickets:read,                   │
│ (demo user)                      │ mcp:reservations:write                               │
├──────────────────────────────────┼──────────────────────────────────────────────────────┤
│ oauth2-playbook-inventory-       │ mcp:events:read, mcp:tickets:read                    │
│ readonly (demo-readonly user)    │ (NO reservations:write)                              │
└──────────────────────────────────┴──────────────────────────────────────────────────────┘
```

So when `demo-readonly` triggers a `reserveEvent` tool call:

1. Their agent token contains no `mcp:reservations:write` scope — Keycloak never issued it at login
2. The MCP server's scope check (`requireAuthenticatedUser(reservationsWriteToolScope)`) fails immediately
3. Even if it didn't, the token exchange request for `api:inventory:reservations:write` would be **rejected by Keycloak** — you cannot exchange for a scope the subject user was never granted
4. The resource server never even sees the request

The 403 is not just enforced at one layer — it is structurally impossible to bypass at any layer of the chain.

---

## How It Is Built

### OAuth Metadata Auto-Discovery

The MCP server exposes a `/.well-known/oauth-authorization-server` endpoint. GitHub Copilot reads this automatically and initiates the Authorization Code + PKCE flow — no manual token configuration needed in `mcp.json`:

```json
{
  "issuer": "https://localhost:9443/realms/oauth2-playbook",
  "authorization_endpoint": "https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/auth",
  "token_endpoint": "https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/token",
  "code_challenge_methods_supported": ["S256"]
}
```

### Scope Enforcement at the Tool Level

Every MCP tool checks scopes before executing:

```java
@Tool(description = "Reserve tickets for one event at a time")
public ReservationResult reserveEvent(...) {
    // Validates mcp:reservations:write — throws 403 for demo-readonly user
    AuthenticatedUser user = securityGatewayService.requireAuthenticatedUser(reservationsWriteToolScope);
    // token exchange + API call only happens if scope check passes
}
```

### Full Request Flow

```
AI Agent (Copilot)
    |
    | 1. Authorization Code + PKCE --> Keycloak
    | 2. Receives Bearer Token (mcp:* scopes)
    |
    v
MCP Server (Port 8085)
    |
    | 3. Validates inbound token (iss, aud, scopes)
    | 4. Token Exchange --> Keycloak (api:inventory:* scopes)
    |
    v
Resource Server (Port 9080)
    |
    | 5. Validates exchanged token
    | 6. Enforces scope-based access on each endpoint
    |
    v
Response back to AI Agent
```

---

## Testing with Different Users

Two users are pre-configured to demonstrate scope-based access control:

| User | Role | Events | Tickets | Reserve |
|------|------|--------|---------|---------|
| `demo` | Team Lead | yes | yes | **yes** |
| `demo-readonly` | Developer | yes | yes | **403 Forbidden** |

Password for both users: `demo`

### Switching Users — Clearing the SSO Session

Keycloak maintains browser SSO sessions. Simply logging out of one tab is not enough — the session persists. To force a completely fresh login with a different user, run:

```bash
open "https://localhost:9443/realms/oauth2-playbook/protocol/openid-connect/logout?post_logout_redirect_uri=http://localhost&client_id=oauth2-playbook-mcp-agent"
```

This clears the Keycloak SSO cookie. Then:

1. Open the **Tools** panel in Copilot Chat (hammer icon)
2. **Stop** the `inventory-tools` MCP server
3. **Start** it again — a browser login prompt will appear
4. Sign in with `demo-readonly` — and try `reserveEvent` to see the 403

---

## The Redirect URI Problem in Development

The current Keycloak config registers `"redirectUris": ["*"]` for the MCP agent client. This was required because GitHub Copilot uses a **dynamic localhost port** for its PKCE callback — for example `http://127.0.0.1:33428/callback`. Keycloak does not support port-level wildcards like `http://127.0.0.1:*`.

> **Never use wildcard redirect URIs in production.**
> It enables authorization code theft through open redirect attacks — an attacker can craft a malicious redirect URI, intercept the code, and exchange it for a token.

Production approaches:
- Use a **fixed callback port** and register the exact URI
- Route all callbacks through a **reverse proxy** with a stable URL
- For native desktop agents, follow RFC 8252 (loopback interface redirection)

---

## Key Takeaways

| Concept | Why It Matters |
|---------|---------------|
| Authorization Code + PKCE | Secure for public clients — no client secret embedded |
| Token Exchange (RFC 8693) | Enforces trust boundaries between protocol layers |
| Scope-based access control | Fine-grained permissions per user — same API, different access |
| OAuth Metadata Discovery | AI agents auto-configure via `/.well-known` — zero manual setup |
| SSO session logout | Critical for switching test users in development |
| Audience validation | Tokens are locked to their intended service — prevents misuse |

---

## References

- [RFC 7636 — PKCE](https://datatracker.ietf.org/doc/html/rfc7636)
- [RFC 8693 — Token Exchange](https://datatracker.ietf.org/doc/html/rfc8693)
- [RFC 8252 — OAuth for Native Apps](https://datatracker.ietf.org/doc/html/rfc8252)
- [OAuth 2.1 Draft](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1)
- [Quarkus MCP Server](https://docs.quarkiverse.io/quarkus-mcp-server/dev/index.html)
- [Keycloak Token Exchange](https://www.keycloak.org/docs/latest/securing_apps/#_token-exchange)

---

*For local setup, test users, switching between users, and project structure — see [README.md](./README.md)*



