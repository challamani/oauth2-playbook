import { useEffect, useState } from "react";
import { getUser, logout } from "../auth/authService";

/**
 * Decode a JWT (ID Token) and return the payload as a plain object.
 * This is a simple base64url decode — it does NOT verify the signature
 * (the OIDC library already did that during the token exchange).
 */
function decodeJwt(token) {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

/** Format a UNIX epoch (seconds) into a readable date/time string. */
function formatEpoch(epoch) {
  if (!epoch) return "—";
  return new Date(epoch * 1000).toLocaleString();
}

export default function Home() {
  const [user, setUser] = useState(null);
  const [idTokenClaims, setIdTokenClaims] = useState(null);

  useEffect(() => {
    getUser().then((u) => {
      if (u) {
        setUser(u);
        if (u.id_token) {
          setIdTokenClaims(decodeJwt(u.id_token));
        }
      }
    });
  }, []);

  if (!user) return <div>Loading...</div>;

  // Highlight the authentication-context claims that prove MFA
  const mfaClaims = idTokenClaims
    ? {
        acr: idTokenClaims.acr,
        amr: idTokenClaims.amr,
        auth_time: idTokenClaims.auth_time,
        auth_time_readable: formatEpoch(idTokenClaims.auth_time),
      }
    : {};

  return (
    <div style={{ fontFamily: "monospace", padding: "2rem", maxWidth: 900, margin: "0 auto" }}>
      <h1>🔐 MFA Authentication Demo</h1>
      <p>
        Welcome, <strong>{user.profile?.preferred_username}</strong> —
        you authenticated with <em>Password + OTP (TOTP)</em>.
      </p>

      {/* ─── MFA evidence ─── */}
      <section style={sectionStyle}>
        <h2>Authentication Context (MFA Evidence)</h2>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thStyle}>Claim</th>
              <th style={thStyle}>Value</th>
              <th style={thStyle}>Meaning</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style={tdStyle}><code>acr</code></td>
              <td style={tdStyle}><code>{JSON.stringify(mfaClaims.acr) ?? "—"}</code></td>
              <td style={tdStyle}>Authentication Context Class Reference — level of assurance achieved during login</td>
            </tr>
            <tr>
              <td style={tdStyle}><code>amr</code></td>
              <td style={tdStyle}><code>{JSON.stringify(mfaClaims.amr) ?? "not present"}</code></td>
              <td style={tdStyle}>Authentication Methods References — list of methods used (e.g. pwd, otp)</td>
            </tr>
            <tr>
              <td style={tdStyle}><code>auth_time</code></td>
              <td style={tdStyle}><code>{mfaClaims.auth_time}</code></td>
              <td style={tdStyle}>When the user actively authenticated: {mfaClaims.auth_time_readable}</td>
            </tr>
          </tbody>
        </table>
        <p style={{ fontSize: "0.85rem", color: "#666", marginTop: "0.5rem" }}>
          <strong>Note:</strong> Keycloak populates <code>acr</code> by default.
          The <code>amr</code> claim may not appear unless a custom protocol mapper is added (see README).
          The presence of <code>auth_time</code> with a recent timestamp confirms the user actively logged in (not just re-used a session cookie).
        </p>
      </section>

      {/* ─── User profile ─── */}
      <section style={sectionStyle}>
        <h2>User Profile Claims</h2>
        <pre style={preStyle}>
          {JSON.stringify(user.profile, null, 2)}
        </pre>
      </section>

      {/* ─── Full decoded ID token ─── */}
      <section style={sectionStyle}>
        <h2>Full Decoded ID Token</h2>
        <pre style={preStyle}>
          {idTokenClaims
            ? JSON.stringify(idTokenClaims, null, 2)
            : "ID Token not available"}
        </pre>
      </section>

      {/* ─── Raw ID Token (JWT) ─── */}
      <section style={sectionStyle}>
        <h2>Raw ID Token (JWT)</h2>
        <textarea
          readOnly
          value={user.id_token || ""}
          rows={6}
          style={{ width: "100%", fontFamily: "monospace", fontSize: "0.8rem" }}
        />
        <p style={{ fontSize: "0.85rem", color: "#666" }}>
          Copy this JWT and paste it into <a href="https://jwt.io" target="_blank" rel="noreferrer">jwt.io</a> to inspect it independently.
        </p>
      </section>

      <button onClick={logout} style={buttonStyle}>
        Logout
      </button>
    </div>
  );
}

/* ─── inline styles ─── */
const sectionStyle = {
  marginTop: "2rem",
  padding: "1rem",
  border: "1px solid #ddd",
  borderRadius: 8,
  background: "#f9f9f9",
};
const tableStyle = { width: "100%", borderCollapse: "collapse", marginTop: "0.5rem" };
const thStyle = { textAlign: "left", borderBottom: "2px solid #ccc", padding: "6px 8px" };
const tdStyle = { borderBottom: "1px solid #eee", padding: "6px 8px", verticalAlign: "top" };
const preStyle = {
  background: "#272822",
  color: "#f8f8f2",
  padding: "1rem",
  borderRadius: 6,
  overflow: "auto",
  maxHeight: 400,
  fontSize: "0.85rem",
};
const buttonStyle = {
  marginTop: "2rem",
  padding: "10px 28px",
  fontSize: "1rem",
  cursor: "pointer",
  background: "#e74c3c",
  color: "#fff",
  border: "none",
  borderRadius: 6,
};

