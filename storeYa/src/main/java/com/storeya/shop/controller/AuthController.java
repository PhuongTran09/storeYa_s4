package com.storeya.shop.controller;

import com.storeya.shop.dto.request.ForgotPasswordRequest;
import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.ApiResponse;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.service.auth.IAuthService;
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
            logger.info("Login succeeded for user");
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            // Cause stays server-side: authService.login wraps Keycloak's response text,
            // which names the issuer URL and grant details.
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Sai email hoặc mật khẩu."));
        }
    }

    /**
     * No try/catch here: duplicates surface as ResponseStatusException(409) and anything
     * unexpected becomes a generic 500, both handled by GlobalExceptionHandler.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@RequestBody @Valid RegisterRequest request) {
        authService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success("Registration successful", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email không được để trống"));
        }

        // Always the same reply. sendOtp stays quiet about unknown addresses and
        // exhausted quotas, so this endpoint cannot confirm or deny an account.
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "OTP đã gửi đến email của bạn"));
    }


    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody ForgotPasswordRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank() ||
                request.getOtp() == null || request.getOtp().isBlank() ||
                request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Email, OTP và mật khẩu mới đều là bắt buộc"));
        }

        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    /**
     * The refresh token comes from the request body, which is what the frontend already
     * sends. It used to be read from HttpSession, but nothing ever wrote that attribute
     * (login only returns the token), so Keycloak logout was never called and the SSO
     * session survived "sign out". Taking HttpSession as a parameter also made Spring
     * mint a throwaway session per call.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) Map<String, String> body) {
        String refreshToken = body == null ? null : body.get("refreshToken");
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        } else {
            logger.warn("Logout requested without a refresh token; Keycloak session left active");
        }
        return ResponseEntity.ok(Map.of("message", "Logout thành công"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        try {
            TokenResponse newToken = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            logger.warn("Refresh token invalid or expired: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid_refresh_token"));
        }
    }

}
