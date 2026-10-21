package com.storeya.shop.service.auth;

import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.entity.User;
import com.storeya.shop.repository.UserRepository;
import com.storeya.shop.security.RedisWindowCounter;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService implements IAuthService {

    private static final SecureRandom OTP_RANDOM = new SecureRandom();

    private static final Duration OTP_WINDOW = Duration.ofMinutes(15);

    @Value("${keycloak.auth-server-url}")
    private String authUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${security.rate-limit.otp-send-per-15min:3}")
    private int otpSendLimit;

    @Value("${security.rate-limit.otp-attempts:5}")
    private int otpMaxAttempts;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender javaMailSender;
    private final RedisWindowCounter windowCounter;

    public AuthService(UserRepository userRepository, ModelMapper modelMapper, StringRedisTemplate redisTemplate, JavaMailSender javaMailSender, RedisWindowCounter windowCounter) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;

        this.redisTemplate = redisTemplate;
        this.javaMailSender = javaMailSender;
        this.windowCounter = windowCounter;
    }

    @Override
    public void logout(String refreshToken) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/logout", authUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(url, entity, String.class);
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public TokenResponse login(LoginRequest request) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", authUrl, realm);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", request.getMail());
        form.add("password", request.getPassword());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> body = response.getBody();

            if (body == null || !body.containsKey("access_token")) {
                throw new RuntimeException("Keycloak response missing token info");
            }

            return new TokenResponse(
                    body.get("access_token").toString(),
                    body.get("refresh_token").toString(),
                    Long.parseLong(body.get("expires_in").toString()),
                    Long.parseLong(body.get("refresh_expires_in").toString())

            );
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public User registerUser(RegisterRequest request) {

        // Deliberately vague and identical for both cases: distinguishing "email taken"
        // from "username taken" would re-open the account enumeration that sendOtp closes.
        // The wording points an existing member at login instead.
        final String cannotRegister = "Không thể đăng ký với thông tin đã nhập. Nếu bạn đã có tài khoản, hãy đăng nhập.";

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, cannotRegister);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, cannotRegister);
        }
        String hash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        User user = modelMapper.map(request, User.class);
        user.setPassword(hash);
        userRepository.save(user);


        return user;
    }

    @Override
    public void sendOtp(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        // Unknown address and exhausted quota both return quietly: a distinguishable
        // error here makes this endpoint a user-enumeration oracle and a free mail bomb.
        if (userRepository.findByEmailNormalized(normalizedEmail).isEmpty()) {
            log.warn("OTP requested for an address with no account");
            return;
        }

        if (windowCounter.increment("OTP:SEND:" + normalizedEmail, OTP_WINDOW) > otpSendLimit) {
            log.warn("OTP send quota exhausted for this address");
            return;
        }

        try {
            String otp = String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
            redisTemplate.opsForValue().set("OTP:" + normalizedEmail, otp, 2, TimeUnit.MINUTES);
            windowCounter.reset("OTP:ATT:" + normalizedEmail);

            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Mã OTP đặt lại mật khẩu");
            String htmlMsg = """
                        <div style='font-family: Arial, sans-serif;'>
                            <h2 style='color:#007bff;'>Store Ya - Đặt lại mật khẩu</h2>
                            <p>Xin chào,</p>
                            <p>Mã OTP của bạn là:</p>
                            <div style='font-size: 20px; font-weight: bold; color: #ff4d4f;'>%s</div>
                            <p>Mã sẽ hết hạn sau <b>2 phút</b>.</p>
                            <p>Trân trọng,<br/>Store Ya Support</p>
                        </div>
                    """.formatted(otp);
            helper.setText(htmlMsg, true);

            helper.setFrom("noreply@storeya.com", "Store Ya Support");

            javaMailSender.send(message);
        } catch (Exception e) {
            // The SMTP cause stays in the log; the client only gets a generic line.
            log.error("OTP email delivery failed", e);
            throw new RuntimeException("Không thể gửi mã lúc này, vui lòng thử lại sau");
        }
    }


    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        // One message for every failure mode. Distinguishing "no such user" from
        // "expired" from "wrong code" would hand out a valid-account oracle.
        final String invalidOtp = "Mã OTP không hợp lệ hoặc đã hết hạn.";

        if (otp == null || !otp.matches("\\d{6}")) {
            throw new RuntimeException(invalidOtp);
        }

        // A 6-digit code with a 2-minute TTL is 10^6 combinations, so it is only safe
        // with a hard attempt ceiling. Over the ceiling the code is burned immediately.
        if (windowCounter.increment("OTP:ATT:" + normalizedEmail, OTP_WINDOW) > otpMaxAttempts) {
            redisTemplate.delete("OTP:" + normalizedEmail);
            throw new RuntimeException(invalidOtp);
        }

        String cachedOtp = redisTemplate.opsForValue().get("OTP:" + normalizedEmail);
        if (cachedOtp == null) {
            throw new RuntimeException(invalidOtp);
        }
        if (!MessageDigest.isEqual(cachedOtp.getBytes(StandardCharsets.UTF_8),
                otp.getBytes(StandardCharsets.UTF_8))) {
            throw new RuntimeException(invalidOtp);
        }

        Optional<User> userOpt = userRepository.findByEmailNormalized(normalizedEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException(invalidOtp);
        }
        User user = userOpt.get();

        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPassword(hash);
        userRepository.save(user);


        redisTemplate.delete("OTP:" + normalizedEmail);
        windowCounter.reset("OTP:ATT:" + normalizedEmail);
    }


    @Override
    public TokenResponse refreshToken(String refreshToken) {
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", authUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("access_token")) {
                throw new RuntimeException("Keycloak response missing token info");
            }


            long expiresIn = Long.parseLong(body.get("expires_in").toString());
            long refreshExpiresIn = Long.parseLong(body.get("refresh_expires_in").toString());

            return new TokenResponse(
                    body.get("access_token").toString(),
                    body.get("refresh_token").toString(),
                    expiresIn,
                    refreshExpiresIn
            );


        } catch (Exception e) {
            throw new RuntimeException("Refresh failed: " + e.getMessage());
        }
    }


    @Override
    public Long getUserIdFromToken(Jwt principal) {
        // sub không phải số => dùng preferred_username hoặc email
        String username = principal.getClaim("preferred_username");
        // tra DB ra userId theo username
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }


}
