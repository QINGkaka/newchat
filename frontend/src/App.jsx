import NavBar from "./components/NavBar.jsx";
import {Routes, Route, Navigate} from 'react-router';
import HomePage from "./pages/HomePage.jsx";
import SignUpPage from "./pages/SignUpPage.jsx";
import LoginPage from "./pages/LoginPage.jsx";
import SettingsPage from "./pages/SettingsPage.jsx";
import ProfilePage from "./pages/ProfilePage.jsx";
import { useAuthStore } from "./store/useAuthStore.js";
import { useEffect } from "react";
import {Loader} from 'lucide-react';
import {Toaster} from "react-hot-toast";
import './lib/i18n.js';

function App() {
  const {authUser, checkAuth, isCheckingAuth, onlineUsers} =  useAuthStore();
  useEffect(()=>{
    checkAuth();
  },[]); // 只在组件挂载时执行一次 checkAuth 函数，以检查用户是否已登录。
  console.log(onlineUsers);

  if (isCheckingAuth && !authUser) {
    return <div className="flex justify-center items-center h-screen"><Loader className="animate-spin"/>Loading...</div>;
  }
  return (
    <div>
     <NavBar />
     <Routes>
        {/* <Route path="/" element={authUser? <HomePage /> : <Navigate to="/login" />} /> */}
        <Route path="/" element={<HomePage /> } />
        <Route path="/signup" element={!authUser? <SignUpPage />:  <Navigate to="/" />} />
        <Route path="/login" element={!authUser? <LoginPage />: <Navigate to="/" />} />
        <Route path="/settings" element={<SettingsPage />} />
        <Route path="/profile" element={authUser? <ProfilePage />: <Navigate to="/login" />} />
      </Routes>
    <Toaster />
    </div>
  )
}

export default App
