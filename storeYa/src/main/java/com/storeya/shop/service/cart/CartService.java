package com.storeya.shop.service.cart;

import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.CartItem;
import com.storeya.shop.entity.Product;
import com.storeya.shop.repository.CartItemRepository;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.ProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService implements ICartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(CartRepository cartRepository,
                       ProductRepository productRepository,
                       CartItemRepository cartItemRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    private void updateCartTotal(Cart cart) {
        double total = cart.getItems().stream()
                // NHÂN ĐƠN GIÁ VỚI SỐ LƯỢNG
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(total);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setTotalPrice(0.0);
            return cartRepository.save(cart);
        });
    }

    @Override
    @Transactional
    public Cart addToCart(Long userId, Long productId, int quantity) {
        if (quantity == 0) {
            throw new IllegalArgumentException("Quantity cannot be zero.");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        int oldQuantity = (existingItem != null) ? existingItem.getQuantity() : 0;
        int newQuantity = oldQuantity + quantity;

        if (newQuantity <= 0) {
            // Nếu số lượng mới <= 0, xóa sản phẩm khỏi giỏ
            if (existingItem != null) {
                product.setStock(product.getStock() + oldQuantity); // Hoàn trả lại kho
                cart.getItems().remove(existingItem);
                cartItemRepository.delete(existingItem);
            }
        } else {
            // Tính toán sự thay đổi số lượng cần lấy từ kho
            int quantityChange = newQuantity - oldQuantity;

            // Kiểm tra xem kho có đủ cho sự thay đổi này không
            if (product.getStock() < quantityChange) {
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            }

            if (existingItem == null) {
                existingItem = new CartItem();
                existingItem.setCart(cart);
                existingItem.setProduct(product);
                existingItem.setPrice(product.getPrice()); // Lưu đơn giá tại thời điểm thêm
                cart.getItems().add(existingItem);
            }

            existingItem.setQuantity(newQuantity);
            product.setStock(product.getStock() - quantityChange); // Chỉ trừ đi phần thay đổi

            cartItemRepository.save(existingItem);
        }

        productRepository.save(product);
        updateCartTotal(cart); // Gọi phương thức đã sửa lỗi
        return cartRepository.save(cart);
    }


    @Override
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Override
    @Transactional
    public Cart updateItem(Long userId, Long productId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        // Sửa lại logic tìm kiếm cho đúng
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product with id " + productId + " not found in cart"));

        Product product = item.getProduct();
        int oldQuantity = item.getQuantity();

        if (quantity <= 0) {
            // Nếu số lượng mới là 0 hoặc âm -> Xóa sản phẩm khỏi giỏ
            product.setStock(product.getStock() + oldQuantity); // Hoàn trả toàn bộ số lượng cũ vào kho
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            // Logic tồn kho mới, dựa trên sự thay đổi
            int quantityChange = quantity - oldQuantity;


            if (product.getStock() < quantityChange) {
                throw new RuntimeException("Not enough stock for product: " + product.getName() +
                        ". Available: " + product.getStock() + ", Required change: " + quantityChange);
            }

            // Cập nhật số lượng mới cho item
            item.setQuantity(quantity);
            // Cập nhật tồn kho dựa trên sự thay đổi
            product.setStock(product.getStock() - quantityChange);

            cartItemRepository.save(item);
        }

        productRepository.save(product);
        updateCartTotal(cart); // Gọi phương thức tính tổng tiền đã được sửa lỗi
        return cartRepository.save(cart);
    }


    @Override
    @Transactional
    public Cart removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (item == null) return cart;

        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    // 🟢 Xóa toàn bộ giỏ hàng
    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        cartItemRepository.deleteAll(cart.getItems());
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);
    }

    @Override
    public List<Cart> getAllCarts() {
        return cartRepository.findAll();
    }
}
