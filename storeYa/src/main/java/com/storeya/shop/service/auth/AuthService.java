package com.storeya.shop.service.auth;

import com.storeya.shop.dto.request.LoginRequest;
import com.storeya.shop.dto.request.RegisterRequest;
import com.storeya.shop.dto.response.TokenResponse;
import com.storeya.shop.entity.User;
import com.storeya.shop.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessToken;
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
import org.mindrot.jbcrypt.BCrypt;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AuthService implements IAuthService {

    @Value("${keycloak.auth-server-url}")
    private String authUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender javaMailSender;

    public AuthService(UserRepository userRepository, ModelMapper modelMapper, StringRedisTemplate redisTemplate, JavaMailSender javaMailSender) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;

        this.redisTemplate = redisTemplate;
        this.javaMailSender = javaMailSender;
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
                    Long.parseLong(body.get("expires_in").toString())

            );
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage());
        }
    }

    @Override
    public User registerUser(RegisterRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
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
        Optional<User> user = userRepository.findByEmailNormalized(normalizedEmail);

        if (user.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với email này");
        }

        try {
            String otp = String.format("%06d", new Random().nextInt(999999));
            redisTemplate.opsForValue().set("OTP:" + normalizedEmail, otp, 2, TimeUnit.MINUTES);

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
            throw new RuntimeException("Gửi email thất bại: " + e.getMessage());
        }
    }


    @Override
    public void resetPassword(String email, String otp, String newPassword) {
        String normalizedEmail = email.trim().toLowerCase();

        // Lấy OTP trong Redis
        String cachedOtp = redisTemplate.opsForValue().get("OTP:" + normalizedEmail);
        if (cachedOtp == null) {
            throw new RuntimeException("OTP đã hết hạn hoặc không tồn tại");
        }
        if (!cachedOtp.equals(otp)) {
            throw new RuntimeException("OTP không đúng");
        }

        Optional<User> userOpt = userRepository.findByEmailNormalized(normalizedEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy user với email này");
        }
        User user = userOpt.get();

        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        user.setPassword(hash);
        userRepository.save(user);


        redisTemplate.delete("OTP:" + normalizedEmail);
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

            return new TokenResponse(
                    body.get("access_token").toString(),
                    body.get("refresh_token").toString(),
                    Long.parseLong(body.get("expires_in").toString())
            );
        } catch (Exception e) {
            throw new RuntimeException("Refresh failed: " + e.getMessage());
        }


    }


}
