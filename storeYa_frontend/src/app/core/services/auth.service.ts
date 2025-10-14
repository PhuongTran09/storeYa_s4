import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, tap, throwError, interval, of, Subscription, switchMap } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';
import { Route, Router } from '@angular/router';

const BASE_URL = apiUrl.BASE_URL + '/auth';

interface JwtToken {
  exp?: number;
  preferred_username?: string;
  email?: string;
  realm_access?: {
    roles?: string[];
  };
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private authState = new BehaviorSubject<boolean>(this.hasToken());
  isAuthenticated$ = this.authState.asObservable();

  private tokenWatcher?: Subscription;

  constructor(
    private router: Router,
  ) {

    this.startTokenWatcher();
  }

  // ===== LOGIN =====
  login(account: { mail: string; password: string }) {
    return this.http.post(BASE_URL + '/login', account, { headers: { noauth: 'noauth' } }).pipe(
      tap((res: any) => {
        localStorage.setItem('access_token', res.accessToken);
        localStorage.setItem('refresh_token', res.refreshToken);
        this.authState.next(true);
        this.startTokenWatcher();
      }),
      catchError(err => throwError(() => err))
    );
  }

  // ===== LOGOUT =====
  logout() {
    const refresh = localStorage.getItem('refresh_token');
    if (refresh) {
      this.http.post(BASE_URL + '/logout', { refreshToken: refresh }, { headers: { noauth: 'noauth' } })
        .subscribe({
          next: () => console.log('✅ Logged out on server'),
          error: (err) => console.error('❌ Server logout failed', err)
        });
    }

    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');

    this.authState.next(false);
    this.stopTokenWatcher();
    setTimeout(() => {
      this.router.navigate(['/']);
    }, 1500);
  }

  // ===== REFRESH TOKEN =====
  refreshToken() {
    const refresh = localStorage.getItem('refresh_token');
    if (!refresh) {
      this.logout();
      return throwError(() => new Error('No refresh token'));
    }

    return this.http.post(BASE_URL + '/refresh', { refreshToken: refresh }, { headers: { noauth: 'noauth' } })
      .pipe(
        tap((res: any) => {
          localStorage.setItem('access_token', res.accessToken);
          localStorage.setItem('refresh_token', res.refreshToken);
          this.authState.next(true);
          console.log('🔁 Token refreshed');
        }),
        catchError(err => {
          console.error('Refresh failed → logout');
          this.logout();
          return throwError(() => err);
        })
      );
  }

  // ===== AUTO CHECK TOKEN =====
  private startTokenWatcher() {
    this.stopTokenWatcher(); // tránh tạo nhiều interval trùng nhau

    this.tokenWatcher = interval(30 * 1000) // check mỗi 30 giây
      .pipe(
        switchMap(() => {
          const access = localStorage.getItem('access_token');
          const refresh = localStorage.getItem('refresh_token');

          if (!refresh) {
            console.warn('No refresh token → logout');
            this.logout();
            return of(null);
          }

          // Nếu refresh token hết hạn → logout ngay
          if (this.isTokenExpired(refresh)) {
            console.warn('Refresh token expired → logout');
            this.logout();
            return of(null);
          }

          // Nếu access token sắp hết hạn (còn < 60s) → gọi refresh
          if (access && this.getTokenExpiration(access) - Date.now() < 60 * 1000) {
            console.log('refreshing...');
            return this.refreshToken().pipe(
              catchError(() => {
                this.logout();
                return of(null);
              })
            );
          }

          return of(null);
        })
      )
      .subscribe();
  }

  private stopTokenWatcher() {
    this.tokenWatcher?.unsubscribe();
  }

  // ===== TOKEN UTIL =====
  private decodeToken(token: string): any {
    try {
      return JSON.parse(atob(token.split('.')[1]));
    } catch {
      return null;
    }
  }

  private getTokenExpiration(token: string): number {
    const decoded = this.decodeToken(token);
    return decoded?.exp ? decoded.exp * 1000 : 0;
  }

  private isTokenExpired(token: string): boolean {
    const exp = this.getTokenExpiration(token);
    if (!exp) return true;
    return Date.now() > exp;
  }

  // ===== HELPER =====
  getToken(): string {
    return localStorage.getItem('access_token') || '';
  }


  getRoles(): string[] {
    const token = this.getToken();
    if (!token || token.split('.').length !== 3) return [];
    try {
      const payload: JwtToken = JSON.parse(atob(token.split('.')[1]));
      return payload.realm_access?.roles || [];
    } catch {
      return [];
    }
  }

  isAuthenticated(): boolean {
    return this.authState.value;
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('access_token');
  }

  // ===== REGISTER / FORGOT / RESET =====
  register(account: {
    username: string;
    email: string;
    firstName: string;
    lastName: string;
    password: string;
  }) {
    return this.http.post(BASE_URL + '/register', account, {
      headers: { noauth: 'noauth' },
    }).pipe(
      catchError((err) => throwError(() => err))
    );
  }

  forgotPassword(email: string) {
    return this.http.post(BASE_URL + '/forgot-password', { email }, {
      headers: { noauth: 'noauth' },
    }).pipe(
      catchError((err) => throwError(() => new Error(err?.error?.message || 'Yêu cầu thất bại')))
    );
  }

  resetPassword(otp: string, newPassword: string, email: string) {
    return this.http.post(BASE_URL + '/reset-password', { otp, newPassword, email }, {
      headers: { noauth: 'noauth' },
    }).pipe(
      catchError((err) => throwError(() => new Error(err?.error?.message || 'Đặt lại mật khẩu thất bại')))
    );
  }
}
