import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertComponent } from './alert/component/alert.component';
import { LogTestComponent } from './logging/test/log-test.component';

@NgModule( {
  declarations: [
    AlertComponent,
    LogTestComponent
  ],
  imports: [ CommonModule ],
  exports: [
    AlertComponent,
    LogTestComponent
  ]
} )
export class SharedModule {
}
