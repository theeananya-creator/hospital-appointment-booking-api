import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { authApi } from "../api";
import { restoreSession, setAccessToken, setAuthenticationFailureHandler } from "../api/client";
import { userFromToken } from "../utils/jwt";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [accessToken, setCurrentAccessToken] = useState(null);
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const acceptTokens = useCallback((tokens) => {
    setCurrentAccessToken(tokens.accessToken);
    setAccessToken(tokens.accessToken);
    // Keep only the rotating refresh token across reloads; access tokens stay in memory.
    window.localStorage.setItem("refreshToken", tokens.refreshToken);
    setUser(userFromToken(tokens.accessToken));
  }, []);

  const logout = useCallback(() => {
    setCurrentAccessToken(null);
    setAccessToken(null);
    setUser(null);
    window.localStorage.removeItem("refreshToken");
  }, []);

  useEffect(() => {
    setAuthenticationFailureHandler(() => {
      logout();
      if (!["/login", "/register"].includes(window.location.pathname)) {
        window.location.assign("/login");
      }
    });
    restoreSession()
      .then((token) => {
        if (token) {
          setCurrentAccessToken(token);
          setAccessToken(token);
          setUser(userFromToken(token));
        }
      })
      .catch(() => {
        logout();
      })
      .finally(() => setLoading(false));
  }, [logout]);

  const login = useCallback(async (credentials) => {
    const { data } = await authApi.login(credentials);
    acceptTokens(data);
    return userFromToken(data.accessToken);
  }, [acceptTokens]);

  const register = useCallback(async (details) => {
    const { data } = await authApi.register(details);
    acceptTokens(data);
    return userFromToken(data.accessToken);
  }, [acceptTokens]);

  const value = useMemo(() => ({
    accessToken,
    user,
    loading,
    login,
    register,
    logout,
  }), [accessToken, user, loading, login, register, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used inside AuthProvider");
  return context;
}
