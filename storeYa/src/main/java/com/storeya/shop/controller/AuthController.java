package com.storeya.shop.controller;

import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.ApiResponse;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.service.auth.IAuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final IAuthService authService;

    public AuthController(IAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {

        try {
            TokenResponse token = authService.login(request);
            logger.info("Login Successfully");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Login failed");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody @Valid RegisterRequest request) {
        try {
            authService.registerUser(request);
            return ResponseEntity.ok(ApiResponse.success("Registration successful", null));
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Registration failed"));
        }
    }
}
