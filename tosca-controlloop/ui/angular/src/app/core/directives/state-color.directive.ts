import { Directive, ElementRef, HostBinding, Input, OnInit, Renderer2 } from '@angular/core';
import { ControlLoopState } from '../../models/enums/cl-state.enum';

@Directive( {
  selector: '[toscaStateColor]'
} )
export class StateColorDirective implements OnInit {
  @Input() state: ControlLoopState;

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

    switch ( this.state ) {
      case ControlLoopState.UNINITIALISED:
        this.elClass.push( 'btn-unitialised' );
        break;
      case ControlLoopState.PASSIVE:
        this.elClass.push( 'btn-passive' );
        break;
      case ControlLoopState.RUNNING:
        this.elClass.push( 'btn-running' );
        break;
      default:
        this.elClass.push( 'btn-default' );
        break;
    }
  }

}
