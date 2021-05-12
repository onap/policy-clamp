import { NgModule } from '@angular/core';
import { StateColorDirective } from './directives/state-color.directive';
import { OrderedStateColorDirective } from './directives/ordered-state-color.directive';

@NgModule( {
  declarations: [
    StateColorDirective,
    OrderedStateColorDirective,
  ],
  exports: [
    StateColorDirective,
    OrderedStateColorDirective
  ]
} )
export class CoreModule {
}
