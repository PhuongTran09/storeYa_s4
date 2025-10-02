import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../confirm-dialog/confirm-dialog.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-navbar-admin',
  standalone: true,
  imports: [ConfirmDialogComponent,CommonModule],
  templateUrl: './navbar-admin.component.html',
  styleUrls: ['./navbar-admin.component.scss']
})
export class NavbarAdminComponent implements OnInit {
  username = '';
  showLogoutConfirm = false;

  constructor(private authService: AuthService, private router: Router) { }

  ngOnInit(): void {
    this.username = this.authService.getUsername();
  }

  onConfirmLogout() {
    this.authService.logout();
    this.router.navigate(['/home']);
    window.location.href = '/home';
  }

  logout() {
    this.showLogoutConfirm = true;
  }

  onCancelLogout() {
    this.showLogoutConfirm = false;
  }

}
