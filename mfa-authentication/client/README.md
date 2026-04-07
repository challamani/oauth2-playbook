# MFA Authentication Client

React single-page application that authenticates against Keycloak using Authorization Code + PKCE. The Keycloak realm is configured to **require multi-factor authentication (Password + TOTP)**, so every login goes through both factors.

After login the app decodes the ID Token and displays the authentication-context claims (`acr`, `amr`, `auth_time`) that prove MFA was used.

## Prerequisites

- Node.js 18+
- The Keycloak OAuth2 provider must be running (see parent README)

## Install & Run

```bash
cd mfa-authentication/client
npm install
HTTPS=true npm start
```

The app starts at `https://localhost:3000` and will redirect you to Keycloak for login automatically.

## First Login (TOTP Setup)

On the very first login Keycloak will prompt you to **configure a one-time password (TOTP)**:

1. Sign in with `demo` / `demo123`.
2. Keycloak presents a QR code — scan it with an authenticator app (Google Authenticator, FreeOTP, Authy, Microsoft Authenticator, etc.).
3. Enter the 6-digit code shown in your authenticator app to complete setup.
4. On every subsequent login you will need your password **and** the current TOTP code.

## What the App Shows

After authentication the Home page displays:

- **Authentication Context** table — `acr`, `amr`, `auth_time` claims with explanations.
- **User Profile** — decoded profile claims from the ID Token.
- **Full Decoded ID Token** — every claim in the token as JSON.
- **Raw JWT** — the encoded token string you can paste into [jwt.io](https://jwt.io) for independent verification.

