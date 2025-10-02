package com.storeya.shop.controller;

import com.storeya.shop.dto.CartDTO;
import com.storeya.shop.entity.Cart;
import com.storeya.shop.mapper.CartMapper;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.cart.ICartService;
import org.keycloak.representations.AccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final ICartService cartService;
    private final CartMapper cartMapper;
    private final IAuthService authService;

    public CartController(ICartService cartService, CartMapper cartMapper, IAuthService authService) {
        this.cartService = cartService;
        this.cartMapper = cartMapper;
        this.authService = authService;
    }



}
