import { useEffect } from "react";

export default function Callback() {
  useEffect(() => {
    window.location.replace("/");
  }, []);

  return <div>Completing sign-in...</div>;
}