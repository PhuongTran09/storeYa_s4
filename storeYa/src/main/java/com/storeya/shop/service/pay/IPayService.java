package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface IPayService {

    Map<String, Object> createPayment(PaymentDTO dto, HttpServletRequest request);
    void processVnPayIPN(Map<String, String> vnPayParams);

    Map<String, Object> retryPayment(PaymentDTO dto, HttpServletRequest request);
    PaymentDTO confirmPayment(Long paymentId);

    PaymentDTO  cancelPayment(PaymentDTO dto,Long userId, HttpServletRequest request);
    List<PaymentDTO> getPaymentsByUser(Long userId);

}
