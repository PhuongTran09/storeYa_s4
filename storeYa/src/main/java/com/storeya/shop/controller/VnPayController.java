package com.storeya.shop.controller;

import com.storeya.shop.dto.request.PaymentRequest;
import com.storeya.shop.service.vnpay.IVnPayService;
import com.storeya.shop.service.vnpay.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
public class VnPayController {

    private final IVnPayService vnPayService;

    public VnPayController(VnPayService vnPayService) {
        this.vnPayService = vnPayService;
    }

    @PostMapping("/create-payment")
    public ResponseEntity<Map<String, String>> createPayment(HttpServletRequest request, @RequestBody PaymentRequest paymentRequest) {
        String paymentUrl = vnPayService.createVnPayPayment(
                request,
                paymentRequest.getAmount(),
                paymentRequest.getOrderInfo(),
                paymentRequest.getOrderId()
        );
        return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVnPayIPN(HttpServletRequest request) {
        Map<String, String> response = vnPayService.handleVnPayIPN(request);
        return ResponseEntity.ok(response);
    }
}