import './App.css';

import { BrowserRouter, Routes, Route } from "react-router-dom";
import { useEffect, useState } from "react";
import { getUser, login } from "./auth/authService";
import Home from "./pages/Home";
import Callback from "./pages/Callback";

export default function App() {
  const [checkedAuth, setCheckedAuth] = useState(false);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    getUser().then(user => {
      if (!user || user.expired) {
        login(); // AUTO REDIRECT TO LOGIN
      } else {
        setIsAuthenticated(true);
      }
      setCheckedAuth(true);
    });
  }, []);

  if (!checkedAuth) return <div>Checking authentication...</div>;

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/callback" element={<Callback />} />
        <Route
          path="/"
          element={isAuthenticated ? <Home /> : null}
        />
      </Routes>
    </BrowserRouter>
  );
}

