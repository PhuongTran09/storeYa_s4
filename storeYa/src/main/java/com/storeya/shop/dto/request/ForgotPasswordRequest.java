package com.storeya.shop.dto.request;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class ForgotPasswordRequest {
    @NotBlank
    private String email;
    @NotBlank
    private String newPassword;
    @NotBlank
    private String otp;
}
