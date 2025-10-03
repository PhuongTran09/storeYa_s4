package com.storeya.shop.mapper;

import com.storeya.shop.dto.CartDTO;
import com.storeya.shop.dto.CartItemDTO;
import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.CartItem;
import com.storeya.shop.entity.ProductImage;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CartMapper {

    private final ModelMapper modelMapper;

    public CartMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;

        modelMapper.typeMap(CartItem.class, CartItemDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getProduct().getId(), CartItemDTO::setProductId);
            mapper.map(src -> src.getProduct().getName(), CartItemDTO::setProductName);
        });
    }

    public CartDTO toDTO(Cart cart) {
        CartDTO dto = modelMapper.map(cart, CartDTO.class);

        dto.setItems(cart.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    public CartItemDTO toItemDTO(CartItem item) {
        CartItemDTO dto = modelMapper.map(item, CartItemDTO.class);

        // map ảnh ở đây (chỉ khi có dữ liệu)
        if (item.getProduct().getImages() != null && !item.getProduct().getImages().isEmpty()) {
            dto.setImage(item.getProduct().getImages().get(0).getUrl());
        }
        return dto;
    }
}
