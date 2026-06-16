import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ConsentFormComponent } from './components/consent-form/consent-form.component';
import { ConsentStatusComponent } from './components/consent-status/consent-status.component';
import { FetchDataComponent } from './components/fetch-data/fetch-data.component';
import { AccountsViewComponent } from './components/accounts-view/accounts-view.component';

const routes: Routes = [
  { path: '', redirectTo: 'consent/new', pathMatch: 'full' },
  { path: 'consent/new', component: ConsentFormComponent },
  { path: 'consent/:id', component: ConsentStatusComponent },
  { path: 'fetch/:consentId', component: FetchDataComponent },
  { path: 'accounts/:sessionId', component: AccountsViewComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
