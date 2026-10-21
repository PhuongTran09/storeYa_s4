package com.storeya.shop.mapper;

import com.storeya.shop.dto.OrderItemDTO;
import com.storeya.shop.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {
    public OrderItemDTO toDTO(OrderItem item) {
        if (item == null) return null;
        OrderItemDTO dto = new OrderItemDTO();
        dto.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
        dto.setProductName(item.getProductName());
        dto.setPrice(item.getPrice());
        dto.setQuantity(item.getQuantity());
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            dto.setImage(item.getProduct().getImages().get(0).getUrl());
        }
        return dto;
    }
}
