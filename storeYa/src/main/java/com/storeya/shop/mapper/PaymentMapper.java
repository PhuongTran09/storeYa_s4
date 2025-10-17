package com.storeya.shop.mapper;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.Payment;
import com.storeya.shop.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PaymentMapper {
    private final OrderItemMapper orderItemMapper;
    public PaymentDTO toDTO(Payment payment) {
        if (payment == null) return null;

        PaymentDTO dto = new PaymentDTO();
        dto.setUserId(payment.getUser() != null ? payment.getUser().getId() : null);
        dto.setAmount(payment.getAmount());
        dto.setMethod(payment.getMethod());
        dto.setStatus(payment.getStatus());
        dto.setRecipientName(payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
        dto.setRecipientPhone(payment.getUser().getPhone());
        dto.setRecipientAddress(payment.getUser().getAddress());
        dto.setRecipientEmail(payment.getUser().getEmail());
        dto.setDetails(payment.getDetails());
        dto.setPaidAt(payment.getPaidAt());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setSetCode(payment.getSetCode());
        dto.setUpdatedAt(payment.getUpdateAt());

        if (payment.getItems() != null && !payment.getItems().isEmpty()) {
            dto.setItems(payment.getItems().stream()
                    .map(orderItemMapper::toDTO) // Dùng orderItemMapper để chuyển đổi từng item
                    .collect(Collectors.toList()));
        } else {
            dto.setItems(Collections.emptyList()); // Trả về mảng rỗng thay vì null
        }

        return dto;

    }

    public Payment toEntity(PaymentDTO dto) {
        if (dto == null) return null;

        Payment payment = new Payment();
        payment.setAmount(dto.getAmount());
        payment.setMethod(dto.getMethod());
        payment.setStatus(dto.getStatus());
        payment.setRecipientName(dto.getRecipientName());
        payment.setRecipientPhone(dto.getRecipientPhone());
        payment.setRecipientAddress(dto.getRecipientAddress());
        payment.setRecipientEmail(dto.getRecipientEmail());
        payment.setDetails(dto.getDetails());
        payment.setPaidAt(dto.getPaidAt());
        payment.setCreatedAt(dto.getCreatedAt());
        payment.setSetCode(dto.getSetCode());
        payment.setUpdateAt(dto.getUpdatedAt());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            payment.setUser(user);
        }

        return payment;
    }
}
