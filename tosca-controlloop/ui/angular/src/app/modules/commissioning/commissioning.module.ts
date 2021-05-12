import { NgModule } from '@angular/core';
import { CommissioningComponent } from './component/commissioning.component';
import { SharedModule } from '../../shared/shared.module';
import { CoreModule } from '../../core/core.module';
import { CommonModule } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@NgModule( {
  declarations: [
    CommissioningComponent
  ],
  imports: [
    CommonModule,
    NgbModule,
    CoreModule,
    SharedModule
  ]
} )
export class CommissioningModule {
}
