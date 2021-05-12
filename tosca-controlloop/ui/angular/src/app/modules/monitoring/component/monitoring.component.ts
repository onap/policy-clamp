import { Component, OnInit } from '@angular/core';
import { LogService } from '../../../shared/logging/service/log.service';
import { DataService } from '../../../shared/services/data.service';

@Component( {
  selector: 'tosca-monitoring',
  templateUrl: './monitoring.component.html',
  styleUrls: [ './monitoring.component.scss' ]
} )
export class MonitoringComponent implements OnInit {
  public isCollapsed = false;

  constructor( private logger: LogService, private dataService: DataService ) {
  }

  ngOnInit(): void {
    this.logger.debug( '======== Monitoring Component Init ========' );
    this.dataService.fetchControlLoopList();
  }
}
