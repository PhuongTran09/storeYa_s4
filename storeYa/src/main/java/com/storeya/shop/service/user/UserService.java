package com.storeya.shop.service.user;

import com.storeya.shop.dto.UserDTO;
import com.storeya.shop.entity.User;
import com.storeya.shop.mapper.UserMapper;
import com.storeya.shop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElse(null);
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        // Duplicate identities make getUserIdFromToken unpredictable, since that lookup
        // resolves a Keycloak username against the users table.
        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username đã tồn tại.");
        }
        if (userDTO.getEmail() != null && userRepository.existsByEmail(userDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng.");
        }
        User user = userMapper.toEntity(userDTO);
        User saved = userRepository.save(user);
        return userMapper.toDTO(saved);
    }

    @Override
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        return userRepository.findById(id)
                .map(existing -> {
                    // /api/users/me lands here, so a user can reach this with their own id.
                    // Taking over someone else's email would hijack OTP delivery routing.
                    if (userDTO.getEmail() != null && !userDTO.getEmail().equals(existing.getEmail())) {
                        userRepository.findByEmailNormalized(userDTO.getEmail())
                                .filter(owner -> !owner.getId().equals(id))
                                .ifPresent(owner -> {
                                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                                            "Email đã được sử dụng.");
                                });
                    }
                    existing.setFirstName(userDTO.getFirstName());
                    existing.setLastName(userDTO.getLastName());
                    existing.setEmail(userDTO.getEmail());
                    existing.setPhone(userDTO.getPhone());
                    existing.setAddress(userDTO.getAddress());
                    User updated = userRepository.save(existing);
                    return userMapper.toDTO(updated);
                })
                .orElse(null);
    }

    @Override
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDTO updateCurrentUser(Long userId, UserDTO userDTO) {
        return updateUser(userId, userDTO);
    }
}
