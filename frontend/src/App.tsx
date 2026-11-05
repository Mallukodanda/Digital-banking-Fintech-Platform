import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from './context/AuthContext';
import { NotificationProvider } from './context/NotificationContext';

// Common & Layouts
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { DashboardLayout } from './components/layout/DashboardLayout';
import { AdminLayout } from './components/layout/AdminLayout';

// Public Pages
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';

// Customer Pages
import { DashboardPage } from './pages/customer/DashboardPage';
import { AccountsPage } from './pages/customer/AccountsPage';
import { TransferMoneyPage } from './pages/customer/TransferMoneyPage';
import { BeneficiariesPage } from './pages/customer/BeneficiariesPage';
import { TransactionsPage } from './pages/customer/TransactionsPage';
import { StatementsPage } from './pages/customer/StatementsPage';
import { NotificationsPage } from './pages/customer/NotificationsPage';
import { ProfilePage } from './pages/customer/ProfilePage';

// Admin Pages
import { AdminDashboardPage } from './pages/admin/AdminDashboardPage';
import { FraudAlertsPage } from './pages/admin/FraudAlertsPage';
import { AuditLogsPage } from './pages/admin/AuditLogsPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
      staleTime: 30000,
    },
  },
});

export const App: React.FC = () => {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <NotificationProvider>
          <BrowserRouter>
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<LandingPage />} />
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Protected Customer Routes */}
              <Route
                element={
                  <ProtectedRoute allowedRoles={['ROLE_CUSTOMER', 'ROLE_ADMIN']}>
                    <DashboardLayout />
                  </ProtectedRoute>
                }
              >
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/accounts" element={<AccountsPage />} />
                <Route path="/transfer" element={<TransferMoneyPage />} />
                <Route path="/beneficiaries" element={<BeneficiariesPage />} />
                <Route path="/transactions" element={<TransactionsPage />} />
                <Route path="/statements" element={<StatementsPage />} />
                <Route path="/notifications" element={<NotificationsPage />} />
                <Route path="/profile" element={<ProfilePage />} />
              </Route>

              {/* Protected Admin Routes */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute allowedRoles={['ROLE_ADMIN', 'ROLE_AUDITOR']}>
                    <AdminLayout />
                  </ProtectedRoute>
                }
              >
                <Route index element={<AdminDashboardPage />} />
                <Route path="fraud" element={<FraudAlertsPage />} />
                <Route path="audit" element={<AuditLogsPage />} />
              </Route>

              {/* Catch-all Fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </BrowserRouter>
        </NotificationProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
};
