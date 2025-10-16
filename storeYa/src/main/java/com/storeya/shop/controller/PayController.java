package com.storeya.shop.controller;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.pay.IPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody PaymentDTO paymentDTO,
            HttpServletRequest request
    ) {
        Map<String, Object> result = payService.createPayment(paymentDTO, request);
        return ResponseEntity.ok(result);
    }


    @PostMapping("/{paymentId}/confirm")
    public PaymentDTO confirmPayment(@PathVariable Long paymentId) {
        return payService.confirmPayment(paymentId, null);
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
}
