package com.storeya.shop.config;


import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Configuration
@Getter
// @Data would add setters and, worse, a toString() containing the merchant secret,
// which then rides along in any log.info("...{}", config).
@ToString(exclude = "vnp_HashSecret")
public class VnPayConfig {

    @Value("${vnpay.tmnCode}")
    private String vnp_TmnCode;

    @Value("${vnpay.hashSecret}")
    private String vnp_HashSecret;

    @Value("${vnpay.payUrl}")
    private String vnp_PayUrl;

    @Value("${vnpay.returnUrl}")
    private String vnp_ReturnUrl;

    @Value("${vnpay.version}")
    private String vnp_Version;

    @Value("${vnpay.apiUrl}")
    private String vnp_ApiUrl;


}
