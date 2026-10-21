package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.Payment;
import com.storeya.shop.entity.User;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.mapper.PaymentMapper;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.PaymentRepository;
import com.storeya.shop.repository.ProductRepository;
import com.storeya.shop.repository.UserRepository;
import com.storeya.shop.service.vnpay.IVnPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * retryPayment used to trust the caller's setCode with no ownership check, so anyone
 * could mint a VNPay URL for somebody else's order - and the setCode space was only
 * "HD" + 4 characters, so guessing one was practical.
 */
class PayServiceRetryOwnershipTest {

    private static final String GENERIC = "Không tìm thấy đơn hàng phù hợp.";

    private PaymentRepository paymentRepository;
    private IVnPayService vnPayService;
    private PayService service;
    private final HttpServletRequest request = new MockHttpServletRequest();

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentMapper paymentMapper = mock(PaymentMapper.class);
        CartRepository cartRepository = mock(CartRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        vnPayService = mock(IVnPayService.class);
        service = new PayService(paymentRepository, userRepository, paymentMapper,
                cartRepository, productRepository, vnPayService);
    }

    @Test
    void unknownSetCodeIsRejectedWithoutSigningAnything() {
        when(paymentRepository.findBySetCode("HDNOPE")).thenReturn(Optional.empty());

        SecurityException failure = assertThrows(SecurityException.class,
                () -> service.retryPayment(dto(1L, "HDNOPE"), request));

        assertEquals(GENERIC, failure.getMessage());
        verifyNoInteractions(vnPayService);
    }

    @Test
    void someoneElsesOrderIsRejectedWithoutSigningAnything() {
        when(paymentRepository.findBySetCode("HDVICTIM"))
                .thenReturn(Optional.of(payment(100L, 99L, PaymentStatus.PENDING, PaymentMethod.VNPay)));

        SecurityException failure = assertThrows(SecurityException.class,
                () -> service.retryPayment(dto(1L, "HDVICTIM"), request));

        assertEquals(GENERIC, failure.getMessage());
        verifyNoInteractions(vnPayService);
    }

    /** "Not found" and "not yours" must be indistinguishable, or setCodes can be probed. */
    @Test
    void missingAndForeignOrdersGiveTheSameAnswer() {
        when(paymentRepository.findBySetCode("HDGHOST")).thenReturn(Optional.empty());
        when(paymentRepository.findBySetCode("HDOTHER"))
                .thenReturn(Optional.of(payment(101L, 99L, PaymentStatus.PENDING, PaymentMethod.VNPay)));

        String fromMissing = assertThrows(SecurityException.class,
                () -> service.retryPayment(dto(1L, "HDGHOST"), request)).getMessage();
        String fromForeign = assertThrows(SecurityException.class,
                () -> service.retryPayment(dto(1L, "HDOTHER"), request)).getMessage();

        assertEquals(fromMissing, fromForeign);
    }

    /** Positive control: the two rejections above are the guard, not a broken happy path. */
    @Test
    void ownersPendingOrderStillGetsAPaymentUrl() {
        when(paymentRepository.findBySetCode("HDMINE"))
                .thenReturn(Optional.of(payment(7L, 1L, PaymentStatus.PENDING, PaymentMethod.VNPay)));
        when(vnPayService.createVnPayPayment(eq(request), anyLong(), anyString(), anyString()))
                .thenReturn("https://sandbox.vnpayment.vn/pay?x=1");

        Map<String, Object> result = service.retryPayment(dto(1L, "HDMINE"), request);

        assertNotNull(result.get("paymentUrl"));
        assertEquals("HDMINE", result.get("paymentCode"));
        verify(vnPayService).createVnPayPayment(eq(request), anyLong(), anyString(), anyString());
    }

    @Test
    void aPaymentWithNoUserRowIsNotRetriableByAnyone() {
        Payment orphan = new Payment();
        orphan.setId(5L);
        orphan.setStatus(PaymentStatus.PENDING);
        orphan.setMethod(PaymentMethod.VNPay);
        orphan.setAmount(BigDecimal.valueOf(10000));
        when(paymentRepository.findBySetCode("HDORPHAN")).thenReturn(Optional.of(orphan));

        assertThrows(SecurityException.class, () -> service.retryPayment(dto(1L, "HDORPHAN"), request));
        verifyNoInteractions(vnPayService);
    }

    private static PaymentDTO dto(Long userId, String setCode) {
        PaymentDTO dto = new PaymentDTO();
        dto.setUserId(userId);
        dto.setSetCode(setCode);
        return dto;
    }

    private static Payment payment(Long id, Long ownerId, PaymentStatus status, PaymentMethod method) {
        User owner = new User();
        owner.setId(ownerId);
        Payment payment = new Payment();
        payment.setId(id);
        payment.setUser(owner);
        payment.setStatus(status);
        payment.setMethod(method);
        payment.setAmount(BigDecimal.valueOf(50000));
        return payment;
    }
}
