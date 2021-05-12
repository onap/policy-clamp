import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { ControlLoopList } from '../../../models/cl-list.model';

@Injectable( { providedIn: 'root' } )
export class ClService {
  public clsChanged = new Subject<ControlLoopList>();
  private clList: ControlLoopList = new ControlLoopList();

  constructor() {
  }

  public setControlLoopListInstatiation( clList: ControlLoopList ): void {
    this.clList = clList;
    this.clsChanged.next( this.clList );
  }

  public getControlLoopList(): ControlLoopList {
    return this.clList;
  }
}
