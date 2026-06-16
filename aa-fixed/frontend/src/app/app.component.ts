import { Component } from '@angular/core';

@Component({
  selector: 'app-root',
  template: `
    <div class="aa-shell">
      <header class="aa-header">
        <div class="aa-header-inner">
          <div class="aa-logo">
            <span class="aa-logo-mark">DC</span>
            <span class="aa-logo-text">Del Capital — Account Aggregator</span>
          </div>
          <nav class="aa-nav">
            <a routerLink="/consent/new" routerLinkActive="active">New Consent</a>
          </nav>
        </div>
      </header>
      <main class="aa-main">
        <router-outlet></router-outlet>
      </main>
      <footer class="aa-footer">
        <p>Del Capital Pvt. Ltd. · RBI AA Framework · Data handled securely per PDPB guidelines</p>
      </footer>
    </div>
  `,
  styles: [`
    .aa-shell { display: flex; flex-direction: column; min-height: 100vh; font-family: 'Segoe UI', system-ui, sans-serif; background: #f8fafc; }
    .aa-header { background: #1e293b; color: #fff; padding: 0; box-shadow: 0 2px 8px rgba(0,0,0,.2); }
    .aa-header-inner { max-width: 1100px; margin: 0 auto; padding: 0 24px; height: 60px; display: flex; align-items: center; justify-content: space-between; }
    .aa-logo { display: flex; align-items: center; gap: 12px; }
    .aa-logo-mark { background: #3b82f6; color: #fff; border-radius: 6px; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 13px; }
    .aa-logo-text { font-size: 15px; font-weight: 600; letter-spacing: .2px; }
    .aa-nav a { color: #94a3b8; text-decoration: none; font-size: 14px; padding: 6px 12px; border-radius: 6px; transition: all .2s; }
    .aa-nav a.active, .aa-nav a:hover { color: #fff; background: rgba(255,255,255,.1); }
    .aa-main { flex: 1; max-width: 1100px; margin: 0 auto; padding: 32px 24px; width: 100%; }
    .aa-footer { background: #1e293b; color: #64748b; font-size: 12px; text-align: center; padding: 12px; }
  `]
})
export class AppComponent {
  title = 'Del Capital AA';
}
