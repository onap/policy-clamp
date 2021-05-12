import { RouterModule, Routes } from '@angular/router';
import { NgModule } from '@angular/core';
import { CommissioningComponent } from './modules/commissioning/component/commissioning.component';
import { MonitoringComponent } from './modules/monitoring/component/monitoring.component';
import { ClResolverService } from './modules/monitoring/services/cl-resolver.service';

const toscaRoutes: Routes = [
  { path: '', redirectTo: '/monitoring', pathMatch: 'full' },
  { path: 'commissioning', component: CommissioningComponent },
  { path: 'monitoring', component: MonitoringComponent, resolve: [ ClResolverService ] },
];

@NgModule( {
  imports: [ RouterModule.forRoot( toscaRoutes ) ],
  exports: [ RouterModule ]
} )
export class AppRoutingModule {
}
