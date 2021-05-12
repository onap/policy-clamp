import { Directive, ElementRef, HostBinding, Input, OnInit, Renderer2 } from '@angular/core';
import { ControlLoopOrderedState } from '../../models/enums/cl-ordered-state.enum';

@Directive( {
  selector: '[toscaOrderedStateColor]'
} )
export class OrderedStateColorDirective implements OnInit {
  @Input() orderedState: ControlLoopOrderedState;

  protected elClass: string[] = [];

  @Input( 'class' )
  @HostBinding( 'class' )
  get elementClass(): string {
    return this.elClass.join( ' ' );
  }

  set( val: string ): void {
    this.elClass = val.split( ' ' );
  }

  constructor( private elRef: ElementRef, private renderer: Renderer2 ) {
  }

  ngOnInit(): void {
    this.renderer.addClass( this.elRef.nativeElement, this.elClass[1] );

    this.checkState();
  }

  private checkState(): void {
    this.elClass.push( 'btn' );

    switch ( this.orderedState ) {
      case ControlLoopOrderedState.UNINITIALISED:
        this.elClass.push( 'btn-unitialised' );
        break;
      case ControlLoopOrderedState.PASSIVE:
        this.elClass.push( 'btn-passive' );
        break;
      case ControlLoopOrderedState.RUNNING:
        this.elClass.push( 'btn-running' );
        break;
      default:
        this.elClass.push( 'btn-default' );
        break;
    }
  }

}
