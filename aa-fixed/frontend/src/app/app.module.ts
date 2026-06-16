import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ConsentFormComponent } from './components/consent-form/consent-form.component';
import { ConsentStatusComponent } from './components/consent-status/consent-status.component';
import { FetchDataComponent } from './components/fetch-data/fetch-data.component';
import { AccountsViewComponent } from './components/accounts-view/accounts-view.component';
import { AuthInterceptor } from './interceptors/auth.interceptor';

@NgModule({
  declarations: [
    AppComponent,
    ConsentFormComponent,
    ConsentStatusComponent,
    FetchDataComponent,
    AccountsViewComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    ReactiveFormsModule,
    FormsModule,
    AppRoutingModule
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
