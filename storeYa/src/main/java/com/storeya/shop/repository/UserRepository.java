package com.storeya.shop.repository;

import com.storeya.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    Optional<User> findByEmailNormalized(@Param("email") String email);

    Optional<User> findByUsername(String username);

    Optional<User> findById(Long id);

}
