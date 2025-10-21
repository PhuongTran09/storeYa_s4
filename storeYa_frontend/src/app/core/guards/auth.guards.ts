import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { NavbarService } from '../services/navbar.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const navbarService = inject(NavbarService);
  const router = inject(Router);

  const url = state.url;

  const hasVnPayParams = route.queryParamMap.has('vnp_ResponseCode');

  // 1. Chưa login
  if (!authService.isAuthenticated()) {
    // Nếu là admin hoặc user route => về 404
    if (url.startsWith('/admin') || url.startsWith('/user')) {
      router.navigateByUrl('/404');
      return false;
    }

    // Các route public thì vẫn cho modal login
    navbarService.openLoginModal();
    return false;
  }

  // 2. Parse roles
  const token = authService.getToken();
  let roles: string[] = [];
  try {
    const payload: any = JSON.parse(atob(token.split('.')[1]));
    roles = payload.realm_access?.roles || [];
  } catch (e) {
    console.error('Invalid token', e);
    router.navigateByUrl('/404');
    return false;
  }

  // 3. Check role
  if (url.startsWith('/admin') && !roles.includes('admin')) {
    router.navigateByUrl('/404');
    return false;
  }

  if (url.startsWith('/user') && !roles.includes('user')) {
    router.navigateByUrl('/404');
    return false;
  }



  return true;
};

export const paymentResultGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  
  const router = inject(Router);

  // Kiểm tra xem URL có chứa tham số 'vnp_ResponseCode' mà VNPay trả về không
  const hasVnPayParams = route.queryParamMap.has('vnp_ResponseCode');

  if (hasVnPayParams) {
    // Nếu có, cho phép truy cập
    return true;
  } else {
    // Nếu không có (truy cập chay), đá về trang chủ
    console.warn('Truy cập trái phép vào trang payment-result, đang điều hướng về trang chủ.');
    router.navigate(['/']); // Điều hướng về trang chủ
    return false; // Chặn truy cập
  }
};
