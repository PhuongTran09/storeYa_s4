package com.storeya.shop.repository;

import com.storeya.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findAllByOrderByIdAsc(Pageable pageable);
    boolean existsByCategoryId(Long categoryId);
}
