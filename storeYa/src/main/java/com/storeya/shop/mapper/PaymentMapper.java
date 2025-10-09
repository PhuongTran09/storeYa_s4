package com.storeya.shop.mapper;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.Payment;
import com.storeya.shop.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentDTO toDTO(Payment payment) {
        if (payment == null) return null;

        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setUserId(payment.getUser() != null ? payment.getUser().getId() : null);
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setRecipientName(payment.getRecipientName());
        dto.setRecipientPhone(payment.getRecipientPhone());
        dto.setRecipientAddress(payment.getRecipientAddress());
        dto.setRecipientEmail(payment.getRecipientEmail());
        dto.setDetails(payment.getDetails());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }

    public Payment toEntity(PaymentDTO dto) {
        if (dto == null) return null;

        Payment payment = new Payment();
        payment.setId(dto.getId());
        payment.setAmount(dto.getAmount());
        payment.setMethod(dto.getMethod());
        payment.setStatus(dto.getStatus());
        payment.setRecipientName(dto.getRecipientName());
        payment.setRecipientPhone(dto.getRecipientPhone());
        payment.setRecipientAddress(dto.getRecipientAddress());
        payment.setRecipientEmail(dto.getRecipientEmail());
        payment.setDetails(dto.getDetails());
        payment.setPaidAt(dto.getPaidAt());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            payment.setUser(user);
        }

        return payment;
    }
}
