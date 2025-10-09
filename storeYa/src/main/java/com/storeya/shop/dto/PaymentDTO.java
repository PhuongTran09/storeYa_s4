package com.storeya.shop.dto;

import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private Long id;
    private Long userId;  // ánh xạ từ payment.user.id
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String recipientName;
    private String recipientPhone;
    private String recipientAddress;
    private String recipientEmail;
    private String details;
    private LocalDateTime paidAt;
}
