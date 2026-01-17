# Authorization Code Flow with PKCE Example

## Start the OAuth2 Provider with pre-configured realm

- Understand the realm: `authorization-code-flow/oauth2-provider/imports/realm.json
- There is a `oauth2-playbook-auth-code-pkce` client configured for Authorization Code flow with PKCE.
- Remember this client, you have to set this client in the React app configuration.


```bash
cd ./authorization-code-flow/oauth2-provider
./start.sh
```


## Create React App Skeleton

```bash
cd ./authorization-code-flow

npx create-react-app client
npm install react-router-dom
npm install oidc-client-ts
---
```

## Run the Client Application

```bash
cd ./authorization-code-flow/client
export HTTPS=true && npm start
```