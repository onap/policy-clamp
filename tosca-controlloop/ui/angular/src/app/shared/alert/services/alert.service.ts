import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { Alert } from '../model/alert.model';
import { filter } from 'rxjs/operators';
import { AlertSettings } from '../model/alert-settings';
import { LogService } from '../../logging/service/log.service';

@Injectable( {
  providedIn: 'root'
} )
export class AlertService {
  private subject = new BehaviorSubject<Alert>( null );
  private defaultId = 'default-alert';

  constructor( private logger: LogService ) {
  }

  onAlert( id = this.defaultId ): Observable<Alert> {
    return this.subject.asObservable().pipe( filter( x => x && x.id === id ) );
  }

  success( message: string, options?: any ): void {
    this.alert( new Alert( { ...options, alertType: AlertSettings.SUCCESS, message } ) );
    this.logger.log( message, { ...options, alertType: AlertSettings.SUCCESS } );
  }

  error( message: string, options?: any ): void {
    this.alert( new Alert( { ...options, alertType: AlertSettings.ERROR, message } ) );
    this.logger.error( message, { ...options, alertType: AlertSettings.ERROR } );
  }

  info( message: string, options?: any ): void {
    this.alert( new Alert( { ...options, alertType: AlertSettings.INFO, message } ) );
    this.logger.log( message, { ...options, alertType: AlertSettings.INFO } );
  }

  warn( message: string, options?: any ): void {
    this.alert( new Alert( { ...options, alertType: AlertSettings.WARNING, message } ) );
    this.logger.warn( message, { ...options, alertType: AlertSettings.WARNING } );
  }

  alert( alert: Alert ): void {
    alert.id = alert.id || this.defaultId;
    this.subject.next( alert );
  }

  clear( id = this.defaultId ): void {
    this.subject.next( new Alert( { id } ) );
  }
}
