import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { AaApiService } from '../../services/aa-api.service';
import { ConsentResponse } from '../../models/aa.models';

@Component({
  selector: 'app-consent-status',
  templateUrl: './consent-status.component.html',
  styleUrls: ['./consent-status.component.css']
})
export class ConsentStatusComponent implements OnInit, OnDestroy {

  consent: ConsentResponse | null = null;
  loading = true;
  error = '';
  private pollSub?: Subscription;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private api: AaApiService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loadConsent(id);

    // Poll every 5s while PENDING
    this.pollSub = interval(5000).pipe(
      switchMap(() => this.api.getConsent(id)),
      takeWhile(c => c.status === 'PENDING', true)
    ).subscribe({
      next: c => { this.consent = c; this.loading = false; },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  private loadConsent(id: string): void {
    this.api.getConsent(id).subscribe({
      next: c => { this.consent = c; this.loading = false; },
      error: () => { this.error = 'Could not load consent.'; this.loading = false; }
    });
  }

  openConsentUrl(): void {
    if (this.consent?.consentUrl) {
      window.open(this.consent.consentUrl, '_blank');
    }
  }

  proceedToFetch(): void {
    if (this.consent) {
      this.router.navigate(['/fetch', this.consent.id]);
    }
  }

  statusClass(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'badge-warning',
      ACTIVE:  'badge-success',
      REJECTED: 'badge-danger',
      REVOKED:  'badge-danger',
      EXPIRED:  'badge-secondary',
      PAUSED:   'badge-warning',
    };
    return map[status] ?? 'badge-secondary';
  }

  statusIcon(status: string): string {
    const map: Record<string, string> = {
      PENDING: '⏳', ACTIVE: '✅', REJECTED: '❌', REVOKED: '🚫', EXPIRED: '⌛', PAUSED: '⏸'
    };
    return map[status] ?? '•';
  }
}
