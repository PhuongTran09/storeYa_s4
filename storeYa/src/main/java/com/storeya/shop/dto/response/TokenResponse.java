package com.storeya.shop.dto.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    public TokenResponse(String accessToken, String refreshToken, long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;

    }

}
