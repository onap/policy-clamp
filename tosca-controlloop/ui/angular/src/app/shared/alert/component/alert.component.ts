import { Component, Input, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { Alert } from '../model/alert.model';
import { Subscription } from 'rxjs';
import { AlertService } from '../services/alert.service';
import { AlertSettings } from '../model/alert-settings';

@Component( {
  selector: 'tosca-app-alert',
  templateUrl: 'alert.component.html',
  styleUrls: [ 'alert.component.scss' ],
  encapsulation: ViewEncapsulation.None
} )
export class AlertComponent implements OnInit, OnDestroy {
  @Input() id = 'default-alert';
  @Input() fade = true;

  alerts: Alert[] = [];
  alertSubscription: Subscription;
  routeSubscription: Subscription;

  constructor( private alertService: AlertService ) {
  }

  ngOnInit(): void {
    this.alertSubscription = this.alertService.onAlert( this.id )
      .subscribe( alert => {
        if ( !alert.message ) {
          this.alerts = [];
          return;
        }

        this.alerts.push( alert );

        if ( alert.autoClose ) {
          setTimeout( () => this.removeAlert( alert ), 3000 );
        }
      } );
  }

  removeAlert( alert: Alert ): void {
    if ( !this.alerts.includes( alert ) ) {
      return;
    }

    if ( this.fade ) {
      this.alerts.find( x => x === alert ).fade = true;

      setTimeout( () => {
        this.alerts = this.alerts.filter( x => x !== alert );
      }, 250 );
    } else {
      this.alerts = this.alerts.filter( x => x !== alert );
    }
  }

  cssClass( alert: Alert ): string {
    if ( !alert ) {
      return;
    }

    const classes = [ 'alert', 'alert-dismissible' ];

    const alertTypeClass = {
      [AlertSettings.SUCCESS]: 'alert alert-success',
      [AlertSettings.ERROR]: 'alert alert-danger',
      [AlertSettings.INFO]: 'alert alert-info',
      [AlertSettings.WARNING]: 'alert alert-warning'
    };

    classes.push( alertTypeClass[alert.alertType] );

    if ( alert.fade ) {
      classes.push( 'fade' );
    }

    return classes.join( ' ' );
  }

  ngOnDestroy(): void {
    if ( this.alertSubscription ) {
      this.alertSubscription.unsubscribe();
    }

    if ( this.routeSubscription ) {
      this.routeSubscription.unsubscribe();
    }
  }

}
