package com.storeya.shop.controller;

import com.storeya.shop.dto.request.ForgotPasswordRequest;
import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.ApiResponse;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.service.auth.IAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final IAuthService authService;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {

        try {
            TokenResponse token = authService.login(request);
            logger.info("Login Successfully");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            logger.error("Login failed for user {}: {}", request.getMail(), e.getMessage());
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
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email không được để trống"));
        }

        try {
            authService.sendOtp(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "OTP đã gửi đến email của bạn"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Gửi OTP thất bại: " + e.getMessage()));
        }
    }


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() ||
                request.getOtp() == null || request.getOtp().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email, OTP và mật khẩu mới đều là bắt buộc"));
        }

        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Đổi mật khẩu thất bại: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String refreshToken = (String) session.getAttribute("refresh_token");
        if (refreshToken != null) {
            authService.logout(refreshToken); // gọi Keycloak logout
        }
        session.invalidate(); // clear session
        return ResponseEntity.ok(Map.of("message", "Logout thành công"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        try {
            TokenResponse newToken = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            System.err.println("Refresh token invalid or expired: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_refresh_token"));
        }
    }





}
