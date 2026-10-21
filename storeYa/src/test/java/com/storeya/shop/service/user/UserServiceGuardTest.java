package com.storeya.shop.service.user;

import com.storeya.shop.dto.UserDTO;
import com.storeya.shop.entity.User;
import com.storeya.shop.mapper.UserMapper;
import com.storeya.shop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * POST /api/users used to write whatever it was given. Two rows sharing a username break
 * AuthService.getUserIdFromToken, which resolves a Keycloak subject against that column,
 * and two rows sharing an email make OTP delivery ambiguous.
 */
class UserServiceGuardTest {

    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository, new UserMapper());
    }

    @Test
    void aTakenUsernameIsRefusedBeforeSaving() {
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.createUser(dto(null, "dup", "fresh@example.com")));

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aTakenEmailIsRefusedBeforeSaving() {
        when(userRepository.existsByUsername("unique")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.createUser(dto(null, "unique", "taken@example.com")));

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void aFreshIdentityIsSaved() {
        when(userRepository.existsByUsername("unique")).thenReturn(false);
        when(userRepository.existsByEmail("fresh@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserDTO saved = service.createUser(dto(null, "unique", "fresh@example.com"));

        assertEquals("unique", saved.getUsername());
        verify(userRepository).save(any(User.class));
    }

    /** Editing someone else's email is how an attacker takes over the OTP route. */
    @Test
    void stealingAnotherPersonsEmailIsRefused() {
        User target = user(1L, "target@example.com");
        User other = user(2L, "victim@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmailNormalized("victim@example.com")).thenReturn(Optional.of(other));

        ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                () -> service.updateUser(1L, dto(1L, "target", "victim@example.com")));

        assertEquals(HttpStatus.CONFLICT, failure.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void keepingYourOwnEmailStillWorks() {
        User target = user(1L, "target@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmailNormalized("target@example.com")).thenReturn(Optional.of(target));
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserDTO updated = service.updateUser(1L, dto(1L, "target", "target@example.com"));

        assertEquals("target@example.com", updated.getEmail());
        verify(userRepository).save(target);
    }

    /** /api/users/me routes here, so a profile edit that changes nothing must not 409. */
    @Test
    void renamingToAnEmailNobodyElseHoldsWorks() {
        User target = user(1L, "old@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(target));
        when(userRepository.findByEmailNormalized("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        UserDTO updated = service.updateUser(1L, dto(1L, "target", "new@example.com"));

        assertEquals("new@example.com", updated.getEmail());
    }

    private static User user(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setUsername("u" + id);
        return user;
    }

    private static UserDTO dto(Long id, String username, String email) {
        UserDTO dto = new UserDTO();
        dto.setId(id);
        dto.setUsername(username);
        dto.setEmail(email);
        return dto;
    }
}
