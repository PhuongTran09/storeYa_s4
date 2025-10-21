import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators'; // ✨ Import catchError
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/payments';

// Interface DTO (giữ nguyên của bạn)
export interface PaymentDTO {
  id?: number;
  userId?: number;
  amount?: number;
  method: string; // COD, VNPAY, MOMO
  status?: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  recipientEmail: string;
  details?: string;
  createdAt?: Date;
  updatedAt?: Date;
  paidAt?: Date;
  items?: any[];
  setCode?: string;
}

// ✨ Interface PaymentResponse đã được xóa

@Injectable({
  providedIn: 'root'
})
export class PaymentService {

  constructor(private http: HttpClient) { }

  /**
   * 🧾 Tạo đơn thanh toán mới (COD, VNPAY, etc.)
   * ✨ SỬA LỖI: Cập nhật kiểu dữ liệu trả về thành Observable<any>
   */
  createPayment(payment: PaymentDTO): Observable<any> {
    return this.http.post<any>(`${BASE_URL}`, payment).pipe(
      catchError(this.handleError) // ✨ Thêm xử lý lỗi
    );
  }

  /** 💳 Lấy danh sách thanh toán theo user hiện tại */
  getPaymentsByUser(): Observable<PaymentDTO[]> {
    return this.http.get<PaymentDTO[]>(`${BASE_URL}`).pipe(
      catchError(this.handleError) // ✨ Thêm xử lý lỗi
    );
  }

  /** ✅ Xác nhận thanh toán (cho admin) */
  confirmPayment(paymentId: number): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${BASE_URL}/${paymentId}/confirm`, {}).pipe(
      catchError(this.handleError) // ✨ Thêm xử lý lỗi
    );
  }

  /** ❌ Hủy thanh toán (cho user hoặc admin) */
  cancelPayment(paymentId: number): Observable<PaymentDTO> {
    return this.http.post<PaymentDTO>(`${BASE_URL}/${paymentId}/cancel`, {}).pipe(
      catchError(this.handleError) // ✨ Thêm xử lý lỗi
    );
  }

  /**
   * ✨ THÊM VÀO: Hàm xử lý lỗi chung
   */
  private handleError(error: HttpErrorResponse) {
    let errorMessage = 'Đã có lỗi không xác định xảy ra!';
    if (error.error instanceof ErrorEvent) {
      // Lỗi phía client
      errorMessage = `Lỗi: ${error.error.message}`;
    } else {
      // Lỗi phía backend
      // Backend của bạn có thể trả về lỗi trong error.error.message
      errorMessage = error.error?.message || `Lỗi máy chủ: ${error.status}`;
    }
    console.error(error);
    return throwError(() => new Error(errorMessage));
  }
}