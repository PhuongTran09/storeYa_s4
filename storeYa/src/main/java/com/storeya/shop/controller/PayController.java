package com.storeya.shop.controller;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.pay.IPayService;
import com.storeya.shop.service.vnpay.IVnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PayController {

    private final IPayService payService;
    private final IAuthService authService;
    private final IVnPayService  vnPayService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody PaymentDTO paymentDTO,
            HttpServletRequest request,
            @AuthenticationPrincipal Jwt principal
    ) {
        Long userId = authService.getUserIdFromToken(principal);
        paymentDTO.setUserId(userId);
        Map<String, Object> result = payService.createPayment(paymentDTO, request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('admin')")
    public PaymentDTO confirmPayment(@PathVariable Long paymentId) {
        return payService.confirmPayment(paymentId);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> userGetPayments(@AuthenticationPrincipal Jwt principal) {
        Long userId = authService.getUserIdFromToken(principal);
        List<PaymentDTO> payments = payService.getPaymentsByUser(userId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentDTO cancelPayment(@PathVariable Long paymentId) {
        return payService.cancelPayment(paymentId);
    }

    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> handleVnPayIPN(HttpServletRequest request) {
        // ✨ SECURE FLOW: Validate first, then process
        Map<String, String> response = vnPayService.handleVnPayIPN(request);

        if ("00".equals(response.get("RspCode"))) {
            Map<String, String> vnPayParams = vnPayService.getVnPayParamsAsMap(request);
            payService.processVnPayIPN(vnPayParams);
        }

        return ResponseEntity.ok(response);
    }
}
