import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

const USERNAME = environment.username;
const PASSWORD = environment.password;

@Injectable()
export class BasicAuthInterceptor implements HttpInterceptor {

  intercept( req: HttpRequest<any>, next: HttpHandler ): Observable<HttpEvent<any>> {

    const modReq = req.clone( {
      setHeaders: {
        Authorization: 'Basic ' + btoa( USERNAME + ':' + PASSWORD ),
        Accept: 'application/json',
        'Content-Type': 'application/json',
      },
      withCredentials: true
    } );

    return next.handle( modReq );
  }
}
