import { useEffect, useRef } from "react";
import { userManager } from "../auth/authConfig";

export default function Callback() {
  const processed = useRef(false);

  useEffect(() => {
    // Guard against React StrictMode double-invocation in dev mode.
    // signinRedirectCallback() consumes the authorization code — calling it
    // twice causes Keycloak to reject the second attempt ("Code already used").
    if (processed.current) return;
    processed.current = true;

    userManager
      .signinRedirectCallback()
      .then(() => {
        window.location.replace("/");
      })
      .catch((err) => {
        console.error("Callback error:", err);
        // If the code was already consumed or is invalid, redirect to
        // the home page so App.js can check the session / re-login.
        window.location.replace("/");
      });
  }, []);

  return <div>Signing in...</div>;
}

