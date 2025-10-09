package com.storeya.shop.mapper;

import com.storeya.shop.dto.UserDTO;
import com.storeya.shop.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
    User toEntity(UserDTO dto);
}
