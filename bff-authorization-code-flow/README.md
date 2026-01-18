# Confidential Client -  authorization code flow + PKCE

BFF Authorization Code Flow (Backends for Frontends)

This is not a production-ready setup. It is intended for educational purposes to illustrate the OAuth2 Authorization Code Flow is achievable in back channel.

Backend clients (also known as confidential clients) can securely store client credentials (like client secret) and are capable of maintaining the confidentiality of these credentials. Therefore, they can perform the OAuth2 Authorization Code Flow without exposing sensitive information to the end-user's browser or front-end application.

## Sequence Diagram

```mermaid
sequenceDiagram
  participant User
  participant Browser(UI)
  participant Backend(BFF)
  participant AuthServer
  participant ResourceServer

  User->>Browser(UI): Clicks "Login"
  Browser(UI)->>Backend(BFF): GET /auth/start
  Backend(BFF)->>Backend(BFF): generate state/nonce, store server-side
  Backend(BFF)->>Browser(UI): 302 -> AuthServer (with state, nonce, redirect_uri)
  Browser(UI)->>AuthServer: User authenticates + consents
  AuthServer->>Browser(UI): 302 -> redirect_uri?code=...&state=...
  Browser(UI)->>Backend(BFF): Callback (contains code,state)
  Backend(BFF)->>Backend(BFF): verify state/nonce
  Backend(BFF)->>AuthServer: POST token endpoint (code exchange, client auth)
  AuthServer->>Backend(BFF): tokens (access, refresh, id_token)
  Backend(BFF)->>Backend(BFF): validate tokens, create server session, set HttpOnly Secure cookie
  Browser(UI)->>Backend(BFF): API calls with session cookie
  Backend(BFF)->>ResourceServer: call APIs using access token (server-to-server)
  ResourceServer->>Backend(BFF): API response
  Backend(BFF)->>Browser(UI): return data for UI
```




