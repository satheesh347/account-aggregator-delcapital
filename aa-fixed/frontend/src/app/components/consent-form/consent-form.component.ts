import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AaApiService } from '../../services/aa-api.service';
import { CreateConsentRequest } from '../../models/aa.models';

@Component({
  selector: 'app-consent-form',
  templateUrl: './consent-form.component.html',
  styleUrls: ['./consent-form.component.css']
})
export class ConsentFormComponent implements OnInit {

  form!: FormGroup;
  loading = false;
  error = '';

  fiTypeOptions = [
    { value: 'DEPOSIT', label: 'Bank Deposits' },
    { value: 'MUTUAL_FUNDS', label: 'Mutual Funds' },
    { value: 'INSURANCE_POLICIES', label: 'Insurance' },
    { value: 'NPS', label: 'NPS' },
    { value: 'EQUITIES', label: 'Equities' },
  ];

  purposeOptions = [
    { value: '101', label: 'Wealth Management' },
    { value: '102', label: 'Customer Spending Patterns' },
    { value: '103', label: 'Financial Underwriting' },
    { value: '104', label: 'Credit Decisioning' },
  ];

  constructor(
    private fb: FormBuilder,
    private apiService: AaApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const now = new Date();
    const expiry = new Date(now);
    expiry.setFullYear(expiry.getFullYear() + 1);

    this.form = this.fb.group({
      customerExternalId: ['', [Validators.required, Validators.minLength(3)]],
      customerName:       ['', [Validators.required]],
      mobile:             ['', [Validators.required, Validators.pattern(/^[6-9]\d{9}$/)]],
      email:              ['', [Validators.email]],
      purposeCode:        ['104', [Validators.required]],
      purposeText:        ['Financial data access for credit decisioning'],
      fiTypes:            [['DEPOSIT'], [Validators.required]],
      consentStart:       [now.toISOString().substring(0, 16), [Validators.required]],
      consentExpiry:      [expiry.toISOString().substring(0, 16), [Validators.required]],
      fetchType:          ['ONETIME'],
      redirectUrl:        [''],
    });
  }

  onFiTypeChange(value: string, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const current: string[] = this.form.get('fiTypes')!.value ?? [];
    if (checked) {
      this.form.get('fiTypes')!.setValue([...current, value]);
    } else {
      this.form.get('fiTypes')!.setValue(current.filter(v => v !== value));
    }
  }

  isFiTypeSelected(value: string): boolean {
    return (this.form.get('fiTypes')!.value ?? []).includes(value);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const v = this.form.value;
    const req: CreateConsentRequest = {
      customerExternalId: v.customerExternalId,
      customerName:       v.customerName,
      mobile:             v.mobile,
      email:              v.email || undefined,
      purposeCode:        v.purposeCode,
      purposeText:        v.purposeText,
      fiTypes:            v.fiTypes,
      consentStart:       new Date(v.consentStart).toISOString(),
      consentExpiry:      new Date(v.consentExpiry).toISOString(),
      fetchType:          v.fetchType,
      redirectUrl:        v.redirectUrl || undefined,
    };

    this.apiService.createConsent(req).subscribe({
      next: (consent) => {
        this.loading = false;
        // Redirect customer to Digio consent URL if available
        if (consent.consentUrl) {
          window.open(consent.consentUrl, '_blank');
        }
        this.router.navigate(['/consent', consent.id]);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Failed to create consent. Please try again.';
      }
    });
  }

  get f() { return this.form.controls; }
}
