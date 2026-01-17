# Authorization Code Flow with PKCE Example

## Create React App Skeleton

If you don't already have the client app, create it and add required packages:

```bash
cd ./authorization-code-flow
npx create-react-app client
cd client
npm install react-router-dom oidc-client-ts
```

## Add authConfig in the Client App

- Create a new folder `src/auth`
- Add `authConfig.js` and `authService.js` files based on the examples in this repo.
- Create a new folder `src/pages` and add `Home.js` and `Callback.js` pages based on the examples in this repo.
- Modify `src/App.js` to include routing to the above pages.


## Start the OAuth2 Provider with pre-configured realm

- Inspect the realm: `authorization-code-flow/oauth2-provider/imports/realm.json`
- Client ID provided: `oauth2-playbook-auth-code-pkce` (Authorization Code + PKCE).
- Set this client in the React app config: `authorization-code-flow/client/src/auth/authConfig.js` (update `client_id` and redirect URI as needed).

```bash
cd ./authorization-code-flow/oauth2-provider
./start.sh
#Load - https://localhost:9443/
#Default admin user: `admin` / `admin`
```

## Setup the Resource Server

- Verify CORS settings in the resource server to allow requests from the client app (for example, `https://localhost:3000`).
- In a new terminal, run the resource server setup script to generate self-signed certs and start the server:

```shell
./authorization-code-flow/setup-resource-server.sh
```

## Run the Client Application

Start the dev server with HTTPS enabled (recommended for OIDC flows):

Note: Before you launch the client app, ensure that the OAuth2 provider and resource server are running, you must load following URL in browser to accept the self-signed certificate used by the React app:

- Keycloak [well-known](https://localhost:9443/realms/oauth2-playbook/.well-known/openid-configuration)
- Load Resource Server endpoint[https://localhost:8443/api/users](https://localhost:8443/api/users)

Then run

```bash
cd ./authorization-code-flow/client
#demo/demo123
export HTTPS=true && npm start
```

Notes:
- The React app includes a `Callback` page at `/callback` — ensure the provider redirect URI matches the app (for example `https://localhost:3000/callback`).
- Use the realm import file above to review clients, scopes and roles if you need to adjust the Keycloak configuration.