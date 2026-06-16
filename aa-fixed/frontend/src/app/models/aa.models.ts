export interface CreateConsentRequest {
  customerExternalId: string;
  customerName: string;
  mobile: string;
  email?: string;
  purposeCode: string;
  purposeText?: string;
  fiTypes: string[];
  consentStart: string;
  consentExpiry: string;
  fetchType: 'ONETIME' | 'PERIODIC';
  redirectUrl?: string;
  callbackUrl?: string;
  idempotencyKey?: string;
}

export interface ConsentResponse {
  id: string;
  digioConsentId: string;
  status: ConsentStatus;
  purposeCode: string;
  purposeText: string;
  fetchType: string;
  consentStart: string;
  consentExpiry: string;
  fiTypes: string[];
  consentUrl: string;
  redirectUrl: string;
  createdAt: string;
  updatedAt: string;
}

export type ConsentStatus = 'PENDING' | 'ACTIVE' | 'PAUSED' | 'REVOKED' | 'EXPIRED' | 'REJECTED';

export interface FetchDataRequest {
  consentId: string;
  fiTypes?: string[];
  dateRangeFrom: string;
  dateRangeTo: string;
  idempotencyKey?: string;
}

export interface FetchSessionResponse {
  id: string;
  consentId: string;
  digioSessionId: string;
  status: 'INITIATED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'PARTIAL';
  fiTypes: string[];
  dateRangeFrom: string;
  dateRangeTo: string;
  errorCode?: string;
  errorMessage?: string;
  fetchedAt?: string;
  createdAt: string;
}

export interface AccountResponse {
  id: string;
  fipId: string;
  accountType: string;
  fiType: string;
  maskedAccNo: string;
  currency: string;
  holderName: string;
  ifscCode: string;
  branch: string;
  balance: number;
  asOfDate: string;
  transactions: TransactionResponse[];
}

export interface TransactionResponse {
  id: string;
  txnId: string;
  txnDate: string;
  amount: number;
  txnType: 'CREDIT' | 'DEBIT';
  mode: string;
  narration: string;
  reference: string;
  balance: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errorCode?: string;
  timestamp: string;
}
