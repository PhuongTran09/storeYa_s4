package com.storeya.shop.service.product;

import com.storeya.shop.dto.ProductDTO;
import com.storeya.shop.entity.Category;
import com.storeya.shop.entity.Product;
import com.storeya.shop.entity.ProductImage;
import com.storeya.shop.mapper.ProductMapper;
import com.storeya.shop.repository.ProductRepository;
import com.storeya.shop.repository.CategoryRepository;
import com.storeya.shop.service.cloudinary.ICloudinaryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductService implements IProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final ICloudinaryService cloudinaryService;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo, ICloudinaryService cloudinaryService) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.cloudinaryService = cloudinaryService;
    }

    @Override
    public List<ProductDTO> getAll() {
        try {
            return productRepo.findAll()
                    .stream()
                    .map(ProductMapper::toDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách sản phẩm: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Page<ProductDTO> findAll(int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            return productRepo.findAllByOrderByIdAsc(pageable)
                    .map(ProductMapper::toDTO);
        } catch (Exception e) {
            log.error("Lỗi khi phân trang sản phẩm: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Optional<ProductDTO> findById(Long id) {
        return productRepo.findById(id)
                .map(ProductMapper::toDTO)
                .or(() -> {
                    log.warn("Không tìm thấy sản phẩm với id: {}", id);
                    return Optional.empty();
                });
    }

    @Override
    public ProductDTO create(ProductDTO dto) {
        try {
            Product product = new Product();
            product.setName(dto.getName());
            product.setPrice(dto.getPrice());
            product.setDescription(dto.getDescription());
            product.setStock(dto.getStock() != null ? dto.getStock() : 0);

            // Gán category từ DTO nếu có
            if (dto.getCategory() != null && dto.getCategory().getId() != null) {
                Category cat = new Category();
                cat.setId(dto.getCategory().getId());
                product.setCategory(cat);
            }

            // Attach category từ DB (nếu cần check tồn tại)
            attachCategory(product);

            // Đảm bảo images list luôn khởi tạo
            if (product.getImages() == null) {
                product.setImages(new ArrayList<>());
            }

            // Gán image URLs thành ProductImage
            if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
                dto.getImageUrls().stream()
                        .filter(Objects::nonNull)         // loại bỏ null
                        .map(String::trim)                // loại bỏ khoảng trắng
                        .filter(url -> !url.isEmpty())    // loại bỏ rỗng
                        .forEach(url -> {
                            ProductImage img = new ProductImage();
                            img.setUrl(url);
                            img.setProduct(product);
                            product.getImages().add(img);
                        });
            }

            Product saved = productRepo.save(product);
            log.info("✅ Tạo sản phẩm thành công với id: {}", saved.getId());
            return ProductMapper.toDTO(saved);

        } catch (Exception e) {
            log.error("❌ Lỗi khi tạo sản phẩm: {}", e.getMessage(), e);
            throw e;
        }
    }


    @Override
    public ProductDTO update(Long id, ProductDTO product) {
        try {
            Product existing = productRepo.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với id = " + id));

            existing.setName(product.getName());
            existing.setDescription(product.getDescription());
            existing.setPrice(product.getPrice());
            existing.setStock(product.getStock());
            if (product.getCategory() != null & (product.getCategory() != null ? product.getCategory().getId() : null) != null) {
                Category category = categoryRepo.findById(product.getCategory().getId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Không tìm thấy category với id = " + product.getCategory().getId()
                        ));
                existing.setCategory(category);
            }


            if (product.getImageUrls() != null) {
                existing.getImages().clear();
                for (String url : product.getImageUrls()) {
                    if (url == null || url.isBlank()) continue;
                    ProductImage image = new ProductImage();
                    image.setUrl(url);
                    image.setProduct(existing);
                    existing.getImages().add(image);
                }
            }


            Product saved = productRepo.save(existing);
            log.info("Cập nhật sản phẩm thành công với id: {}", saved.getId());
            return ProductMapper.toDTO(saved);
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật sản phẩm id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void delete(Long id) {
        try {
            // Lấy product
            Product product = productRepo.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm với id = " + id));

            // Map sang DTO để lấy imageUrls
            ProductDTO dto = ProductMapper.toDTO(product);

            // Xóa ảnh trên Cloudinary
            if (dto.getImageUrls() != null) {
                for (String url : dto.getImageUrls()) {
                    try {
                        String publicId = cloudinaryService.getPublicIdFromUrl(url);
                        cloudinaryService.deleteFile(publicId);
                    } catch (Exception e) {
                        log.warn("Không xóa được ảnh trên Cloudinary: {}", url, e);
                    }
                }
            }

            // Xóa product
            productRepo.deleteById(id);
            log.info("Xóa sản phẩm thành công id: {}", id);

        } catch (Exception e) {
            log.error("Lỗi khi xóa sản phẩm id {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }


    private void attachCategory(Product product) {
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Category category = categoryRepo.findById(product.getCategory().getId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy category với id = " + product.getCategory().getId()));
            product.setCategory(category);
        }
    }
}
