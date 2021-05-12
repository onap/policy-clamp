import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Injectable()
export class HttpErrorInterceptorService implements HttpInterceptor {

  intercept( req: HttpRequest<any>, next: HttpHandler ): Observable<HttpEvent<any>> {

    return next.handle( req ).pipe( catchError( err => {

      let errorMessage = 'An unknown error occurred!';

      if ( !err.error || !err.error.error ) {
        return throwError( errorMessage );
      }

      switch ( err.error.status ) {
        case 400:
          errorMessage = 'Bad Request!';
          break;
        case 401:
          errorMessage = 'Unauthorized!';
          break;
        default:
          errorMessage = 'An unknown error occurred!';

      }

      return throwError( errorMessage );
    } ) );
  }

}
