package com.storeya.shop.controller;

import com.storeya.shop.dto.ProductDTO;
import com.storeya.shop.dto.response.PageResponse;

import com.storeya.shop.service.category.ICategoryService;
import com.storeya.shop.service.cloudinary.ICloudinaryService;
import com.storeya.shop.service.product.IProductService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final IProductService productService;
    private final ICloudinaryService cloudinaryService;

    public ProductController(IProductService productService, ICloudinaryService cloudinaryService) {
        this.productService = productService;
        this.cloudinaryService = cloudinaryService;


    }

    @GetMapping("/public/all")
    public ResponseEntity<List<ProductDTO>> findAll() {
        List<ProductDTO> products = productService.getAll();
        return ResponseEntity.ok(products);
    }

    // Lấy tất cả product
    @GetMapping()
    public ResponseEntity<PageResponse<ProductDTO>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ProductDTO> productPage = productService.findAll(page, size);

        PageResponse<ProductDTO> response = new PageResponse<>(
                productPage.getContent(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );

        return ResponseEntity.ok(response);
    }

    // Lấy product theo id
    @GetMapping("/public/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Tạo mới product
    @PostMapping
    public ResponseEntity<ProductDTO> create(@RequestBody ProductDTO dto) {
        ProductDTO saved = productService.create(dto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build(); // 204
    }

    @PostMapping("/cloudinary/delete")
    public ResponseEntity<?> deleteCloudinary(@RequestBody Map<String, String> body) throws IOException {
        String url = body.get("url");
        String publicId = cloudinaryService.getPublicIdFromUrl(url);
        cloudinaryService.deleteFile(publicId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> update(
            @PathVariable Long id,
            @RequestBody ProductDTO dto) {
        try {
            ProductDTO updated = productService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }


}
