import logo from '../logo.svg';
import { useEffect, useState } from "react";
import { getUser, logout } from "../auth/authService";

export default function Home() {
  const [user, setUser] = useState(null);

  useEffect(() => {
    getUser().then(u => setUser(u));
  }, []);

  async function loadUsers() {
    const response = await fetch("https://localhost:8443/api/users", {
      headers: {
        Authorization: `Bearer ${user.access_token}`,
        "Content-Type": "application/json"
      },
      credentials: "include"
    });

    const data = await response.json();
    alert(JSON.stringify(data));
  }

  if (!user) return <div>Loading...</div>;

  return (
    <div>
      <h2>Welcome {user.profile?.preferred_username}</h2>
        <img src={logo} className="App-logo" alt="logo" />
      <button onClick={loadUsers}>Load Users</button>
      <button onClick={logout}>Logout</button>
    </div>
  );
}