import { useEffect } from "react";
import { userManager } from "../auth/authConfig";

export default function Callback() {
  useEffect(() => {
    userManager
      .signinRedirectCallback()
      .then(() => {
        window.location.replace("/");
      })
      .catch(console.error);
  }, []);

  return <div>Signing in...</div>;
}