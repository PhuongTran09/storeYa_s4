import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
  ]
})
export class LoginComponent implements OnInit {

  @Output() registerClick = new EventEmitter<void>();

  openRegister() {
    this.registerClick.emit();
  }

  @Output() forgotPasswordClick = new EventEmitter<void>();
  openForgotPassword() {
    this.forgotPasswordClick.emit();
  }

  loginForm: FormGroup;
  isSubmitting = false;
  showLoginError = false;
  showPassword = false;
  passwordFocused = false;

  ngOnInit() {
    this.loginForm = this.fb.group({
      mail: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });

  }

  // Lưu message lỗi cho từng field
  errorMessages: any = {
    mail: '',
    password: ''
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      mail: ['', [Validators.required]],
      password: ['', [Validators.required]]
    });
  }


 onSubmit(): void {
  if (this.loginForm.invalid || this.isSubmitting) {
    this.loginForm.markAllAsTouched();
    return;
  }

  this.isSubmitting = true;
  this.showLoginError = false;

  this.authService.login(this.loginForm.value)
    .pipe(
      catchError((err) => {
        this.showLoginError = true;
        this.isSubmitting = false;
        return of(null);
      })
    )
    .subscribe({
      next: (res) => {
        if (res) {
          // Lấy roles từ JWT
          const roles = this.authService.getRoles();

          // Redirect theo role
          if (roles.includes('admin')) {
            this.router.navigate(['/admin/dashboard']);
          } else {
            this.router.navigate(['/home']);
          }
        }
      },
      error: () => {
        this.isSubmitting = false;
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
}



  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }
}
