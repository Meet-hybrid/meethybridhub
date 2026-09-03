"use client";

import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from "react";
import { storefrontApi } from "@/lib/api";

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  isLoading: true,
  login: async () => {},
  register: async () => {},
  logout: async () => {},
});

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    setIsAuthenticated(storefrontApi.isAuthenticated());
    setIsLoading(false);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    await storefrontApi.login(email, password);
    setIsAuthenticated(true);
  }, []);

  const register = useCallback(async (email: string, password: string, fullName: string) => {
    await storefrontApi.register(email, password, fullName);
    setIsAuthenticated(true);
  }, []);

  const logout = useCallback(async () => {
    await storefrontApi.logout();
    setIsAuthenticated(false);
  }, []);

  return (
    <AuthContext.Provider value={{ isAuthenticated, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
