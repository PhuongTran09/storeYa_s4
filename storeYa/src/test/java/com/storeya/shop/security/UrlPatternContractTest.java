package com.storeya.shop.security;

import com.storeya.shop.controller.CategoryController;
import com.storeya.shop.controller.PayController;
import com.storeya.shop.controller.ProductController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tripwire for the class of bug that left /api/users unguarded: the chain matched
 * "/api/user/**", which does not match the real path "/api/users", so every user CRUD
 * call fell through to anyRequest().authenticated() and everything looked fine.
 *
 * Pure reflection: no Spring context, no database, so it runs wherever mvn test runs.
 */
class UrlPatternContractTest {

    /** What the chain should decide for a path. */
    enum Decision {
        ANONYMOUS,
        AUTHENTICATED,
        ADMIN
    }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /**
     * One entry per concrete controller path, path variables substituted. AUTHENTICATED
     * means "the chain stops at authenticated()"; admin-only mutations underneath it are
     * enforced by @PreAuthorize and checked by adminMutationsUseHasAuthorityNotHasRole.
     */
    private static final Map<String, Decision> EXPECTED = expected();

    private static Map<String, Decision> expected() {
        Map<String, Decision> map = new LinkedHashMap<>();
        map.put("/api/auth/login", Decision.ANONYMOUS);
        map.put("/api/auth/register", Decision.ANONYMOUS);
        map.put("/api/auth/forgot-password", Decision.ANONYMOUS);
        map.put("/api/auth/reset-password", Decision.ANONYMOUS);
        map.put("/api/auth/logout", Decision.ANONYMOUS);
        map.put("/api/auth/refresh", Decision.ANONYMOUS);
        map.put("/api/products/public/all", Decision.ANONYMOUS);
        map.put("/api/products/public/1", Decision.ANONYMOUS);
        map.put("/api/categories/public/all", Decision.ANONYMOUS);
        map.put("/api/payments/vnpay-ipn", Decision.ANONYMOUS);

        // The cart is keyed off the JWT principal. It used to be listed as public, which
        // turned an anonymous call into a null-userId lookup and a 500 instead of a 401.
        map.put("/api/carts", Decision.AUTHENTICATED);
        map.put("/api/carts/items", Decision.AUTHENTICATED);
        map.put("/api/carts/items/update/1", Decision.AUTHENTICATED);
        map.put("/api/carts/items/1", Decision.AUTHENTICATED);
        map.put("/api/payments", Decision.AUTHENTICATED);
        map.put("/api/payments/1/confirm", Decision.AUTHENTICATED);
        map.put("/api/payments/retry", Decision.AUTHENTICATED);
        map.put("/api/payments/cancel", Decision.AUTHENTICATED);

        // Content management: authenticated() at the chain level, @PreAuthorize below.
        map.put("/api/categories/get", Decision.AUTHENTICATED);
        map.put("/api/categories", Decision.AUTHENTICATED);
        map.put("/api/categories/1", Decision.AUTHENTICATED);
        map.put("/api/products", Decision.AUTHENTICATED);
        map.put("/api/products/1", Decision.AUTHENTICATED);
        map.put("/api/products/cloudinary/delete", Decision.AUTHENTICATED);

        // The regression that started this: user rows hold PII, and the email column
        // routes password-reset OTP, so editing one is account takeover.
        map.put("/api/users", Decision.ADMIN);
        map.put("/api/users/1", Decision.ADMIN);
        map.put("/api/users/me", Decision.AUTHENTICATED);
        return map;
    }

    @Test
    void everyControllerPathResolvesToTheIntendedDecision() throws Exception {
        Set<String> mapped = collectControllerPaths();
        List<String> problems = new ArrayList<>();

        for (String path : mapped) {
            Decision actual = decide(path);
            Decision wanted = EXPECTED.get(path);
            if (wanted == null) {
                problems.add(path + " is new: decide its role, then add it to EXPECTED");
            } else if (actual != wanted) {
                problems.add(path + " resolves to " + actual + " but must be " + wanted);
            }
        }
        for (String declared : EXPECTED.keySet()) {
            if (!mapped.contains(declared)) {
                problems.add(declared + " is declared here but no controller maps it any more");
            }
        }

        assertTrue(problems.isEmpty(), () -> System.lineSeparator() + String.join(System.lineSeparator(), problems));
    }

    /**
     * The rules must cover the plural prefix the controllers actually use, not a
     * singular near-miss - and /me must stay open to signed-in users even though
     * /api/users/* is admin-only, which is an ordering property, not a membership one.
     */
    @Test
    void userRecordsAreAdminOnlyWhileSelfServiceStaysOpen() {
        assertEquals(Decision.ADMIN, decide("/api/users"));
        assertEquals(Decision.ADMIN, decide("/api/users/7"));
        assertEquals(Decision.AUTHENTICATED, decide("/api/users/me"));
    }

    @Test
    void cartIsNotPublic() {
        for (String rule : SecurityConfig.PUBLIC_ENDPOINTS) {
            assertFalse(MATCHER.match(rule, "/api/carts"),
                    () -> "PUBLIC_ENDPOINTS entry " + rule + " exposes the cart");
            assertFalse(MATCHER.match(rule, "/api/carts/items"),
                    () -> "PUBLIC_ENDPOINTS entry " + rule + " exposes the cart");
        }
    }

    /**
     * Authority names carry no ROLE_ prefix (CustomAuthoritiesConverter emits the raw
     * realm role), so hasRole('admin') can never match: it locked COD confirm for
     * everyone, admins included.
     */
    @Test
    void adminMutationsUseHasAuthorityNotHasRole() {
        List<String> problems = new ArrayList<>();
        requireAdmin(ProductController.class, problems, "create", "delete", "update", "deleteCloudinary");
        requireAdmin(CategoryController.class, problems, "create", "update", "delete");
        requireAdmin(PayController.class, problems, "confirmPayment");
        assertTrue(problems.isEmpty(), () -> System.lineSeparator() + String.join(System.lineSeparator(), problems));
    }

    private static void requireAdmin(Class<?> controller, List<String> problems, String... names) {
        for (String name : names) {
            Method method = findMethod(controller, name);
            assertNotNull(method, controller.getSimpleName() + "." + name + " disappeared");
            PreAuthorize annotation =
                    AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
            String value = annotation == null ? null : annotation.value();
            if (value == null || !value.contains("hasAuthority('admin')")) {
                problems.add(controller.getSimpleName() + "." + name + " is guarded by " + value);
            }
        }
    }

    private static Method findMethod(Class<?> controller, String name) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    /** Applies the chain's rules in declaration order, the way authorizeHttpRequests does. */
    private static Decision decide(String path) {
        if (matches(SecurityConfig.PUBLIC_ENDPOINTS, path)) {
            return Decision.ANONYMOUS;
        }
        if (matches(SecurityConfig.SELF_ENDPOINTS, path)) {
            return Decision.AUTHENTICATED;
        }
        if (matches(SecurityConfig.ADMIN_ONLY_ENDPOINTS, path)) {
            return Decision.ADMIN;
        }
        return Decision.AUTHENTICATED;
    }

    private static boolean matches(String[] patterns, String path) {
        for (String pattern : patterns) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> collectControllerPaths() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        Set<String> paths = new LinkedHashSet<>();
        for (var candidate : scanner.findCandidateComponents("com.storeya.shop")) {
            Class<?> controller = Class.forName(((AnnotatedBeanDefinition) candidate).getBeanClassName());
            String[] base = pathsOf(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                for (String suffix : pathsOf(mapping)) {
                    paths.add(concretePath(join(base, suffix)));
                }
            }
        }
        assertTrue(!paths.isEmpty(), "no @RestController found - the scan is broken, not the config");
        return paths;
    }

    private static String[] pathsOf(RequestMapping mapping) {
        if (mapping == null) {
            return new String[]{""};
        }
        String[] paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return paths.length > 0 ? paths : new String[]{""};
    }

    /** Mirrors Spring's own rule: a method pattern without a leading slash gets one. */
    private static String join(String[] base, String suffix) {
        String prefix = base[0];
        if (suffix.isEmpty()) {
            return prefix;
        }
        if (prefix.isEmpty() || prefix.endsWith("/") || suffix.startsWith("/")) {
            return prefix + suffix;
        }
        return prefix + "/" + suffix;
    }

    /** {id} is opaque to the matcher, so hand it something to match against. */
    private static String concretePath(String pattern) {
        return pattern.replaceAll("\\{[^}]+}", "1");
    }
}
