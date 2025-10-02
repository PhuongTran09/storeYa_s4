package com.storeya.shop.service.cart;

import com.storeya.shop.entity.Cart;
import java.util.List;

public interface ICartService {
    Cart addToCart(Long userId, Long productId, int quantity);
    Cart getCart(Long userId);
    Cart updateItem(Long userId, Long productId, int quantity);
    Cart removeItem(Long userId, Long productId);
    void clearCart(Long userId);
    List<Cart> getAllCarts();
}
