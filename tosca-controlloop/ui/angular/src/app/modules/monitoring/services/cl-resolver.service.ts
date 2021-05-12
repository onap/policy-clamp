import { ActivatedRouteSnapshot, Resolve, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';
import { Injectable } from '@angular/core';

import { DataService } from '../../../shared/services/data.service';
import { ClService } from './cl.service';
import { ControlLoopList } from '../../../models/cl-list.model';
import { LogService } from '../../../shared/logging/service/log.service';

@Injectable( { providedIn: 'root' } )
export class ClResolverService implements Resolve<ControlLoopList> {

  constructor( private logger: LogService, private dataStorageService: DataService, private clService: ClService ) {
  }

  resolve( route: ActivatedRouteSnapshot, state: RouterStateSnapshot ):
    Observable<ControlLoopList> | Promise<ControlLoopList> | ControlLoopList {
    this.logger.debug( '======== CL Resolver ========' );
    const controlLoopList = this.clService.getControlLoopList();

    if ( controlLoopList.isEmpty() ) {
      const clList = this.dataStorageService.fetchControlLoopList();
      this.logger.debug( 'CLList', clList );
      return clList;
    }

    return controlLoopList;
  }
}
