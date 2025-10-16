import {Component} from '@angular/core';
import {CartService} from '../../core/services/cart.service';
import {PaymentDTO, PaymentService} from '../../core/services/payment.service';
import {ToastService} from '../../core/services/toast.service';
import {Router, RouterLink} from '@angular/router';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {UserService} from '../../core/services/user.service';
import {VndPipe} from '../../shared/pipes/truncate.pipe';

@Component({
  selector: 'app-payments',
  standalone: true,
  imports: [CommonModule, FormsModule, VndPipe, RouterLink],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.scss'
})
export class PaymentsComponent {
   cartItems: any[] = [];
  total = 0;

  payment: PaymentDTO = {
    method: 'COD',
    recipientName: '',
    recipientPhone: '',
    recipientAddress: '',
    recipientEmail: '',
    details: ''
  };

  loading = false;

  constructor(
    private cartService: CartService,
    private paymentService: PaymentService,
    private toast: ToastService,
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCart();
    this.loadUser();
  }

 /** 🛒 Load giỏ hàng user hiện tại */
  loadCart(): void {
    this.cartService.loadCart().subscribe({
      next: (res: any) => {
        this.cartItems = res.items ?? [];
        this.cartService.total$.subscribe(total => {
          this.total = total;
        });
      },
      error: (err) => {
        console.error('❌ Load cart failed', err);
        this.toast.show('Không thể tải giỏ hàng', 'error');
      }
    });
  }

   loadUser() {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.payment.recipientName = `${user.firstName} ${user.lastName}`.trim();
        this.payment.recipientPhone = user.phone || '';
        this.payment.recipientAddress = user.address || '';
        this.payment.recipientEmail = user.email || '';
      },
      error: () => {
        this.toast.show('Không thể tải thông tin người dùng','warning');
      }
    });
  }

  /** 💸 Gửi yêu cầu tạo thanh toán (COD) */
  submitOrder(): void {
    if (!this.payment.recipientName || !this.payment.recipientPhone || !this.payment.recipientAddress) {
      this.toast.show('Vui lòng nhập đầy đủ thông tin nhận hàng','warning');
      return;
    }

    this.loading = true;
    this.paymentService.createPayment(this.payment).subscribe({
      next: () => {
        this.toast.show('Đặt hàng thành công!','success');
        this.router.navigate(['/orders']);
      },
      error: (err) => {
        console.error('Create payment failed', err);
        this.toast.show('Đặt hàng thất bại!','error');
        this.loading = false;
      }
    });
  }
}
