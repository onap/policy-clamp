import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { map, tap } from 'rxjs/operators';
import { ControlLoopList } from '../../models/cl-list.model';
import { ClService } from '../../modules/monitoring/services/cl.service';

const API_URL = environment.apiUrl;

@Injectable( { providedIn: 'root' } )
export class DataService {
  constructor( private http: HttpClient, private clService: ClService ) {
  }

  public fetchControlLoopList(): Observable<ControlLoopList> {
    return this.http.get( `${ API_URL }/instantiation` )
      .pipe(
        map( responseData => {
          return ControlLoopList.fromJSON( responseData );
        } ),
        tap( clList => {
          this.clService.setControlLoopListInstatiation( clList );
        } )
      );
  }
}
