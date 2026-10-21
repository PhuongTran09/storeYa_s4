package com.storeya.shop.dto;

import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private String image;

}
