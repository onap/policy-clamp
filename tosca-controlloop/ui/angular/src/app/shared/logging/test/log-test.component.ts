import { Component } from '@angular/core';
import { LogService } from '../service/log.service';

@Component( {
  selector: 'tosca-log-test',
  templateUrl: 'log-test.component.html'
} )
export class LogTestComponent {
  constructor( private logger: LogService ) {
  }

  testLog(): void {
    this.logger.log( 'Test the `log()` Method' );
  }

  clearLog(): void {
    this.logger.clear();
  }
}
