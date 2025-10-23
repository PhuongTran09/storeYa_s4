package com.storeya.shop.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
public class VnPayConfig {

    @Value("${vnpay.tmnCode}")
    public String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    public String vnp_HashSecret;

    @Value("${vnpay.payUrl}")
    public String vnp_PayUrl;

    @Value("${vnpay.returnUrl}")
    public String vnp_ReturnUrl;

    @Value("${vnpay.version}")
    private String vnp_Version;

    @Value("${vnpay.apiUrl}")
    public String vnp_ApiUrl;


}
