# Refresh Token Grant Flow
 
 This is not a production-ready setup. It is intended for educational purposes to illustrate the OAuth2 Refresh Token Grant Flow.

## Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant ClientApp
    participant AuthServer
    participant ResourceServer
    User->>ClientApp: (1)Access protected resource
    ClientApp->>AuthServer: (2)Redirect to Authorization Endpoint with PKCE, with offline_access or standard refresh scope.
    AuthServer->>User: (3)Prompt for login and consent
    User->>AuthServer: (4)Submit credentials and consent
    AuthServer->>ClientApp: (5)Redirect back with Authorization Code
    ClientApp->>AuthServer: (6)Exchange Code for Access Token + Refresh Token
    AuthServer->>ClientApp: (7)Return Access Token + Refresh Token
    ClientApp->>ResourceServer: (8)Access protected resource with Access Token
    ResourceServer->>ClientApp: (9)Return protected resource
    Note over ClientApp, AuthServer: Access Token expires
    ClientApp->>AuthServer: (10)Request new Access Token using Refresh Token
    AuthServer->>ClientApp: (11)Return new Access Token
    ClientApp->>ResourceServer: (12)Access protected resource with new Access Token
    ResourceServer->>ClientApp: (13)Return protected resource
```

*This grant type implementation is not yet completed in oauth2-playbook*.