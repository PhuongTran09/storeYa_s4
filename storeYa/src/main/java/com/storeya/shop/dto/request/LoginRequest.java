package com.storeya.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class LoginRequest {

    @NotBlank(message = "Không được để trống")
    private String mail;
    @NotBlank(message = "Không được để trống")
    private String password;
}
