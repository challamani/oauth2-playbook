# OAuth2 Playbook

This repository contains example implementations, reference configurations, and best-practice notes for several OAuth 2.0 and OpenID Connect flows. It's intended as a hands-on playbook to learn, demo and test common flows, provider setups, and resource-server integration.

## Objective

- Provide minimal, runnable examples for common OAuth2 flows so developers can experiment locally.
- Demonstrate how to wire a provider, clients, and resource servers together for testing and learning.
- Capture best-practices and configuration tips for secure deployments.

## History

OAuth 2.0 was published to simplify delegated authorization for web and mobile apps (see the official spec links below). It replaced earlier, more complex signature-based approaches (OAuth 1.0) and many proprietary sign-in or API-auth schemes that relied on sharing passwords or custom tokens.

## Common Use Cases

- Delegated authorization for web applications and single-page apps (Authorization Code, PKCE).
- Native and mobile apps using PKCE to avoid embedding secrets.
- Machine-to-machine (service) access using the Client Credentials grant.
- Device and constrained-input scenarios using the Device Authorization Grant.
- Single Sign-On and identity claims via OpenID Connect (an identity layer on OAuth 2.0).

## What came before OAuth2 (brief)

- Password-based API access or HTTP Basic Auth (clients held user credentials).
- Proprietary single-sign-on or API key systems with no standardized token lifetimes or scopes.
- OAuth 1.0 and OAuth 1.0a used cryptographic request signing (more complex but avoided sending raw passwords). Many large providers migrated from those approaches to OAuth 2.0 and OpenID Connect for better developer ergonomics and standardized flows.


## Top-level structure

- `authorization-code-flow/` — examples and docs for the Authorization Code flow.
- `client-credentials-flow/` — example clients and notes for machine-to-machine flows.
- `device-code-flow/` — device flow example: contains a `client/` helper and an `oauth2-provider/` test Keycloak setup.
	- `device-code-flow/client/create-user.sh` — example script that uses the device flow to obtain a token and call the example API.
	- `device-code-flow/oauth2-provider/` — local provider compose files, certs and import realms.
- `resource-server/` — example resource server and a sample application under `resource-server/`.
- `best-practices/` — recommendations and notes for secure OAuth2 deployments.

## Quick start (local)

1. Start the provider (see the `device-code-flow/oauth2-provider/start.sh` or provider README).
2. Run a client example in the corresponding flow folder (e.g. `device-code-flow/client/create-user.sh`).
3. Inspect the `resource-server/resource-server` for how to validate access tokens and protect endpoints.

## Notes

- Secrets and certificates live under provider folders (for local testing). Add these paths to `.gitignore` if you want to keep them out of Git.
- The examples aim to be minimal; treat them as learning material — do not use sample secrets in production.

- IMPORTANT: Each scenario folder contains its own README with flow-specific setup and run instructions.
	See the README inside the relevant directory (for example, `device-code-flow/`, `authorization-code-flow/`, or `client-credentials-flow/`) before running examples.

If you'd like, I can add short run commands for each flow or a single script to boot all local services.

## References & Best Practices

- OAuth 2.0 (RFC 6749): https://tools.ietf.org/html/rfc6749
- Bearer Token Usage (RFC 6750): https://tools.ietf.org/html/rfc6750
- OAuth 2.0 Threat Model and Security Considerations (RFC 6819): https://tools.ietf.org/html/rfc6819
- OAuth 2.0 for Native Apps (RFC 8252): https://tools.ietf.org/html/rfc8252
- OpenID Connect Core 1.0: https://openid.net/specs/openid-connect-core-1_0.html
- OAuth Security Best Current Practice (IETF drafts & guidance): https://datatracker.ietf.org/doc/html/rfc9700

Suggested practical best-practices (short): use Authorization Code + PKCE for public clients, avoid implicit flow, prefer short-lived access tokens with refresh tokens where appropriate, validate audience and scopes on resource servers, use HTTPS everywhere, and follow the IETF and provider security guidance linked above.