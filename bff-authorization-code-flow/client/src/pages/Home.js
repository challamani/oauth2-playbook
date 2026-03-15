import logo from '../logo.svg';
import { useState } from "react";
import { BFF_BASE_URL, API_BASE_PATH } from "../auth/authConfig";

export default function Home() {
  const [usersResponse, setUsersResponse] = useState("");

  async function loadUsers() {
    const targetUrl = `${BFF_BASE_URL}${API_BASE_PATH}/users`;
    
    console.log("--- START LOAD USERS ---");
    console.log("1. Target URL:", targetUrl);

    try {
      const response = await fetch(targetUrl, {
        headers: {
          "Content-Type": "application/json"
        },
        credentials: "include"
      });

      console.log("2. Response Received Status:", response.status);
      console.log("3. Was Redirected internally?:", response.redirected);
      console.log("4. Final URL after redirects (if any):", response.url);

      // Check for OIDC Redirect hijacking the fetch
      if (response.redirected && response.url.includes("protocol/openid-connect/auth")) {
        console.warn("DETECTED: Browser tried to follow OIDC redirect inside fetch. This will likely trigger a CORS error.");
      }

      if (response.status === 401 || response.status === 403) {
        console.log("5. Auth Challenge detected. Redirecting entire tab to start OIDC flow...");
        // Delaying slightly so you can actually read the console before the page changes
        setTimeout(() => {
          window.location.href = targetUrl;
        }, 500);
        return;
      }

      const responseText = await response.text();
      console.log("6. Response Body (Preview):", responseText.substring(0, 100));

      if (!response.ok) {
        console.error("7. Response Not OK. Status Text:", response.statusText);
        setUsersResponse(responseText || "Unable to load users. Please try again.");
        return;
      }

      console.log("8. SUCCESS: Data loaded.");
      setUsersResponse(responseText);

    } catch (error) {
      console.error("--- CATCH BLOCK TRIGGERED ---");
      console.error("Error Name:", error.name);
      console.error("Error Message:", error.message);
      
      if (error.message.includes("Failed to fetch")) {
        console.error("POSSIBLE CAUSES: \n- CORS policy violation (check Keycloak Web Origins)\n- SSL Certificate not trusted (open BFF URL in a new tab)\n- BFF Server is down.");
      }
      window.location.href = `${BFF_BASE_URL}${API_BASE_PATH}/users`;
      setUsersResponse("Network/CORS error occurred. Check browser console.");
    }
  }

  return (
    <div>
      <img src={logo} className="App-logo" alt="logo" />
      <button onClick={loadUsers}>Load Users</button>
      <hr />
      <h3>Server Response:</h3>
      {usersResponse && (
        <pre style={{ backgroundColor: '#eee', padding: '10px', textAlign: 'left' }}>
          {usersResponse}
        </pre>
      )}
    </div>
  );
}