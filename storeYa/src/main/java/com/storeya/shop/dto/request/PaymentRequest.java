package com.storeya.shop.dto.request;

import lombok.Data;

@Data
public class PaymentRequest {
    private long amount;
    private String orderInfo;
    private String orderId;
}
