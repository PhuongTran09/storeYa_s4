import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { VndPipe } from '../../shared/pipes/truncate.pipe';

@Component({
  selector: 'app-payment-result',
  standalone: true,
  imports: [CommonModule, RouterLink,VndPipe], // Thêm RouterLink để tạo nút "Quay về"
  templateUrl: './payment-result.component.html',
  styleUrl: './payment-result.component.scss'
})
export class PaymentResultComponent implements OnInit {

  paymentStatus: 'success' | 'failed' | 'unknown' = 'unknown';
  message: string = 'Đang xử lý...';
  amount: number = 0;
  
  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    // Lắng nghe các tham số query trên URL
    this.route.queryParams.subscribe(params => {
      
      // Lấy mã phản hồi từ VNPay
      const responseCode = params['vnp_ResponseCode'];
      
      // Lấy các thông tin khác
      this.amount = Number(params['vnp_Amount']) / 100 || 0; // Chuyển về đơn vị VNĐ

      // Kiểm tra trạng thái
      if (responseCode === '00') {
        this.paymentStatus = 'success';
        this.message = 'Bạn đã thanh toán đơn hàng thành công!';
      } else {
        this.paymentStatus = 'failed';
        this.message = 'Thanh toán thất bại. Vui lòng thử lại hoặc liên hệ với cửa hàng.';
      }
    });
  }
}