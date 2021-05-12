import { ControlLoop } from './cl.model';
import { ControlLoopElement } from './cl-element.model';

export class ControlLoopList {
  private controlLoops: ControlLoop[] = [];
  private controlLoopElements: ControlLoopElement[] = [];

  static fromJSON( data: any ): ControlLoopList {
    const clList: ControlLoopList = new ControlLoopList();
    data.controlLoopList.forEach( ( newCL: ControlLoop ) => {
      for ( const key of Object.keys( newCL.elements ) ) {
        const element = newCL.elements[key];
        clList.controlLoopElements.push( element );
      }
      clList.push( newCL );
    } );

    return clList;
  }

  public get length(): number {
    return this.controlLoops.length;
  }

  public get cls(): ControlLoop[] {
    return this.controlLoops;
  }

  public get clEl(): ControlLoopElement[] {
    return this.controlLoopElements;
  }

  public push( cl: ControlLoop ): void {
    this.controlLoops.push( cl );
  }

  public isEmpty(): boolean {
    return this.cls.length === 0;
  }

  public clear(): void {
    this.controlLoops = [];
  }
}
