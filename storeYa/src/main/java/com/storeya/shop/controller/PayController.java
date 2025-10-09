package com.storeya.shop.controller;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.service.pay.IPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PayController {

    private final IPayService payService;

    @PostMapping
    public PaymentDTO createPayment(@RequestBody PaymentDTO dto) {
        return payService.createPayment(dto);
    }

    @GetMapping("/user/{userId}")
    public List<PaymentDTO> getPaymentsByUser(@PathVariable Long userId) {
        return payService.getPaymentsByUser(userId);
    }

    @PostMapping("/{paymentId}/confirm")
    public PaymentDTO confirmPayment(@PathVariable Long paymentId) {
        return payService.confirmPayment(paymentId, null);
    }

    @PostMapping("/{paymentId}/cancel")
    public PaymentDTO cancelPayment(@PathVariable Long paymentId) {
        return payService.cancelPayment(paymentId);
    }
}
