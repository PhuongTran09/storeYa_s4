package com.storeya.shop.service.vnpay;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;


public interface IVnPayService {
    String createVnPayPayment(HttpServletRequest request, long amount, String orderInfo,String orderId);

    Map<String, String> handleVnPayIPN(HttpServletRequest request);
    Map<String, String>  getVnPayParamsAsMap(HttpServletRequest request);

    Map<String, String>  refundPayment(HttpServletRequest request, long amount, String orderInfo, String orderId, String originalTransactionNo, String originalCreateDate);
}
