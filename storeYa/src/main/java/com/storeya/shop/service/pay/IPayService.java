package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;

import java.util.List;

public interface IPayService {

    /**
     * Tạo một giao dịch thanh toán (pending)
     */
    PaymentDTO createPayment(PaymentDTO dto);

    /**
     * Xác nhận thanh toán thành công
     */
    PaymentDTO confirmPayment(Long paymentId, String transactionId);

    /**
     * Hủy thanh toán
     */
    PaymentDTO cancelPayment(Long paymentId);

    /**
     * Lấy danh sách thanh toán của user
     */
    List<PaymentDTO> getPaymentsByUser(Long userId);

    /**
     * Lấy chi tiết thanh toán theo ID
     */
    PaymentDTO getPaymentById(Long id);
}
