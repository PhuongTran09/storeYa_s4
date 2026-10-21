package com.storeya.shop.service.pay;

import com.storeya.shop.entity.Payment;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.PaymentRepository;
import com.storeya.shop.repository.ProductRepository;
import com.storeya.shop.repository.UserRepository;
import com.storeya.shop.service.vnpay.IVnPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * /api/payments/vnpay-ipn is permitAll, so its input is attacker-shaped. Reading
 * vnp_Amount and vnp_TxnRef straight into long used to answer a malformed callback with
 * a NumberFormatException and a 500 stack trace.
 */
class VnPayIpnGuardTest {

    private PaymentRepository paymentRepository;
    private PayService service;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        service = new PayService(paymentRepository, mock(UserRepository.class),
                mock(com.storeya.shop.mapper.PaymentMapper.class), mock(CartRepository.class),
                mock(ProductRepository.class), mock(IVnPayService.class));
    }

    @Test
    void aCallbackWithoutAnAmountIsIgnoredQuietly() {
        Map<String, String> params = baseIpn();
        params.remove("vnp_Amount");

        assertDoesNotThrow(() -> service.processVnPayIPN(params));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void aCallbackWithoutATxnRefIsIgnoredQuietly() {
        Map<String, String> params = baseIpn();
        params.remove("vnp_TxnRef");

        assertDoesNotThrow(() -> service.processVnPayIPN(params));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void aCallbackWithNonNumericFieldsIsIgnoredQuietly() {
        Map<String, String> params = baseIpn();
        params.put("vnp_Amount", "not-an-amount");

        assertDoesNotThrow(() -> service.processVnPayIPN(params));
        verifyNoInteractions(paymentRepository);

        Map<String, String> badRef = baseIpn();
        badRef.put("vnp_TxnRef", "drop table payments");
        assertDoesNotThrow(() -> service.processVnPayIPN(badRef));
        verifyNoInteractions(paymentRepository);
    }

    /** The amount check that stops a small payment being marked against a big order. */
    @Test
    void anAmountMismatchCancelsInsteadOfPaying() {
        Payment payment = new Payment();
        payment.setId(42L);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setMethod(PaymentMethod.VNPay);
        payment.setAmount(BigDecimal.valueOf(150_000));
        when(paymentRepository.findById(42L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Map<String, String> params = baseIpn();
        params.put("vnp_Amount", "1000");

        service.processVnPayIPN(params);

        verify(paymentRepository).save(payment);
        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        assertNotEquals(PaymentStatus.PAID, payment.getStatus());
    }

    /** An unknown order must not be marked paid by a callback naming it. */
    @Test
    void anUnknownOrderLeavesNothingToSave() {
        when(paymentRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.processVnPayIPN(baseIpn()));

        verify(paymentRepository).findById(42L);
        Mockito.verifyNoMoreInteractions(paymentRepository);
    }

    private static Map<String, String> baseIpn() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "42");
        params.put("vnp_Amount", "15000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "123456789");
        return params;
    }
}
