import { Button } from "@mui/material";
import { useContext, useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { setCredentials } from "./store/authSlice";
import { AuthContext } from "react-oauth2-code-pkce";
import { BrowserRouter as Router, Navigate, Route, Routes, useLocation } from "react-router";

function App() {

  const { token, tokenData, logIn, logOut, isAuthenticated } = useContext(AuthContext);
  const dispatch = useDispatch();
  const[authReady, setAuthReady] = useState(false);
  
  useEffect(() => {
    if (token) {
      dispatch(setCredentials({token, user: tokenData}));
      setAuthReady(true);
    }
  }, [token, tokenData, dispatch]);


  
  return (
    <Router>
      {!token ? (
      <Button variant = "contained" color="#dc004e"
              onClick={() => logIn()}>login</Button>
      ) : (
        <div>
          <pre>{JSON.stringify(tokenData, null, 2)}</pre>
        </div>
      )}
    </Router>
  )
}

export default App
