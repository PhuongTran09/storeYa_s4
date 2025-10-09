package com.storeya.shop.controller;

import com.storeya.shop.dto.UserDTO;
import com.storeya.shop.mapper.UserMapper;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.user.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final IUserService userService;
    private final IAuthService authService;

    public UserController(IUserService userService, IAuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // 🔹 Lấy tất cả user
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // 🔹 Lấy thông tin user hiện tại từ token Keycloak
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal Jwt principal) {
        Long userId = authService.getUserIdFromToken(principal);
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/me")
    public ResponseEntity<UserDTO> updateCurrentUser(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody UserDTO userDTO) {

        Long userId = authService.getUserIdFromToken(principal);
        UserDTO updatedUser = userService.updateCurrentUser(userId, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    // 🔹 Lấy user theo ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    // 🔹 Tạo user mới
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.createUser(userDTO));
    }

    // 🔹 Cập nhật thông tin user
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        return ResponseEntity.ok(userService.updateUser(id, userDTO));
    }


    // 🔹 Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
