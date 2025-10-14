import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ConfirmDialogComponent } from '../../confirm-dialog/confirm-dialog.component';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-navbar-admin',
  standalone: true,
  imports: [ConfirmDialogComponent,CommonModule],
  templateUrl: './navbar-admin.component.html',
  styleUrls: ['./navbar-admin.component.scss']
})
export class NavbarAdminComponent implements OnInit {
  username: any;
  showLogoutConfirm = false;

  constructor(private authService: AuthService, private router: Router,private userService: UserService) { }

  ngOnInit(): void {
    this.username = this.userService.getCurrentUser().subscribe({
          next: (user) => {
            this.username = user?.username ;
          },
          error: () => {
            this.username = 'User';
          },
        });
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
