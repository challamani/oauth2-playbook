# Confidential Client -  authorization code flow + PKCE

BFF Authorization Code Flow (Backends for Frontends)

This is not a production-ready setup. It is intended for educational purposes to illustrate the OAuth2 Authorization Code Flow is achievable in back channel.

Backend clients (also known as confidential clients) can securely store client credentials (like client secret) and are capable of maintaining the confidentiality of these credentials. Therefore, they can perform the OAuth2 Authorization Code Flow without exposing sensitive information to the end-user's browser or front-end application.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant Browser as ReactUI(Browser)
    participant BFF as ConfidentialClient
    participant KC as Keycloak(IdP)
    participant RS as Quarkus Resource Server

    Note over Browser, BFF: Phase 1: Initiation
    Browser->>BFF: GET /api/login
    Note over BFF: 1. Generate random 'code_verifier'
    Note over BFF: 2. Compute 'code_challenge' = Base64Url(SHA256(verifier))
    Note over BFF: 3. Generate 'state' (CSRF protection)
    Note over BFF: 4. Store verifier & state in encrypted Session Cookie (q_session)
    
    BFF-->>Browser: 302 Redirect to Keycloak /authorize
    Note right of Browser: Includes: client_id, code_challenge, state, redirect_uri

    Note over Browser, KC: Phase 2: Keycloak Authentication
    Browser->>KC: GET /authorize?client_id=...&code_challenge=...
    KC->>Browser: Display Login Page
    Browser->>KC: POST credentials (Username/Password)
    Note over KC: Verify Credentials
    KC-->>Browser: 302 Redirect to BFF /callback
    Note left of Browser: Includes: authorization_code & state

    Note over Browser, BFF: Phase 3: Token Exchange (Back-channel)
    Browser->>BFF: GET /callback?code=AUTH_CODE&state=STATE
    
    Note over BFF: 5. Verify 'state' matches session
    Note over BFF: 6. Retrieve 'code_verifier' from session
    
    BFF->>KC: POST /token (Server-to-Server)
    Note right of BFF: Payload: code, redirect_uri, grant_type=authorization_code,<br/>code_verifier, client_id, client_secret
    
    Note over KC: 7. Validate client_secret
    Note over KC: 8. Verify: Hash(code_verifier) == stored code_challenge
    KC-->>BFF: 200 OK (Access Token, ID Token, Refresh Token)

    Note over BFF: 9. Encrypt Tokens into 'q_session' Cookie
    BFF-->>Browser: 302 Redirect to /dashboard
    Note left of Browser: 'Set-Cookie: q_session=ENCRYPTED_BLOB; HttpOnly; Secure'

    Note over Browser, RS: Phase 4: Resource Access
    Browser->>BFF: GET /api/data
    Note right of Browser: Cookie: q_session=...
    
    Note over BFF: 10. Decrypt Cookie & extract Access Token
    BFF->>RS: GET /resource/protected
    Note right of BFF: Header: Authorization: Bearer <JWT>
    
    Note over RS: 11. Validate JWT (Signature, Iss, Aud, Exp)
    RS-->>BFF: 200 OK (Data JSON)
    BFF-->>Browser: 200 OK (Data JSON)
```




