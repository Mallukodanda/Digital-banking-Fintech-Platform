export type UserRole = 'ROLE_CUSTOMER' | 'ROLE_ADMIN' | 'ROLE_AUDITOR';

export type AccountType = 'CHECKING' | 'SAVINGS' | 'BUSINESS';
export type AccountStatus = 'ACTIVE' | 'FROZEN' | 'CLOSED';

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'P2P_TRANSFER' | 'FEE' | 'INTEREST';
export type TransactionStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'REVERSED';
export type LegType = 'DEBIT' | 'CREDIT';

export type BeneficiaryStatus = 'ACTIVE' | 'INACTIVE';
export type NotificationChannel = 'EMAIL' | 'SMS' | 'PUSH' | 'IN_APP';
export type NotificationStatus = 'UNREAD' | 'READ' | 'FAILED';
export type NotificationType = 'TRANSFER_SUCCESSFUL' | 'TRANSFER_FAILED' | 'FRAUD_ALERT' | 'ACCOUNT_FROZEN' | 'SYSTEM_ALERT';

export type FraudStatus = 'FLAGGED' | 'INVESTIGATING' | 'CONFIRMED_FRAUD' | 'DISMISSED';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type AuditAction = 
  | 'USER_REGISTERED'
  | 'AUTH_LOGIN_SUCCESS'
  | 'AUTH_LOGIN_FAILED'
  | 'ACCOUNT_CREATED'
  | 'ACCOUNT_FROZEN'
  | 'ACCOUNT_UNFROZEN'
  | 'TRANSFER_INITIATED'
  | 'TRANSFER_COMPLETED'
  | 'TRANSFER_FAILED'
  | 'BENEFICIARY_ADDED'
  | 'BENEFICIARY_REMOVED'
  | 'FRAUD_ALERT_GENERATED'
  | 'FRAUD_ALERT_RESOLVED';

export type AuditResult = 'SUCCESS' | 'FAILURE' | 'DENIED' | 'ERROR';

// API Response Envelope
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// Auth Types
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInMs: number;
  email: string;
  role: UserRole;
}

export interface UserProfileResponse {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: UserRole;
  createdAt: string;
}

// Account Types
export interface AccountResponse {
  id?: string;
  accountId?: string;
  accountNumber: string;
  accountType: AccountType;
  currency: string;
  status: AccountStatus;
  balance?: number;
  availableBalance?: number;
  cachedBalance?: number;
  holdBalance?: number;
  overdraftLimit?: number;
  createdAt: string;
  updatedAt?: string;
}

export interface AccountBalanceResponse {
  accountNumber: string;
  currency: string;
  availableBalance: number;
  status: AccountStatus;
  lastUpdated: string;
}

// Transfer Types
export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountNumber: string;
  amount: number;
  currency: string;
  description?: string;
}

export interface TransferReceiptResponse {
  transactionReference: string;
  idempotencyKey: string;
  sourceAccountNumber: string;
  destinationAccountNumber: string;
  amount: number;
  currency: string;
  status: TransactionStatus;
  sourceRemainingBalance: number;
  completedAt: string;
}

export interface TransactionHistoryResponse {
  legId: string;
  transactionReference: string;
  transactionType: TransactionType;
  legType: LegType;
  amount: number;
  currency: string;
  balanceAfter: number;
  description: string;
  timestamp: string;
}

// Beneficiary Types
export interface BeneficiaryResponse {
  id: string;
  beneficiaryName: string;
  destinationAccountNumber: string;
  nickname: string;
  status: BeneficiaryStatus;
  createdAt: string;
}

export interface CreateBeneficiaryRequest {
  destinationAccountNumber: string;
  nickname: string;
}

export interface VerifyBeneficiaryResponse {
  accountNumber: string;
  accountHolderName: string;
  accountType: AccountType;
  accountStatus: AccountStatus;
  valid: boolean;
}

// Notification Types
export interface NotificationResponse {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  channel: NotificationChannel;
  status: NotificationStatus;
  referenceId?: string;
  createdAt: string;
  readAt?: string;
}

// Fraud Types
export interface FraudAlertResponse {
  id: string;
  accountNumber: string;
  triggeredRule: string;
  riskScore: number;
  riskLevel: RiskLevel;
  status: FraudStatus;
  metadata?: string;
  reviewerNotes?: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface FraudReviewRequest {
  status: FraudStatus;
  reviewerNotes: string;
}

// Audit Types
export interface AuditLogResponse {
  id: string;
  actorId?: string;
  actorUsername?: string;
  actorRole: string;
  action: AuditAction;
  resourceType: string;
  resourceId: string;
  result: AuditResult;
  correlationId?: string;
  clientIp?: string;
  metadata?: string;
  createdAt: string;
}

export interface AuditLogFilterRequest {
  actorId?: string;
  actorUsername?: string;
  actorRole?: string;
  action?: AuditAction;
  resourceType?: string;
  resourceId?: string;
  result?: AuditResult;
  correlationId?: string;
  startDate?: string;
  endDate?: string;
}
