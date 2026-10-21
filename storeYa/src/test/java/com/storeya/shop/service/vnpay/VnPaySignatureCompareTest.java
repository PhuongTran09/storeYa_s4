package com.storeya.shop.service.vnpay;

import com.storeya.shop.config.VnPayConfig;
import com.storeya.shop.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * handleVnPayIPN is the callback that moves money, and it is reachable without a token.
 * The signature is the only thing standing between a stranger and a PAID order, so the
 * accept / reject behaviour here is the security boundary.
 */
class VnPaySignatureCompareTest {

    private static final String SECRET = "unit-test-hash-secret-not-a-real-one";

    private VnPayService service;

    @BeforeEach
    void setUp() {
        VnPayConfig config = new VnPayConfig();
        ReflectionTestUtils.setField(config, "vnp_HashSecret", SECRET);
        ReflectionTestUtils.setField(config, "vnp_TmnCode", "TESTTMN");
        ReflectionTestUtils.setField(config, "vnp_Version", "2.1.0");
        ReflectionTestUtils.setField(config, "vnp_PayUrl", "http://localhost:1/vnpay/pay");
        ReflectionTestUtils.setField(config, "vnp_ReturnUrl", "http://localhost:4200/payments");
        ReflectionTestUtils.setField(config, "vnp_ApiUrl", "http://localhost:1/vnpay/api");
        service = new VnPayService(config);
    }

    @Test
    void aCorrectlySignedCallbackIsAccepted() {
        Map<String, String> params = orderParams();
        params.put("vnp_SecureHash", sign(params));

        Map<String, String> result = service.handleVnPayIPN(of(params));

        assertEquals("00", result.get("RspCode"), () -> "valid signature rejected: " + result);
    }

    /** The attack this guards: reuse a genuine signature on a different amount. */
    @Test
    void aTamperedAmountIsRejected() {
        Map<String, String> params = orderParams();
        String signature = sign(params);
        params.put("vnp_Amount", "9999999900");
        params.put("vnp_SecureHash", signature);

        Map<String, String> result = service.handleVnPayIPN(of(params));

        assertEquals("97", result.get("RspCode"), () -> "tampered callback was accepted: " + result);
        assertNotEquals("00", result.get("RspCode"));
    }

    @Test
    void aMissingOrForeignSignatureIsRejected() {
        Map<String, String> withoutSignature = orderParams();
        assertEquals("97", service.handleVnPayIPN(of(withoutSignature)).get("RspCode"));

        Map<String, String> forged = orderParams();
        forged.put("vnp_SecureHash", "0".repeat(128));
        assertEquals("97", service.handleVnPayIPN(of(forged)).get("RspCode"));
    }

    /**
     * The merchant secret must not be reconstructible from the config's toString, which
     * is what log.info("...{}", config) would print.
     */
    @Test
    void configToStringDoesNotCarryTheSecret() {
        VnPayConfig config = new VnPayConfig();
        ReflectionTestUtils.setField(config, "vnp_HashSecret", SECRET);
        ReflectionTestUtils.setField(config, "vnp_TmnCode", "TESTTMN");

        assertEquals("TESTTMN", config.getVnp_TmnCode());
        org.junit.jupiter.api.Assertions.assertFalse(config.toString().contains(SECRET),
                () -> "toString leaks the hash secret: " + config);
    }

    private static Map<String, String> orderParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_TxnRef", "42");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TmnCode", "TESTTMN");
        params.put("vnp_TransactionNo", "123456789");
        return params;
    }

    /** VNPay's canonical rule: sorted field=value pairs joined with &, values URL-encoded. */
    private static String sign(Map<String, String> params) {
        List<String> names = new ArrayList<>(params.keySet());
        Collections.sort(names);
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            if (i > 0) {
                hashData.append('&');
            }
            hashData.append(name).append('=')
                    .append(URLEncoder.encode(params.get(name), StandardCharsets.US_ASCII));
        }
        return VNPayUtil.hmacSHA512(SECRET, hashData.toString());
    }

    private static HttpServletRequest of(Map<String, String> params) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        params.forEach(request::addParameter);
        return request;
    }
}
