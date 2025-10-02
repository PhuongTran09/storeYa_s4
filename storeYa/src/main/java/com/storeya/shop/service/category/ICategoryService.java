package com.storeya.shop.service.category;

import com.storeya.shop.entity.Category;
import com.storeya.shop.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ICategoryService {
    Optional<Category> findById(Long id);
    List<Category> findAll();
    Category create(Category category);
    void delete(Long id);
    Category update(Long id, Category updated);
}
