package com.storeya.shop.service.auth;

import com.storeya.shop.entity.User;
import com.storeya.shop.repository.UserRepository;
import com.storeya.shop.security.RedisWindowCounter;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.modelmapper.ModelMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A 6-digit code with a 2-minute TTL is 10^6 combinations: unusable without an attempt
 * ceiling, and a free user-enumeration oracle without a uniform failure answer.
 */
class OtpServiceTest {

    /** Every rejection says exactly this, so callers cannot tell the causes apart. */
    private static final String GENERIC = "Mã OTP không hợp lệ hoặc đã hết hạn.";

    private static final String EMAIL = "user@example.com";
    private static final String OTP_KEY = "OTP:" + EMAIL;
    private static final String SEND_KEY = "OTP:SEND:" + EMAIL;
    private static final String ATTEMPTS_KEY = "OTP:ATT:" + EMAIL;

    private UserRepository userRepository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private JavaMailSender javaMailSender;
    private RedisWindowCounter windowCounter;
    private AuthService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        javaMailSender = mock(JavaMailSender.class);
        windowCounter = mock(RedisWindowCounter.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new AuthService(userRepository, new ModelMapper(), redisTemplate,
                javaMailSender, windowCounter);
        ReflectionTestUtils.setField(service, "otpSendLimit", 3);
        ReflectionTestUtils.setField(service, "otpMaxAttempts", 5);
    }

    @Test
    void anAddressWithNoAccountGetsNoMailAndNoError() {
        when(userRepository.findByEmailNormalized(EMAIL)).thenReturn(Optional.empty());

        service.sendOtp("  " + EMAIL.toUpperCase() + " ");

        verifyNoInteractions(javaMailSender);
        verifyNoInteractions(windowCounter);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void anExhaustedSendQuotaGetsNoMailAndNoError() {
        when(userRepository.findByEmailNormalized(EMAIL)).thenReturn(Optional.of(new User()));
        when(windowCounter.increment(eq(SEND_KEY), any(Duration.class))).thenReturn(4L);

        service.sendOtp(EMAIL);

        verifyNoInteractions(javaMailSender);
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void aFreshCodeIsSixDigitsAndClearsThePreviousAttemptCounter() throws Exception {
        accountExists();
        when(windowCounter.increment(eq(SEND_KEY), any(Duration.class))).thenReturn(1L);
        when(javaMailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        service.sendOtp(EMAIL);

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(OTP_KEY), code.capture(), eq(2L), eq(TimeUnit.MINUTES));
        assertEquals(6, code.getValue().length(), () -> "otp is not 6 digits: " + code.getValue());
        assertTrue(code.getValue().matches("\\d{6}"), () -> "otp is not numeric: " + code.getValue());
        verify(javaMailSender).send(any(MimeMessage.class));
        verify(windowCounter).reset(ATTEMPTS_KEY);
    }

    @Test
    void aMalformedCodeIsRefusedWithoutSpendingAnAttempt() {
        for (String candidate : new String[]{"12345", "1234567", "abcdef", "12345a", null, "123456 "}) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> service.resetPassword(EMAIL, candidate, "Newpass123!"),
                    () -> "accepted malformed otp: " + candidate);
            assertEquals(GENERIC, failure.getMessage());
        }
        verify(windowCounter, never()).increment(anyString(), any(Duration.class));
        verify(userRepository, never()).save(any());
    }

    /**
     * "no such account", "expired" and "wrong code" answer identically, so the endpoint
     * leaks nothing about which addresses are registered.
     */
    @Test
    void everyFailureModeAnswersIdentically() {
        accountExists();
        when(windowCounter.increment(eq(ATTEMPTS_KEY), any(Duration.class))).thenReturn(1L);

        String expired = messageOfReset("123456");

        when(valueOperations.get(OTP_KEY)).thenReturn("654321");
        String wrongCode = messageOfReset("123456");

        when(valueOperations.get(OTP_KEY)).thenReturn("123456");
        when(userRepository.findByEmailNormalized(EMAIL)).thenReturn(Optional.empty());
        String unknownAccount = messageOfReset("123456");

        assertEquals(GENERIC, expired);
        assertEquals(expired, wrongCode);
        assertEquals(wrongCode, unknownAccount);
        verify(userRepository, never()).save(any());
    }

    @Test
    void attemptsOverTheCeilingBurnTheCode() {
        accountExists();
        when(windowCounter.increment(eq(ATTEMPTS_KEY), any(Duration.class))).thenReturn(6L);

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> service.resetPassword(EMAIL, "123456", "Newpass123!"));

        assertEquals(GENERIC, failure.getMessage());
        verify(redisTemplate).delete(OTP_KEY);
        verify(valueOperations, never()).get(OTP_KEY);
        verify(userRepository, never()).save(any());
    }

    @Test
    void aCorrectCodeStoresAHashAndConsumesItself() {
        User account = new User();
        account.setId(1L);
        account.setEmail(EMAIL);
        account.setPassword("$2a$10$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        when(userRepository.findByEmailNormalized(EMAIL)).thenReturn(Optional.of(account));
        when(windowCounter.increment(eq(ATTEMPTS_KEY), any(Duration.class))).thenReturn(1L);
        when(valueOperations.get(OTP_KEY)).thenReturn("123456");

        service.resetPassword(EMAIL, "123456", "Newpass123!");

        assertNotEquals("Newpass123!", account.getPassword());
        assertTrue(account.getPassword().startsWith("$2"),
                () -> "password was stored unhashed: " + account.getPassword());
        assertFalse(account.getPassword().contains("Newpass123!"));
        verify(userRepository).save(account);
        verify(redisTemplate).delete(OTP_KEY);
        verify(windowCounter).reset(ATTEMPTS_KEY);
    }

    private String messageOfReset(String otp) {
        return assertThrows(RuntimeException.class,
                () -> service.resetPassword(EMAIL, otp, "Newpass123!")).getMessage();
    }

    private void accountExists() {
        when(userRepository.findByEmailNormalized(EMAIL)).thenReturn(Optional.of(new User()));
    }
}
