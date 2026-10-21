import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PaymentDTO, PaymentService } from '../../../core/services/payment.service';
import { UserService } from '../../../core/services/user.service';
import { ToastService } from '../../../core/services/toast.service';
import { HeaderComponent } from '../../../shared/layout/navbar/navbar.component';



@Component({
  selector: 'app-user-list-pay',
  standalone: true,
  imports: [CommonModule, HeaderComponent],
  templateUrl: './user-list-pay.component.html',
  styleUrls: ['./user-list-pay.component.scss']
})
export class UserListPayComponent implements OnInit {
  orders: PaymentDTO[] = [];
  loading = false;

  constructor(
    private paymentService: PaymentService,
    private userService: UserService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  /** 📦 Lấy danh sách đơn hàng của user hiện tại */
loadOrders(): void {
  this.loading = true;
  this.paymentService.getPaymentsByUser().subscribe({
    next: (data) => {
      this.orders = data;
      this.loading = false;
    },
    error: () => {
      this.toast.show('Không thể tải đơn hàng', 'error');
      this.loading = false;
    }
  });
}


  /** 🟢 Format trạng thái hiển thị cho đẹp */
  getStatusText(status: string | undefined): string {
    switch (status) {
      case 'PENDING': return 'Đang xử lý';
      case 'PAID': return 'Đã thanh toán';
      case 'CANCELLED': return 'Đã hủy';
      default: return 'Không xác định';
    }
  }
}
