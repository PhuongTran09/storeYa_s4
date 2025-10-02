import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { CommonModule } from '@angular/common';
import { LoginComponent } from '../../../components/auth/login/login.component';
import { RegisterComponent } from '../../../components/auth/register/register.component';
import { ForgotPasswordComponent } from '../../../components/auth/forgot-password/forgot-password.component';
import { NavbarService } from '../../../core/services/navbar.service';
import { ConfirmDialogComponent } from '../../confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LoginComponent,
    RegisterComponent,
    ForgotPasswordComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss'],
})
export class HeaderComponent implements OnInit {
  isLoggedIn = false;
  username = '';
  isUserMenuOpen = false;

  isMenuOpen = false;
  showLogin = false;
  showRegister = false;
  showForgotPassword = false;
  isClosing = false;
  showLogoutConfirm = false;

  constructor(private authService: AuthService, private router: Router, private navbarService: NavbarService) { }

  ngOnInit(): void {
    this.authService.isAuthenticated$.subscribe((loggedIn) => {
      this.isLoggedIn = loggedIn;
      this.username = loggedIn ? this.authService.getUsername() : '';
      if (loggedIn && this.showLogin) {
        this.navbarService.closeLoginModal();

      }
    });
    this.navbarService.loginModalState$.subscribe(state => {
      this.showLogin = state;
    });




  }

  /** Toggle mobile menu */
  toggleMenu() {
    this.isMenuOpen = !this.isMenuOpen;
  }
  toggleUserMenu() {
    this.isUserMenuOpen = !this.isUserMenuOpen;
  }

  /** Close menu when click link (mobile UX) */
  closeMenu() {
    this.isMenuOpen = false;
  }


  onConfirmLogout() {
    this.authService.logout();
    this.isUserMenuOpen = false;
    this.router.navigate(['/home']);
    window.location.href = '/home';
  }

  logout() {
    this.showLogoutConfirm = true;
  }

  onCancelLogout() {
    this.showLogoutConfirm = false;
  }

  onLoginSuccess() {
    this.navbarService.closeLoginModal();
    window.location.href = '/home';  // ép về home
  }




  /** mở login */
  toggleLogin() {
    if (this.showLogin) {
      this.navbarService.closeLoginModal();
      window.location.href = '/home';
    } else {
      this.resetModals();
      setTimeout(() => this.navbarService.openLoginModal(), 300);
    }
  }

  switchToRegister() {
    this.resetModals();
    this.showRegister = true;

  }

  switchToLogin() {
    this.resetModals();
    this.showLogin = true;

  }

  switchToForgotPassword() {
    this.resetModals();
    this.showForgotPassword = true;

  }

  /** Đóng modal */
  closeAll() {
    this.isClosing = true;
    setTimeout(() => {
      this.showLogin = false;
      this.showRegister = false;
      this.showForgotPassword = false;
      this.isClosing = false;
    }, 300); // match duration-300
  }

  private resetModals() {
    this.showLogin = false;
    this.showRegister = false;
    this.showForgotPassword = false;
  }
}
