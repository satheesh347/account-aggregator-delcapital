import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AaApiService } from '../../services/aa-api.service';
import { AccountResponse, FetchSessionResponse } from '../../models/aa.models';

@Component({
  selector: 'app-accounts-view',
  templateUrl: './accounts-view.component.html',
  styleUrls: ['./accounts-view.component.css']
})
export class AccountsViewComponent implements OnInit {

  session: FetchSessionResponse | null = null;
  accounts: AccountResponse[] = [];
  selectedAccount: AccountResponse | null = null;
  loading = true;
  error = '';

  constructor(private route: ActivatedRoute, private api: AaApiService) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.paramMap.get('sessionId')!;
    this.api.getFetchStatus(sessionId).subscribe({
      next: s => {
        this.session = s;
        this.loadAccounts(sessionId);
      },
      error: () => { this.error = 'Could not load session.'; this.loading = false; }
    });
  }

  private loadAccounts(sessionId: string): void {
    this.api.getAccounts(sessionId).subscribe({
      next: accounts => {
        this.accounts = accounts;
        if (accounts.length) this.selectedAccount = accounts[0];
        this.loading = false;
      },
      error: () => { this.error = 'Could not load account data.'; this.loading = false; }
    });
  }

  selectAccount(account: AccountResponse): void {
    this.selectedAccount = account;
  }

  totalCredit(account: AccountResponse): number {
    return account.transactions
      .filter(t => t.txnType === 'CREDIT')
      .reduce((sum, t) => sum + t.amount, 0);
  }

  totalDebit(account: AccountResponse): number {
    return account.transactions
      .filter(t => t.txnType === 'DEBIT')
      .reduce((sum, t) => sum + t.amount, 0);
  }

  txnClass(txnType: string): string {
    return txnType === 'CREDIT' ? 'credit' : 'debit';
  }
}
