package com.storeya.shop.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private String image;
}
