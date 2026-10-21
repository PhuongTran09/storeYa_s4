package com.storeya.shop.service.cart;

import com.storeya.shop.entity.Cart;

import java.util.List;

public interface ICartService {
    Cart addToCart(Long userId, Long productId, int quantity);
    Cart getCart(Long userId);

    // cartItemId, not productId: CartService.updateItem/removeItem match on CartItem.getId(),
    // which is exactly what the frontend holds (cart-item.component.ts emits item.id).
    Cart updateItem(Long userId, Long cartItemId, int quantity);
    Cart removeItem(Long userId, Long cartItemId);
    void clearCart(Long userId);
    List<Cart> getAllCarts();
}
