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
import java.util.Optional;

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
        cart.setTotalPrice(cart.getItems().stream()
                .mapToDouble(CartItem::getPrice)
                .sum());
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
        if (quantity == 0) throw new RuntimeException("Quantity must not be 0");

        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;
            if (newQuantity < 0) throw new RuntimeException("Quantity cannot be negative");
            else if (newQuantity == 0) {
                cart.getItems().remove(item);
                cartItemRepository.delete(item);
                product.setStock(product.getStock() + item.getQuantity());
            } else {
                if (product.getStock() < quantity)
                    throw new RuntimeException("Not enough stock for product: " + product.getName());
                item.setQuantity(newQuantity);
                item.setPrice(product.getPrice() * newQuantity);
                cartItemRepository.save(item);
                product.setStock(product.getStock() - quantity);
            }
        } else {
            if (quantity < 0) throw new RuntimeException("Quantity cannot be negative");
            if (product.getStock() < quantity)
                throw new RuntimeException("Not enough stock for product: " + product.getName());

            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setPrice(product.getPrice() * quantity);
            cart.getItems().add(item);
            cartItemRepository.save(item);

            product.setStock(product.getStock() - quantity);
        }

        productRepository.save(product);
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    public Cart getCart(Long userId) {
        return getOrCreateCart(userId);
    }

    @Override
    @Transactional
    public Cart updateItem(Long userId, Long productId, int quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());

        if (quantity <= 0) {
            cart.getItems().remove(item);
            cartItemRepository.delete(item);
        } else {
            if (product.getStock() < quantity)
                throw new RuntimeException("Not enough stock for product: " + product.getName());
            item.setQuantity(quantity);
            item.setPrice(product.getPrice() * quantity);
            cartItemRepository.save(item);
            product.setStock(product.getStock() - quantity);
        }

        productRepository.save(product);
        updateCartTotal(cart);
        return cartRepository.save(cart);
    }

    @Override
    @Transactional
    public Cart removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        Product product = item.getProduct();
        product.setStock(product.getStock() + item.getQuantity());
        productRepository.save(product);

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
