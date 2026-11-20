import { createContext, ReactNode, useContext, useEffect, useState } from "react";
import * as SecureStore from "expo-secure-store";
import { api, setAuthToken } from "../api";

type User = { userId: number; email: string; role: string };

type AuthState = {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, firstName: string, lastName: string) => Promise<void>;
  logout: () => Promise<void>;
};

const KEY = "campusvoice_auth";
const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // La pornire: dacă există o sesiune salvată, o refolosim
  useEffect(() => {
    (async () => {
      try {
        const saved = await SecureStore.getItemAsync(KEY);
        if (saved) {
          const { token, user } = JSON.parse(saved);
          setAuthToken(token);
          setUser(user);
        }
      } catch {
        // ignorăm: pornim delogați
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const applyAuth = async (data: any) => {
    const u: User = { userId: data.userId, email: data.email, role: data.role };
    setAuthToken(data.token);
    setUser(u);
    await SecureStore.setItemAsync(KEY, JSON.stringify({ token: data.token, user: u }));
  };

  const login = async (email: string, password: string) => {
    const res = await api.post("/api/auth/login", { email: email.trim().toLowerCase(), password });
    await applyAuth(res.data);
  };

  const register = async (email: string, password: string, firstName: string, lastName: string) => {
    await api.post("/api/auth/register", {
      email: email.trim().toLowerCase(),
      password,
      firstName: firstName.trim(),
      lastName: lastName.trim(),
    });
    await login(email, password); // după înregistrare te logăm direct
  };

  const logout = async () => {
    setAuthToken(null);
    setUser(null);
    await SecureStore.deleteItemAsync(KEY);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth trebuie folosit în AuthProvider");
  return ctx;
};