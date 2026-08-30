import { Box, Button, Typography } from "@mui/material";
import { useContext, useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { setCredentials } from "./store/authSlice";
import { AuthContext } from "react-oauth2-code-pkce";
import { BrowserRouter as Router, Navigate, Route, Routes, useLocation } from "react-router";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail";
import ActivityForm from "./components/ActivityForm";


const ActivitiesPage = () => {
  return (
    <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
      <ActivityForm onActivityAdded={() => window.location.reload()} />
      <ActivityList />
    </Box>
  );
}

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
      <Box
      sx={{
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
      }}>
        <Typography variant="h4" gutterBottom>
          Welcome to the Fitness Tracker App Bitch 
        </Typography>
        <Typography variant="body1" gutterBottom>
          Please log in to access your activities and AI recommendations dumbass.
        </Typography>
        <Button variant = "contained" color="primary" size="large" onClick={() => logIn()}>
          LOGIN
        </Button>
      </Box>
      ) : (
        <Box component="section" sx={{ p: 2, border: '1px dashed grey' }}>
          <Button variant = "contained" color="secondary" onClick={() => logOut()}>
            LOGOUT
          </Button>
          <Routes>
            <Route path="/activities" element={<ActivitiesPage />} />
            <Route path="/activities/:id" element={<ActivityDetail />} />
            
            <Route path="/" element={token ? <Navigate to="/activities" replace/> : <div>Nothing to see here! Try logging in</div>} />
        </Routes>
    </Box>
      )}
    </Router>
  )
}

export default App
