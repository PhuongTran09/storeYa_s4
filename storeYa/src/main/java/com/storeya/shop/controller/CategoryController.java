package com.storeya.shop.controller;

import com.storeya.shop.entity.Category;
import com.storeya.shop.service.category.ICategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ICategoryService categoryService;

    public CategoryController(ICategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Lấy tất cả categories
    @GetMapping("public/all")
    public ResponseEntity<List<Category>> getAll() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    // Lấy category theo id (JSON body)
    @PostMapping("/get")
    public ResponseEntity<Category> getById(@RequestBody Category request) {
        Optional<Category> category = categoryService.findById(request.getId());
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Thêm category mới
    @PostMapping()
    public ResponseEntity<Category> create(@RequestBody Category category) {
        return ResponseEntity.ok(categoryService.create(category));
    }

    // Cập nhật category
    @PutMapping("{id}")
    public ResponseEntity<Category> update(@PathVariable Long id,@RequestBody Category category) {
        Optional<Category> existing = categoryService.findById(id);
        if (existing.isPresent()) {
            Category updated = existing.get();
            updated.setName(category.getName());
            return ResponseEntity.ok(categoryService.update(id,updated));
        }
        return ResponseEntity.notFound().build();
    }

    // Xóa category
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
