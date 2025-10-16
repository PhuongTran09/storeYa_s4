import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {apiUrl} from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/payments';

export interface PaymentDTO {
  id?: number;
  userId?: number;
  amount?: number;
  method: string; // COD, MOMO, BANK_TRANSFER
  status?: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  recipientEmail: string;
  details?: string;
  paidAt?: Date;
  items?: any[]; // Chi tiết sản phẩm trong đơn hàng
}

@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  constructor(private http: HttpClient) {}

  /** 🧾 Tạo đơn thanh toán mới (COD, MOMO, etc.) */
  createPayment(payment: PaymentDTO): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${BASE_URL}`, payment);
  }

  /** 💳 Lấy danh sách thanh toán theo user hiện tại */
  getPaymentsByUser(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(`${BASE_URL}`);
  }

  /** ✅ Xác nhận thanh toán (cho admin hoặc sau khi Momo callback) */
  confirmPayment(paymentId: number): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${BASE_URL}/${paymentId}/confirm`, {});
  }

  /** ❌ Hủy thanh toán (cho user hoặc admin) */
  cancelPayment(paymentId: number): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${BASE_URL}/${paymentId}/cancel`, {});
  }
}
