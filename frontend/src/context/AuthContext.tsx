import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { UserRole, UserProfileResponse, AuthResponse } from '../types';
import { authApi } from '../api/endpoints';

interface AuthContextType {
  user: UserProfileResponse | null;
  role: UserRole | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  }) => Promise<void>;
  logout: () => void;
  refreshUserProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfileResponse | null>(null);
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('access_token'));
  const [role, setRole] = useState<UserRole | null>(() => (localStorage.getItem('user_role') as UserRole) || null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const fetchProfile = useCallback(async () => {
    try {
      const res = await authApi.getProfile();
      setUser(res.data);
      setRole(res.data.role);
      localStorage.setItem('user_role', res.data.role);
    } catch (err: any) {
      // Only clear session on 401 Unauthorized (token invalid/expired)
      if (err?.response?.status === 401) {
        setUser(null);
        setToken(null);
        setRole(null);
        localStorage.clear();
      } else {
        // Fallback profile from stored role so session stays authenticated
        const storedRole = localStorage.getItem('user_role') as UserRole | null;
        if (storedRole) {
          setUser(prev => prev || {
            userId: '',
            email: 'user@bank.com',
            firstName: 'User',
            lastName: '',
            phoneNumber: '',
            role: storedRole,
            createdAt: new Date().toISOString(),
          });
        }
      }
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (token) {
      fetchProfile();
    } else {
      setIsLoading(false);
    }
  }, [token, fetchProfile]);

  const handleAuthSuccess = (data: AuthResponse) => {
    localStorage.setItem('access_token', data.accessToken);
    localStorage.setItem('refresh_token', data.refreshToken);
    localStorage.setItem('user_role', data.role);
    setToken(data.accessToken);
    setRole(data.role);
    setUser({
      userId: (data as any).userId || '',
      email: data.email,
      firstName: data.email.split('@')[0],
      lastName: '',
      phoneNumber: '',
      role: data.role,
      createdAt: new Date().toISOString(),
    });
    fetchProfile().catch(() => {});
  };

  const login = async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    handleAuthSuccess(res.data);
  };

  const register = async (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  }) => {
    const res = await authApi.register(payload);
    handleAuthSuccess(res.data);
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
    setToken(null);
    setRole(null);
    window.location.href = '/login';
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        role,
        token,
        isAuthenticated: !!token,
        isLoading,
        login,
        register,
        logout,
        refreshUserProfile: fetchProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
