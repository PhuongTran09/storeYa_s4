package com.storeya.shop.service.product;

import com.storeya.shop.dto.ProductDTO;
import com.storeya.shop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface IProductService {
    List<ProductDTO> getAll();
    Page<ProductDTO> findAll(int page, int size);
    Optional<ProductDTO> findById(Long id);
    ProductDTO create(ProductDTO dto);              // đồng bộ với service
    ProductDTO update(Long id, ProductDTO dto);     // cũng nên dùng DTO luôn
    void delete(Long id);
}
