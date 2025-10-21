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
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        // Kiểm tra tồn kho, không trừ stock ở đây
        if (product.getStock() < quantity) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' không đủ hàng. Còn lại: " + product.getStock());
        }

        // Kiểm tra nếu sản phẩm đã có trong giỏ
        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem == null) {
            existingItem = new CartItem();
            existingItem.setCart(cart);
            existingItem.setProduct(product);
            existingItem.setPrice(product.getPrice());
            existingItem.setQuantity(quantity);
            cart.getItems().add(existingItem);
        } else {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
        }

        cartItemRepository.save(existingItem);
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Override
    @Transactional
    public Cart updateItem(Long userId, Long cartItemId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item with id " + cartItemId + " not found in cart"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            Product product = item.getProduct();
            if (product.getStock() < quantity) {
                throw new RuntimeException("Không đủ hàng. Còn lại: " + product.getStock());
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        updateCartTotal(cart);
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

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
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
