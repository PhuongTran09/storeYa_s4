import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  /**
   * Keycloak rotates the refresh token on every refresh, so two concurrent 401s that
   * each call refreshToken() with the same stale token means the second one fails and
   * forces a logout. Sharing one in-flight refresh prevents that.
   */
  private refreshing: Observable<string> | null = null;

  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.headers.has('noauth')) return next.handle(req);

    const token = this.authService.getToken();
    if (!token) return next.handle(req);

    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });

    return next.handle(authReq).pipe(
      catchError((err: HttpErrorResponse) => {
        // The auth endpoints are the recovery path itself; retrying them would loop.
        if (err.status !== 401 || req.url.includes('/api/auth/')) {
          return throwError(() => err);
        }
        return this.refreshOnce().pipe(
          switchMap(fresh => next.handle(this.withToken(req, fresh)))
        );
      })
    );
  }

  private refreshOnce(): Observable<string> {
    if (this.refreshing) return this.refreshing;

    this.refreshing = this.authService.refreshToken().pipe(
      map(() => this.authService.getToken()),
      finalize(() => { this.refreshing = null; }),
      shareReplay(1)
    );
    return this.refreshing;
  }

  private withToken(req: HttpRequest<any>, token: string): HttpRequest<any> {
    return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
}
