import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule ,RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.scss']
})
export class ForgotPasswordComponent implements OnInit {

  @Output() loginClick = new EventEmitter<void>();

  openLogin() {
    this.loginClick.emit();
  }

  forgotForm!: FormGroup;
  showPassword = false;
  passwordFocused = false;
  otpSent = false;
  loading = false;
  countdown = 0;
  timer: any;


  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) { }

ngOnInit() {
  this.forgotForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    otp: ['', Validators.required],
    password: [{ value: '', disabled: true }, Validators.required] 
  });
  this.forgotForm.reset({
    email: '',
    otp: '',
    password: { value: '', disabled: true } 
  });
}

sendOtp() {
  const emailControl = this.forgotForm.get('email');
  if (!emailControl || emailControl.invalid) {
    alert('Vui lòng nhập email hợp lệ trước khi gửi OTP!');
    return;
  }

  if (this.countdown > 0) return;

  this.loading = true;
  this.authService.forgotPassword(emailControl.value).subscribe({
    next: () => {
      this.loading = false;
      this.startCountdown();
      this.otpSent = true;

      this.forgotForm.get('password')?.enable();
      alert('OTP đã gửi thành công!');
    },
    error: err => {
      this.loading = false;
      alert(err.message || 'Gửi OTP thất bại!');
    }
  });
}


startCountdown() {
  this.countdown = 60; // 60s chờ
  clearInterval(this.timer);
  this.timer = setInterval(() => {
    this.countdown--;
    if (this.countdown <= 0) {
      clearInterval(this.timer);
    }
  }, 1000);
}

resetPassword() {
  if (!this.otpSent) return;

  const { email, otp, password } = this.forgotForm.value;

  this.loading = true;

  this.authService.resetPassword(otp, password,email)
    .subscribe({
      next: () => {
        alert('Đặt lại mật khẩu thành công!');
        this.forgotForm.reset(); // reset form sau khi thành công
        this.loading = false;
        this.otpSent = false; // nếu muốn gửi OTP lại
        setTimeout(() => this.openLogin(), 2000);
      },
      error: err => {
        alert(err.message || 'Có lỗi xảy ra, thử lại sau!');
        this.loading = false;
      }
    });
}

  togglePasswordVisibility() {
    this.showPassword = !this.showPassword;
  }
}
