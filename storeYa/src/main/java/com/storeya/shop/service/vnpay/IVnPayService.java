package com.storeya.shop.service.vnpay;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;

import java.util.Map;


public interface IVnPayService {
    String createVnPayPayment(HttpServletRequest request, long amount, String orderInfo,String orderId);

    Map<String, String> handleVnPayIPN(HttpServletRequest request);
    Map<String, String>  getVnPayParamsAsMap(HttpServletRequest request);
}
