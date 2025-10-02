package com.storeya.shop.mapper;

import com.storeya.shop.dto.CategoryDTO;
import com.storeya.shop.dto.ProductDTO;
import com.storeya.shop.entity.Category;
import com.storeya.shop.entity.Product;
import com.storeya.shop.entity.ProductImage;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {

    // DTO -> Entity
    public static Product toEntity(ProductDTO dto) {
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setStock(dto.getStock());

        if (dto.getCategory() != null) {
            Category category = new Category();
            category.setId(dto.getCategory().getId());
            product.setCategory(category);
        }

        if (dto.getImageUrls() != null) {
            List<ProductImage> images = dto.getImageUrls().stream()
                    .map(url -> {
                        ProductImage img = new ProductImage();
                        img.setUrl(url);
                        img.setProduct(product);
                        return img;
                    })
                    .collect(Collectors.toList());
            product.setImages(images);
        }

        return product;
    }

    // Entity -> DTO
    public static ProductDTO toDTO(Product product) {
        CategoryDTO categoryDTO = null;
        if (product.getCategory() != null) {
            categoryDTO = new CategoryDTO(product.getCategory().getId(), product.getCategory().getName());
        }

        List<String> imageUrls = product.getImages() != null
                ? product.getImages().stream().map(ProductImage::getUrl).toList()
                : List.of();

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                imageUrls,
                categoryDTO
        );
    }
}