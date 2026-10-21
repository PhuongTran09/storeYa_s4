package com.storeya.shop.controller;

import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.CartItem;
import com.storeya.shop.entity.Product;
import com.storeya.shop.entity.ProductImage;
import com.storeya.shop.mapper.CartMapper;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.cart.ICartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * removeItem answered with the Cart entity while the other four endpoints answered with
 * CartDTO. The entity drags the whole CartItem -> Product -> Category graph along, which
 * only serialises because spring.jpa.open-in-view still defaults to true, and it changes
 * the shape the frontend reads.
 */
class CartControllerShapeTest {

    private ICartService cartService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cartService = mock(ICartService.class);
        IAuthService authService = mock(IAuthService.class);
        when(authService.getUserIdFromToken(any())).thenReturn(1L);
        ModelMapper modelMapper = new ModelMapper();
        CartMapper cartMapper = new CartMapper(modelMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CartController(cartService, cartMapper, authService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(new Jwt("token", Instant.now(), Instant.now().plusSeconds(60),
                        Map.of("alg", "none"), Map.of("sub", "1"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getItemRemovalReturnsTheSameShapeAsGetCart() throws Exception {
        when(cartService.removeItem(anyLong(), anyLong())).thenReturn(cart());
        when(cartService.getCart(anyLong())).thenReturn(cart());

        String fromDelete = mockMvc.perform(delete("/api/carts/items/5"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fromGet = mockMvc.perform(get("/api/carts"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(fromDelete, fromGet,
                "DELETE /items/{id} and GET /carts must not answer with different shapes");
    }

    @Test
    void theCartAnswerCarriesItemFieldsNotProductGraphs() throws Exception {
        when(cartService.removeItem(anyLong(), anyLong())).thenReturn(cart());

        mockMvc.perform(delete("/api/carts/items/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(5))
                .andExpect(jsonPath("$.items[0].productId").value(77))
                .andExpect(jsonPath("$.items[0].productName").value("Áo thun"))
                .andExpect(jsonPath("$.items[0].price").value(150000.0))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].image").value("https://cdn.example/ao-thun.jpg"))
                // The entity form nested the full product here.
                .andExpect(jsonPath("$.items[0].product").doesNotExist())
                .andExpect(jsonPath("$.items[0].cart").doesNotExist());
    }

    /** The frontend's clearCart() now targets DELETE /api/carts, which exists. */
    @Test
    void clearingTheCartAnswersNoContentAndEmptiesIt() throws Exception {
        mockMvc.perform(delete("/api/carts"))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        verify(cartService).clearCart(1L);
    }

    private static Cart cart() {
        ProductImage image = new ProductImage();
        image.setId(9L);
        image.setUrl("https://cdn.example/ao-thun.jpg");

        Product product = new Product();
        product.setId(77L);
        product.setName("Áo thun");
        product.setPrice(150000.0);
        product.setStock(10);
        product.setImages(List.of(image));

        CartItem item = new CartItem();
        item.setId(5L);
        item.setQuantity(2);
        item.setPrice(150000.0);
        item.setProduct(product);

        Cart cart = new Cart();
        cart.setId(3L);
        cart.setUserId(1L);
        cart.setTotalPrice(300000.0);
        cart.setItems(List.of(item));
        item.setCart(cart);
        return cart;
    }
}
