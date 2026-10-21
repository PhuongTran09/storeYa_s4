package com.storeya.shop.service.vnpay;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import com.storeya.shop.config.VnPayConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The signed parameter map contains vnp_SecureHash, which is derived from the merchant
 * secret. Printing it sends a replayable signing oracle into log aggregation.
 *
 * vnp_ApiUrl is deliberately unparseable, so the refund call fails at URL construction
 * and the test never touches the network - the request log line it wants to inspect is
 * emitted before that point.
 */
class VnPayLogRedactionTest {

    private static final String SECRET = "unit-test-hash-secret-not-a-real-one";
    private static final Pattern LONG_HEX_RUN = Pattern.compile("(?i)[0-9a-f]{64,}");

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(VnPayService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    void refundRequestLogsAMaskedSignature() {
        service().refundPayment(new MockHttpServletRequest(), 150_000L,
                " hoan tien don #42", "42", "123456789", "20261021090000");

        List<ILoggingEvent> events = appender.list;
        assertFalse(events.isEmpty(), "nothing was logged, so this test proves nothing");

        String requestLog = events.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("Sending VNPay Refund Request"))
                .findFirst()
                .orElse("");
        assertTrue(requestLog.contains("Sending VNPay Refund Request"),
                () -> "the refund request was not logged at all: " + events);
        assertTrue(requestLog.contains("vnp_SecureHash=***"),
                () -> "signature is not masked: " + requestLog);

        for (ILoggingEvent event : events) {
            String message = event.getFormattedMessage();
            assertFalse(LONG_HEX_RUN.matcher(message).find(),
                    () -> "a signing oracle landed in the log: " + message);
            assertFalse(message.contains(SECRET), () -> "secret in log: " + message);
            assertFalse(textOf(event.getThrowableProxy()).contains(SECRET));
        }
    }

    private VnPayService service() {
        VnPayConfig config = new VnPayConfig();
        ReflectionTestUtils.setField(config, "vnp_HashSecret", SECRET);
        ReflectionTestUtils.setField(config, "vnp_TmnCode", "TESTTMN");
        ReflectionTestUtils.setField(config, "vnp_Version", "2.1.0");
        ReflectionTestUtils.setField(config, "vnp_PayUrl", "http://localhost:1/vnpay/pay");
        ReflectionTestUtils.setField(config, "vnp_ReturnUrl", "http://localhost:4200/payments");
        // Unparseable on purpose: fails before any socket is opened.
        ReflectionTestUtils.setField(config, "vnp_ApiUrl", "not-a-url");
        return new VnPayService(config);
    }

    private static String textOf(IThrowableProxy proxy) {
        return proxy == null ? "" : String.valueOf(proxy.getMessage());
    }
}
