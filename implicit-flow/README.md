# Implicit Flow

This is not a production-ready setup. It is intended for educational purposes to illustrate the OAuth2 Implicit Flow.

This is not a recommended flow for modern applications due to security concerns and the availability of more secure alternatives like the Authorization Code Flow with PKCE.


## Sequence Diagram

```mermaid
sequenceDiagram
    participant User
    participant ClientApp
    participant AuthServer
    participant ResourceServer
    User->>ClientApp: (1)Access protected resource
    ClientApp->>AuthServer: (2)Redirect to Authorization Endpoint
    AuthServer->>User: (3)Prompt for login and consent
    User->>AuthServer: (4)Submit credentials and consent
    AuthServer->>ClientApp: (5)Redirect back with Access Token in URL fragment
    ClientApp->>ResourceServer: (6)Access protected resource with Access Token
    ResourceServer->>ClientApp: (7)Return protected resource
```

*This grant type implementation is not yet completed in oauth2-playbook*.

## When to use Implicit Flow

The Implicit Flow is generally discouraged for modern applications due to security concerns, as it exposes access tokens directly in the URL fragment, which can be intercepted or leaked. However, it may be considered in legacy scenarios where:

- the client application is a single-page application (SPA) that cannot securely store client secrets.
- there is a need for immediate access to tokens without an additional round trip to exchange an authorization for an access token.

In most cases, it is recommended to use the Authorization Code Flow with PKCE for SPAs and other public clients to enhance security and mitigate risks associated with token exposure.