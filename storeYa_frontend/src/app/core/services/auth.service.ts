import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {BehaviorSubject, catchError, tap, throwError} from 'rxjs';
import { apiUrl } from '../../../environment/api.config';


const BASE_URL = apiUrl.BASE_URL + '/auth';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private http = inject(HttpClient);
  private authState = new BehaviorSubject<boolean>(this.hasToken());

  isAuthenticated$ = this.authState.asObservable();

  login(account: { mail: string; password: string }) {
    return this.http.post(BASE_URL + '/login', account, {
      headers: {noauth: 'noauth'},
    }).pipe(
      tap((res: any) => {
        localStorage.setItem('access_token', res.accessToken);
        localStorage.setItem('refresh_token', res.refreshToken);
        this.authState.next(true);
        console.log('Đăng nhập thành công:', res.access_token);
      }),

      catchError((err) => {
        const msg = err?.error?.message || 'Đăng nhập thất bại!';
        return throwError(() => err);
      })
    );
  }

  logout() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('username');
    this.authState.next(false);
  }

  getUsername(): string {
    const token = localStorage.getItem('access_token');

    if (!token || token.split('.').length !== 3) {
      console.warn('Token không hợp lệ hoặc thiếu:', token);
      return 'người dùng';
    }

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.preferred_username || payload.email || 'người dùng';
    } catch (err) {
      console.warn(' Token decode fail:', err);
      return 'người dùng';
    }
  }

  getToken(): string {
    return localStorage.getItem('access_token') || '';
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
      headers: {noauth: 'noauth'},
    }).pipe(
      catchError((err) => {
        const msg = err?.error?.message || 'Đăng ký thất bại!';
        return throwError(() =>err);
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
}
