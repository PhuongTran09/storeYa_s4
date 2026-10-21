package com.storeya.shop.service.user;

import com.storeya.shop.dto.UserDTO;
import java.util.List;

public interface IUserService {

    List<UserDTO> getAllUsers();

    UserDTO getUserById(Long id);

    UserDTO createUser(UserDTO userDTO);

    UserDTO updateUser(Long id, UserDTO userDTO);

    void deleteUser(Long id);
    UserDTO updateCurrentUser(Long userId, UserDTO userDTO);
}
