package com.storeya.shop.service.auth;

import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.entity.User;

public interface IAuthService {
    TokenResponse login(LoginRequest request);

    User registerUser(RegisterRequest registerUser);

    void sendOtp(String email);
    void logout(String refreshToken);
    void resetPassword(String email, String otp, String newPassword);
}
