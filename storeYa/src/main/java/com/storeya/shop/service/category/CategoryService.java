package com.storeya.shop.service.category;

import com.storeya.shop.entity.Category;
import com.storeya.shop.repository.CategoryRepository;
import com.storeya.shop.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService implements ICategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Category create(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public void delete(Long id) {
        boolean exists = productRepository.existsByCategoryId(id);

        if (exists) {
            throw new IllegalStateException("Không thể xóa category này vì vẫn còn product đang sử dụng.");
        }
        categoryRepository.deleteById(id);
    }


    public Category update(Long id, Category updated) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        existing.setName(updated.getName());
        return categoryRepository.save(existing);
    }
}
