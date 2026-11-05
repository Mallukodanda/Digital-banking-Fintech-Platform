import { apiClient } from './client';
import {
  ApiResponse,
  PagedResponse,
  AuthResponse,
  UserProfileResponse,
  AccountResponse,
  AccountBalanceResponse,
  TransferRequest,
  TransferReceiptResponse,
  TransactionHistoryResponse,
  BeneficiaryResponse,
  CreateBeneficiaryRequest,
  VerifyBeneficiaryResponse,
  NotificationResponse,
  FraudAlertResponse,
  FraudReviewRequest,
  FraudStatus,
  AuditLogResponse,
  AuditLogFilterRequest,
  AccountStatus,
} from '../types';

export const authApi = {
  login: async (email: string, password: string):Promise<ApiResponse<AuthResponse>> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', { email, password });
    return res.data;
  },
  register: async (payload: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  }): Promise<ApiResponse<AuthResponse>> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/register', {
      email: payload.email,
      password: payload.password,
      firstName: payload.firstName,
      lastName: payload.lastName,
      phone: payload.phoneNumber,
      phoneNumber: payload.phoneNumber,
    });
    return res.data;
  },
  getProfile: async (): Promise<ApiResponse<UserProfileResponse>> => {
    const res = await apiClient.get<ApiResponse<UserProfileResponse>>('/auth/me');
    return res.data;
  },
};

export const accountApi = {
  getMyAccounts: async (): Promise<ApiResponse<AccountResponse[]>> => {
    const res = await apiClient.get<ApiResponse<AccountResponse[]>>('/accounts');
    return res.data;
  },
  getAccountBalance: async (accountId: string): Promise<ApiResponse<AccountBalanceResponse>> => {
    const res = await apiClient.get<ApiResponse<AccountBalanceResponse>>(`/accounts/${accountId}/balance`);
    return res.data;
  },
  createAccount: async (payload: { accountType: string; currency: string }): Promise<ApiResponse<AccountResponse>> => {
    const res = await apiClient.post<ApiResponse<AccountResponse>>('/accounts', payload);
    return res.data;
  },
};

export const transferApi = {
  executeTransfer: async (
    payload: TransferRequest,
    idempotencyKey: string
  ): Promise<ApiResponse<TransferReceiptResponse>> => {
    const res = await apiClient.post<ApiResponse<TransferReceiptResponse>>('/transfers', payload, {
      headers: {
        'Idempotency-Key': idempotencyKey,
      },
    });
    return res.data;
  },
  getTransactionHistory: async (
    accountId: string,
    page = 0,
    size = 10
  ): Promise<ApiResponse<PagedResponse<TransactionHistoryResponse>>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<TransactionHistoryResponse>>>(
      `/transfers/accounts/${accountId}/history`,
      { params: { page, size } }
    );
    return res.data;
  },
};

export const beneficiaryApi = {
  getBeneficiaries: async (): Promise<ApiResponse<BeneficiaryResponse[]>> => {
    const res = await apiClient.get<ApiResponse<BeneficiaryResponse[]>>('/beneficiaries');
    return res.data;
  },
  verifyBeneficiary: async (accountNumber: string): Promise<ApiResponse<VerifyBeneficiaryResponse>> => {
    const res = await apiClient.post<ApiResponse<VerifyBeneficiaryResponse>>('/beneficiaries/verify', {
      accountNumber,
    });
    return res.data;
  },
  addBeneficiary: async (payload: CreateBeneficiaryRequest): Promise<ApiResponse<BeneficiaryResponse>> => {
    const res = await apiClient.post<ApiResponse<BeneficiaryResponse>>('/beneficiaries', payload);
    return res.data;
  },
  deleteBeneficiary: async (beneficiaryId: string): Promise<ApiResponse<void>> => {
    const res = await apiClient.delete<ApiResponse<void>>(`/beneficiaries/${beneficiaryId}`);
    return res.data;
  },
};

export const notificationApi = {
  getMyNotifications: async (page = 0, size = 15): Promise<ApiResponse<PagedResponse<NotificationResponse>>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<NotificationResponse>>>('/notifications', {
      params: { page, size },
    });
    return res.data;
  },
  getUnreadCount: async (): Promise<ApiResponse<{ unreadCount: number }>> => {
    const res = await apiClient.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count');
    return res.data;
  },
  markAsRead: async (notificationId: string): Promise<ApiResponse<NotificationResponse>> => {
    const res = await apiClient.patch<ApiResponse<NotificationResponse>>(`/notifications/${notificationId}/read`);
    return res.data;
  },
  markAllAsRead: async (): Promise<ApiResponse<{ updatedCount: number }>> => {
    const res = await apiClient.patch<ApiResponse<{ updatedCount: number }>>('/notifications/read-all');
    return res.data;
  },
};

export const adminApi = {
  // Fraud
  getFraudAlerts: async (status?: FraudStatus, page = 0, size = 10): Promise<ApiResponse<PagedResponse<FraudAlertResponse>>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<FraudAlertResponse>>>('/admin/fraud/alerts', {
      params: { status, page, size },
    });
    return res.data;
  },
  reviewFraudAlert: async (alertId: string, payload: FraudReviewRequest): Promise<ApiResponse<FraudAlertResponse>> => {
    const res = await apiClient.patch<ApiResponse<FraudAlertResponse>>(`/admin/fraud/alerts/${alertId}/review`, payload);
    return res.data;
  },
  // Audit Logs
  getAuditLogs: async (filter: AuditLogFilterRequest, page = 0, size = 15): Promise<ApiResponse<PagedResponse<AuditLogResponse>>> => {
    const res = await apiClient.get<ApiResponse<PagedResponse<AuditLogResponse>>>('/admin/audit-logs', {
      params: { ...filter, page, size },
    });
    return res.data;
  },
  // Account Status Update
  updateAccountStatus: async (accountId: string, status: AccountStatus, reason: string): Promise<ApiResponse<AccountResponse>> => {
    const res = await apiClient.patch<ApiResponse<AccountResponse>>(`/admin/accounts/${accountId}/status`, {
      status,
      reason,
    });
    return res.data;
  },
};
