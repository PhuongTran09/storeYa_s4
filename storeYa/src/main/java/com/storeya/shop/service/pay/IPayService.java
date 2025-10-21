package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface IPayService {

    Map<String, Object> createPayment(PaymentDTO dto, HttpServletRequest request);

    Map<String, Object> retryPayment(Long paymentId, HttpServletRequest request);
    void processVnPayIPN(Map<String, String> vnPayParams);

    PaymentDTO confirmPayment(Long paymentId);
    PaymentDTO cancelPayment(Long paymentId);
    List<PaymentDTO> getPaymentsByUser(Long userId);

}
