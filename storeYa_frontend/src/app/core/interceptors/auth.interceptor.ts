import { Injectable } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (req.headers.has('noauth')) return next.handle(req);

    const token = this.authService.getToken();
    if (!token) return next.handle(req);

    const expiresAt = Number(localStorage.getItem('expires_at') || 0);
    if (Date.now() > expiresAt && token) {
      // access token hết hạn → refresh
      return this.authService.refreshToken().pipe(
        switchMap(() => {
          const newToken = this.authService.getToken();
          const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } });
          return next.handle(authReq);
        })
      );
    }

    // attach token bình thường
    const authReq = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    return next.handle(authReq);
  }
}
