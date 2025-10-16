package com.storeya.shop.controller;

import com.storeya.shop.dto.CartDTO;
import com.storeya.shop.entity.Cart;
import com.storeya.shop.mapper.CartMapper;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.cart.ICartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final ICartService cartService;
    private final CartMapper cartMapper;
    private final IAuthService authService;


    private Long getUserId(Jwt principal) {
        return authService.getUserIdFromToken(principal);
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(@AuthenticationPrincipal Jwt principal) {
        Long userId = getUserId(principal);
        Cart cart = cartService.getCart(userId);
        return ResponseEntity.ok(cartMapper.toDTO(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addToCart(@AuthenticationPrincipal Jwt principal,
                                             @RequestParam Long productId,
                                             @RequestParam int quantity) {
        Long userId = getUserId(principal);
        Cart cart = cartService.addToCart(userId, productId, quantity);
        return ResponseEntity.ok(cartMapper.toDTO(cart));
    }

    @PutMapping("/items/update/{productId}")
    public ResponseEntity<CartDTO> updateItem(@AuthenticationPrincipal Jwt principal,
                                              @PathVariable Long productId,
                                              @RequestParam int quantity) {
        Long userId = getUserId(principal);
        Cart cart = cartService.updateItem(userId, productId, quantity);
        return ResponseEntity.ok(cartMapper.toDTO(cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Cart> removeItem(@AuthenticationPrincipal Jwt principal,
                                           @PathVariable Long itemId) {
        Long userId = getUserId(principal);
        Cart updatedCart = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(updatedCart);
    }


    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt principal) {
        Long userId = getUserId(principal);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
