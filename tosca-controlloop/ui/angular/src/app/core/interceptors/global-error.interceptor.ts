import { ErrorHandler, Injectable } from '@angular/core';
import { LogService } from '../../shared/logging/service/log.service';
import { AlertService } from '../../shared/alert/services/alert.service';

@Injectable()
export class GlobalErrorInterceptor implements ErrorHandler {

  constructor( private logger: LogService, private alertService: AlertService ) {
  }

  handleError( error: any ): void {

    this.logger.error( '======== Global Error Interceptor ========' );
    this.logger.error( error.message );

    this.alertService.error( error.message );
  }
}
