# OAuth2 Playbook

This repository contains example implementations, reference configurations, and best-practice notes for several OAuth 2.0 and OpenID Connect flows. It's intended as a hands-on playbook to learn, demo and test common flows, provider setups, and resource-server integration.

## Objective

- Provide minimal, runnable examples for common OAuth2 flows so developers can experiment locally.
- Demonstrate how to wire a provider, clients, and resource servers together for testing and learning.
- Capture best-practices and configuration tips for secure deployments.

## Top-level structure

- `authorization-code-flow/` — examples and docs for the Authorization Code flow.
- `client-credentials-flow/` — example clients and notes for machine-to-machine flows.
- `device-code-flow/` — device flow example: contains a `client/` helper and an `oauth2-provider/` test Keycloak setup.
	- `device-code-flow/client/create-user.sh` — example script that uses the device flow to obtain a token and call the example API.
	- `device-code-flow/oauth2-provider/` — local provider compose files, certs and import realms.
- `resource-server/` — example resource server and a sample application under `example-app/`.
- `best-practices/` — recommendations and notes for secure OAuth2 deployments.

## Quick start (local)

1. Start the provider (see the `device-code-flow/oauth2-provider/start.sh` or provider README).
2. Run a client example in the corresponding flow folder (e.g. `device-code-flow/client/create-user.sh`).
3. Inspect the `resource-server/example-app` for how to validate access tokens and protect endpoints.

## Notes

- Secrets and certificates live under provider folders (for local testing). Add these paths to `.gitignore` if you want to keep them out of Git.
- The examples aim to be minimal; treat them as learning material — do not use sample secrets in production.

- IMPORTANT: Each scenario folder contains its own README with flow-specific setup and run instructions.
	See the README inside the relevant directory (for example, `device-code-flow/`, `authorization-code-flow/`, or `client-credentials-flow/`) before running examples.

If you'd like, I can add short run commands for each flow or a single script to boot all local services.