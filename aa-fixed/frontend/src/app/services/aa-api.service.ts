import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  ApiResponse,
  AccountResponse,
  ConsentResponse,
  CreateConsentRequest,
  FetchDataRequest,
  FetchSessionResponse
} from '../models/aa.models';

@Injectable({ providedIn: 'root' })
export class AaApiService {

  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ── Consent ───────────────────────────────────────────────────────────────

  createConsent(req: CreateConsentRequest): Observable<ConsentResponse> {
    return this.http.post<ApiResponse<ConsentResponse>>(`${this.base}/v1/consents`, req)
      .pipe(map(r => r.data));
  }

  getConsent(consentId: string): Observable<ConsentResponse> {
    return this.http.get<ApiResponse<ConsentResponse>>(`${this.base}/v1/consents/${consentId}`)
      .pipe(map(r => r.data));
  }

  getConsentsForCustomer(externalId: string): Observable<ConsentResponse[]> {
    return this.http.get<ApiResponse<ConsentResponse[]>>(`${this.base}/v1/consents/customer/${externalId}`)
      .pipe(map(r => r.data));
  }

  // ── FI Data ───────────────────────────────────────────────────────────────

  initiateDataFetch(req: FetchDataRequest): Observable<FetchSessionResponse> {
    return this.http.post<ApiResponse<FetchSessionResponse>>(`${this.base}/v1/fi/fetch`, req)
      .pipe(map(r => r.data));
  }

  getFetchStatus(sessionId: string): Observable<FetchSessionResponse> {
    return this.http.get<ApiResponse<FetchSessionResponse>>(`${this.base}/v1/fi/fetch/${sessionId}`)
      .pipe(map(r => r.data));
  }

  getAccounts(sessionId: string): Observable<AccountResponse[]> {
    return this.http.get<ApiResponse<AccountResponse[]>>(`${this.base}/v1/fi/fetch/${sessionId}/accounts`)
      .pipe(map(r => r.data));
  }
}
