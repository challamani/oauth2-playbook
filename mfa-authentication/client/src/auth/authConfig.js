import { UserManager, WebStorageStateStore } from "oidc-client-ts";

export const userManager = new UserManager({
  authority: "https://localhost:9443/realms/oauth2-playbook",
  client_id: "oauth2-playbook-mfa",
  redirect_uri: `${window.location.origin}/callback`,
  response_type: "code",
  scope: "openid profile",
  post_logout_redirect_uri: window.location.origin,
  automaticSilentRenew: true,
  filterProtocolClaims: false, // Keep ALL protocol claims (acr, amr, auth_time, etc.)
  loadUserInfo: true,
  userStore: new WebStorageStateStore({ store: window.sessionStorage }),
});

