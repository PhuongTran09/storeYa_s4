package com.storeya.shop.security;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.dto.UserDTO;
import com.storeya.shop.service.auth.IAuthService;
import com.storeya.shop.service.pay.IPayService;
import com.storeya.shop.service.user.IUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UrlPatternContractTest proves the rules are written correctly; this proves the running
 * chain enforces them, which is the half no static check can cover.
 *
 * BLOCKED unless STOREYA_IT=true with Postgres, Keycloak and Redis reachable: the context
 * needs a real DataSource (ddl-auto=update connects at startup) and the OIDC issuer.
 * The repo has no compose file, so those containers have to be supplied by hand.
 * Compilation is verified here; execution is not.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "STOREYA_IT", matches = "true")
class SecurityAuthorizationMatrixTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUserService userService;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private IPayService payService;

    /** The cart reads its user from the principal, so anonymous used to reach a null userId. */
    @Test
    void anonymousCartCallIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserListIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    /** The hole that was open: any signed-in user could dump, edit and delete accounts. */
    @Test
    void plainUserCannotReachAnotherAccount() throws Exception {
        when(authService.getUserIdFromToken(any())).thenReturn(1L);

        mockMvc.perform(get("/api/users").with(asUser("user")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/2").with(asUser("user"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/2").with(asUser("user")))
                .andExpect(status().isForbidden());
    }

    /** /api/users/* is admin-only, but /me has to keep working or the profile page dies. */
    @Test
    void plainUserCanStillReadTheirOwnProfile() throws Exception {
        when(authService.getUserIdFromToken(any())).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(new UserDTO());

        mockMvc.perform(get("/api/users/me").with(asUser("user")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanManageAccounts() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/users").with(asUser("admin")))
                .andExpect(status().isOk());
    }

    /**
     * hasRole('admin') never matched because realm roles arrive as bare authorities, so
     * confirming a cash order was closed to everybody, admins included.
     */
    @Test
    void onlyAdminCanConfirmACashOrder() throws Exception {
        when(payService.confirmPayment(anyLong())).thenReturn(new PaymentDTO());

        mockMvc.perform(post("/api/payments/1/confirm").with(asUser("user")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/payments/1/confirm").with(asUser("admin")))
                .andExpect(status().isOk());
    }

    /** Mirrors what CustomAuthoritiesConverter emits: the raw role name, no ROLE_ prefix. */
    private static org.springframework.test.web.servlet.request.RequestPostProcessor asUser(String role) {
        Jwt jwt = new Jwt("it-test-subject", Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(300), Map.of("typ", "Bearer"),
                Map.of("sub", "it-test-subject", "preferred_username", "tester"));
        return authentication(new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(role))));
    }
}
