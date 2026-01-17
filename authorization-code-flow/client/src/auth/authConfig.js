import { UserManager, WebStorageStateStore } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: "https://localhost:9443/realms/oauth2-playbook",
  client_id: "oauth2-playbook-auth-code-pkce",
  redirect_uri: `${window.location.origin}/callback`,
  response_type: "code",
  scope: 'openid profile users:read',
  post_logout_redirect_uri: window.location.origin,
  automaticSilentRenew: true,
  filterProtocolClaims: true,
  loadUserInfo: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage })
});
