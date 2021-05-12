import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { ControlLoopList } from '../../../../models/cl-list.model';
import { LogService } from '../../../../shared/logging/service/log.service';
import { ClService } from '../../services/cl.service';

@Component( {
  selector: 'tosca-cl-component',
  templateUrl: './cl.component.html',
  styleUrls: [ './cl.component.scss' ]
} )
export class ClComponent implements OnInit, AfterViewInit, OnDestroy {
  public clsSubscription: Subscription;
  public clList: ControlLoopList;

  constructor( private logger: LogService, private clService: ClService, private cd: ChangeDetectorRef ) {
  }

  ngOnInit(): void {
    this.logger.debug( '======== CL Component Init ========' );
    this.clsSubscription = this.clService.clsChanged.subscribe(
      ( clList: ControlLoopList ) => {
        this.clList = clList;
      }
    );

    this.clList = this.clService.getControlLoopList();
  }

  ngAfterViewInit(): void {
    this.cd.detectChanges();
  }

  numberToLetterConverter( num: number ): string {
    const newNum = num + 65;

    return String.fromCharCode( newNum );
  }

  ngOnDestroy(): void {
    this.logger.debug( '======== CL Component Destroy ========' );
    this.clsSubscription.unsubscribe();
  }

}
