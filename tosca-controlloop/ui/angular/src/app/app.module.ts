import { ErrorHandler, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

import { HeaderComponent } from './modules/header/component/header.component';
import { GlobalErrorInterceptor } from './core/interceptors/global-error.interceptor';
import { BasicAuthInterceptor } from './core/interceptors/basic-auth.interceptor';
import { HttpErrorInterceptorService } from './core/interceptors/http-error-interceptor.service';
import { SharedModule } from './shared/shared.module';
import { CommissioningModule } from './modules/commissioning/commissioning.module';
import { MonitoringModule } from './modules/monitoring/monitoring.module';
import { CoreModule } from './core/core.module';

@NgModule( {
  declarations: [
    AppComponent,
    HeaderComponent
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    NgbModule,
    AppRoutingModule,
    CoreModule,
    SharedModule,
    CommissioningModule,
    MonitoringModule
  ],
  providers: [
    [
      {
        provide: ErrorHandler,
        useClass: GlobalErrorInterceptor
      },
      {
        provide: HTTP_INTERCEPTORS,
        useClass: BasicAuthInterceptor,
        multi: true
      },
      {
        provide: HTTP_INTERCEPTORS,
        useClass: HttpErrorInterceptorService,
        multi: true
      }
    ],
  ],
  bootstrap: [ AppComponent ]
} )
export class AppModule {
}
