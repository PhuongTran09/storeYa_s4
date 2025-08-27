import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { catchError, of } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
  ]
})
export class RegisterComponent implements OnInit {
  registerForm!: FormGroup;
  isSubmitting = false;
  showPassword = false;
  passwordFocused = false;
  showRegisterSuccess = false;
  notifyMessage: string = '';
  notifyType: 'success' | 'error' | null = null;

  errorMessages: Record<string, string> = {
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: ''
  };

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

  ngOnInit() {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(6)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(35), Validators.pattern(/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/)]],
      firstName: ['', [Validators.required, Validators.maxLength(25),]],
      lastName: ['', [Validators.required, Validators.maxLength(25)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    this.registerForm.valueChanges.subscribe(() => {
      this.updateErrorMessages();
      if (this.notifyMessage) {
        this.notifyMessage = '';
        this.notifyType = null;
      }
    });

  }

  /** Hàm chung để lấy lỗi của control */
  private getErrorMessage(control: AbstractControl | null, messages: { [key: string]: string }): string {
    if (!control || (!control.touched && !control.dirty) || control.valid) return '';
    for (const errorKey in messages) {
      if (control.hasError(errorKey)) {
        return messages[errorKey];
      }
    }
    return '';
  }

  updateErrorMessages() {
    const c = this.registerForm.controls;
    this.errorMessages['username'] = this.getErrorMessage(c['username'], {
      required: 'Tên đăng nhập không được để trống.',
      minlength: 'Tên đăng nhập phải ít nhất 6 ký tự.'
    });

    this.errorMessages['email'] = this.getErrorMessage(c['email'], {
      required: 'Email không được để trống.',
      pattern: 'Email phải có định dạng hợp lệ.',
      email: 'Email không hợp lệ.',

    });

    this.errorMessages['firstName'] = this.getErrorMessage(c['firstName'], {
      required: 'First name không được để trống.'
    });

    this.errorMessages['lastName'] = this.getErrorMessage(c['lastName'], {
      required: 'Last name không được để trống.'
    });

    this.errorMessages['password'] = this.getErrorMessage(c['password'], {
      required: 'Mật khẩu không được để trống.',
      minlength: 'Mật khẩu phải ít nhất 6 ký tự.'
    });
  }

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }

  onSubmit() {
    if (this.registerForm.invalid || this.isSubmitting) {
      this.registerForm.markAllAsTouched();
      this.updateErrorMessages();
      return;
    }

    this.isSubmitting = true;

    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.notifyMessage = 'Đăng ký thành công! Chuyển hướng đến trang đăng nhập...';
        this.notifyType = 'success';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.notifyMessage = err.error?.message || 'Đăng ký thất bại!';
        this.notifyType = 'error';
        this.isSubmitting = false;
      }

    });
  }
}
