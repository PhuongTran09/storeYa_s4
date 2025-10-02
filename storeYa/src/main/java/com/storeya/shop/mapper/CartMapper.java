package com.storeya.shop.mapper;

import com.storeya.shop.dto.CartDTO;
import com.storeya.shop.dto.CartItemDTO;
import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.CartItem;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class CartMapper {

    private final ModelMapper modelMapper;

    public CartMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;

        // Cấu hình custom mapping cho CartItem
        modelMapper.typeMap(CartItem.class, CartItemDTO.class).addMappings(mapper -> {
            mapper.map(src -> src.getProduct().getId(), CartItemDTO::setProductId);
            mapper.map(src -> src.getProduct().getName(), CartItemDTO::setProductName);
        });
    }

    public CartDTO toDTO(Cart cart) {
        CartDTO dto = modelMapper.map(cart, CartDTO.class);

        // convert list items
        dto.setItems(cart.getItems().stream()
                .map(item -> modelMapper.map(item, CartItemDTO.class))
                .collect(Collectors.toList()));

        return dto;
    }

    public CartItemDTO toItemDTO(CartItem item) {
        return modelMapper.map(item, CartItemDTO.class);
    }
}
