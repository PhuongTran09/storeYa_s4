package com.storeya.shop.repository;

import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

}
