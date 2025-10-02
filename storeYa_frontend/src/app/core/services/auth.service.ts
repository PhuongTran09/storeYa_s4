import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, catchError, tap, throwError } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/auth';

interface JwtToken {
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

  constructor() {
    const token = localStorage.getItem('access_token');
    const expiresAt = Number(localStorage.getItem('expires_at') || 0);

    if (token && Date.now() < expiresAt) {
      this.authState.next(true);
      this.scheduleTokenExpiry(token);
    } else {
      this.logout();
    }
  }


  login(account: { mail: string; password: string }) {
  return this.http.post(BASE_URL + '/login', account, { headers: { noauth: 'noauth' } }).pipe(
    tap((res: any) => {
      localStorage.setItem('access_token', res.accessToken);
      localStorage.setItem('refresh_token', res.refreshToken);

      const expiresAt = Date.now() + res.expiresIn * 1000;
      localStorage.setItem('expires_at', expiresAt.toString());
      this.authState.next(true);
      this.scheduleTokenExpiry(res.accessToken);
    }),
    catchError(err => throwError(() => err))
  );
}


  private scheduleTokenExpiry(token: string) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000;
      const now = Date.now();
      const timeout = exp - now;

      if (timeout > 0) {
        setTimeout(() => this.refreshToken().subscribe(), timeout);
      } else {
        this.logout();
      }
    } catch (e) {
      console.error('Token invalid:', e);
      this.logout();
    }
  }



  logout() {
    const refresh = localStorage.getItem('refresh_token');

    if (refresh) {
      // gọi API backend để logout trên Keycloak
      this.http.post(BASE_URL + '/logout', { refreshToken: refresh }, { headers: { noauth: 'noauth' } })
        .subscribe({
          next: () => console.log('Logged out on server'),
          error: (err) => console.error('Server logout failed', err)
        });
    }

    // Xóa token local
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('expires_at');
    this.authState.next(false);
  }


  getToken(): string {
    return localStorage.getItem('access_token') || '';
  }

  getUsername(): string {
    const token = this.getToken();
    if (!token || token.split('.').length !== 3) return 'người dùng';

    try {
      const payload: JwtToken = JSON.parse(atob(token.split('.')[1]));
      return payload.preferred_username || payload.email || 'người dùng';
    } catch {
      return 'người dùng';
    }
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
      catchError((err) => {
        const msg = err?.error?.message || 'Đăng ký thất bại!';
        return throwError(() => err);
      })
    );
  }

  forgotPassword(email: string) {
    return this.http.post(BASE_URL + '/forgot-password', { email }, {
      headers: { noauth: 'noauth' },
    }).pipe(
      catchError((err) => {
        const msg = err?.error?.message || 'Yêu cầu lấy lại mật khẩu thất bại!';
        return throwError(() => new Error(msg));
      })
    );
  }

  resetPassword(otp: string, newPassword: string, email: string) {
    return this.http.post(BASE_URL + '/reset-password', { otp, newPassword, email }, {
      headers: { noauth: 'noauth' },
    }).pipe(
      catchError((err) => {
        const msg = err?.error?.message || 'Đặt lại mật khẩu thất bại!';
        return throwError(() => new Error(msg));
      })
    );
  }

  refreshToken() {
    const userId = localStorage.getItem('userId');
    if (!userId) {
      this.logout();
      return throwError(() => new Error('No user ID found'));
    }
    const refresh = localStorage.getItem('refresh_token');
    if (!refresh) {
      return throwError(() => new Error('No refresh token found'));
    }

    return this.http.post(BASE_URL + '/refresh', { refreshToken: refresh }, { headers: { noauth: 'noauth' } })
      .pipe(
        tap((res: any) => {
          localStorage.setItem('access_token', res.accessToken);
          localStorage.setItem('refresh_token', res.refreshToken);

          const expiresAt = Date.now() + res.expiresIn * 1000;
          localStorage.setItem('expires_at', expiresAt.toString());

          this.authState.next(true);

          // đặt lại hẹn giờ
          this.scheduleTokenExpiry(res.accessToken);
        }),
        catchError(err => {
          this.logout();
          return throwError(() => err);
        })
      );
  }



}
