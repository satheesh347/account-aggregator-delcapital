import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { AaApiService } from '../../services/aa-api.service';
import { ConsentResponse, FetchSessionResponse } from '../../models/aa.models';

@Component({
  selector: 'app-fetch-data',
  templateUrl: './fetch-data.component.html',
  styleUrls: ['./fetch-data.component.css']
})
export class FetchDataComponent implements OnInit, OnDestroy {

  form!: FormGroup;
  consent: ConsentResponse | null = null;
  session: FetchSessionResponse | null = null;
  loadingConsent = true;
  submitting = false;
  error = '';
  private pollSub?: Subscription;

  fiTypeOptions = [
    { value: 'DEPOSIT', label: 'Bank Deposits' },
    { value: 'MUTUAL_FUNDS', label: 'Mutual Funds' },
    { value: 'INSURANCE_POLICIES', label: 'Insurance' },
    { value: 'NPS', label: 'NPS' },
    { value: 'EQUITIES', label: 'Equities' },
  ];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private api: AaApiService
  ) {}

  ngOnInit(): void {
    const consentId = this.route.snapshot.paramMap.get('consentId')!;

    const today = new Date();
    const yearAgo = new Date(today);
    yearAgo.setFullYear(yearAgo.getFullYear() - 1);

    this.form = this.fb.group({
      fiTypes:       [['DEPOSIT']],
      dateRangeFrom: [yearAgo.toISOString().substring(0, 10), Validators.required],
      dateRangeTo:   [today.toISOString().substring(0, 10), Validators.required],
    });

    this.api.getConsent(consentId).subscribe({
      next: c => { this.consent = c; this.loadingConsent = false; },
      error: () => { this.error = 'Could not load consent.'; this.loadingConsent = false; }
    });
  }

  ngOnDestroy(): void { this.pollSub?.unsubscribe(); }

  onFiTypeChange(value: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const current: string[] = this.form.get('fiTypes')!.value ?? [];
    this.form.get('fiTypes')!.setValue(
      checked ? [...current, value] : current.filter(v => v !== value)
    );
  }

  isFiTypeSelected(value: string): boolean {
    return (this.form.get('fiTypes')!.value ?? []).includes(value);
  }

  submit(): void {
    if (!this.consent || this.form.invalid) return;
    this.submitting = true;
    this.error = '';

    const v = this.form.value;
    this.api.initiateDataFetch({
      consentId: this.consent.id,
      fiTypes: v.fiTypes,
      dateRangeFrom: v.dateRangeFrom,
      dateRangeTo: v.dateRangeTo,
    }).subscribe({
      next: session => {
        this.session = session;
        this.submitting = false;
        this.startPolling(session.id);
      },
      error: err => {
        this.error = err?.error?.message || 'Failed to initiate data fetch.';
        this.submitting = false;
      }
    });
  }

  private startPolling(sessionId: string): void {
    this.pollSub = interval(4000).pipe(
      switchMap(() => this.api.getFetchStatus(sessionId)),
      takeWhile(s => s.status === 'INITIATED' || s.status === 'PROCESSING', true)
    ).subscribe({
      next: s => {
        this.session = s;
        if (s.status === 'COMPLETED') {
          this.router.navigate(['/accounts', s.id]);
        }
      }
    });
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      INITIATED: 'badge-warning', PROCESSING: 'badge-info',
      COMPLETED: 'badge-success', FAILED: 'badge-danger', PARTIAL: 'badge-warning',
    };
    return map[status] ?? 'badge-secondary';
  }
}
