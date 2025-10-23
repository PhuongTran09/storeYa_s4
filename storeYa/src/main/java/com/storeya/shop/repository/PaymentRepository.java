package com.storeya.shop.repository;

import com.storeya.shop.entity.Payment;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUserId(Long userId);

    Optional<Payment> findFirstByUserIdAndStatusAndMethod(Long userId, PaymentStatus status, PaymentMethod method);

    Optional<Payment> findBySetCode(String setCode);
}
