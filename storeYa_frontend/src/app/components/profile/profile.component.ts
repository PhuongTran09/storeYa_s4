import { Component, OnInit } from '@angular/core';
import { User, UserService } from '../../core/services/user.service';

import { finalize } from 'rxjs';
import { ToastService } from '../../core/services/toast.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../../shared/layout/navbar/navbar.component';
import { Router, RouterLink, RouterModule } from "@angular/router";
import { AuthService } from '../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, RouterLink, ConfirmDialogComponent, RouterModule],
})
export class ProfileComponent implements OnInit {
  user: User = {};
  loading = false;
  saving = false;
  showLogoutConfirm = false;
  showCartConfirm = false;
  constructor(private userService: UserService, private toast: ToastService, private authService: AuthService, private router: Router) {}

  ngOnInit() {
    this.loadUser();
  }

  loadUser() {
    this.loading = true;
    this.userService.getCurrentUser()
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (res) => this.user = res,
        error: () => this.toast.show('Không tải được thông tin user!', 'error')
      });
  }

  save() {
    this.saving = true;
    this.toast.show('Đang lưu thông tin...', 'loading');
    this.userService.updateCurrentUser(this.user)
      .pipe(finalize(() => this.saving = false))
      .subscribe({
        next: () => this.toast.show('Cập nhật thành công!', 'success'),
        error: () => this.toast.show('Lưu thất bại!', 'error')
      });
  }
   onConfirmLogout() {
    this.authService.logout();
    this.router.navigate(['/home']);
  }

  logout() {
    this.showLogoutConfirm = true;
  }

  onCancelLogout() {
    this.showLogoutConfirm = false;
  }
}
