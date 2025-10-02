package com.storeya.shop.dto;

import com.storeya.shop.entity.Cart;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Data
public class CartDTO {
    private Long id;
    private Long userId;
    private Double totalPrice;
    private List<CartItemDTO> items;



}
