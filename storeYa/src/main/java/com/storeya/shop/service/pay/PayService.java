package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.Cart;
import com.storeya.shop.entity.Payment;
import com.storeya.shop.entity.User;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.mapper.PaymentMapper;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.PaymentRepository;
import com.storeya.shop.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayService implements IPayService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final CartRepository cartRepository;

    @Override
    @Transactional
    public PaymentDTO createPayment(PaymentDTO dto) {
        // 🔹 Validate user
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔹 Lấy giỏ hàng của user
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // 🔹 Tính tổng tiền giỏ hàng
        double total = cart.getItems()
                .stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // 🔹 Tạo entity Payment
        Payment payment = paymentMapper.toEntity(dto);
        payment.setUser(user);
        payment.setAmount(BigDecimal.valueOf(total));
        payment.setPaidAt(LocalDateTime.now());

        payment.setRecipientName(
                dto.getRecipientName() != null && !dto.getRecipientName().isBlank()
                        ? dto.getRecipientName()
                        : user.getFirstName() + " " + user.getLastName()
        );
        payment.setRecipientPhone(
                dto.getRecipientPhone() != null && !dto.getRecipientPhone().isBlank()
                        ? dto.getRecipientPhone()
                        : user.getPhone()
        );
        payment.setRecipientAddress(
                dto.getRecipientAddress() != null && !dto.getRecipientAddress().isBlank()
                        ? dto.getRecipientAddress()
                        : user.getAddress()
        );
        payment.setRecipientEmail(
                dto.getRecipientEmail() != null && !dto.getRecipientEmail().isBlank()
                        ? dto.getRecipientEmail()
                        : user.getEmail()
        );

        // 🔹 Xử lý phương thức thanh toán
        if (dto.getMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING);
            if (dto.getDetails() != null && !dto.getDetails().isBlank()) {
                payment.setDetails(dto.getDetails());
            } else {
                payment.setDetails("Thanh toán khi nhận hàng (COD)");
            }
        } else {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            if (dto.getDetails() != null && !dto.getDetails().isBlank()) {
                payment.setDetails(dto.getDetails());
            } else {
                payment.setDetails("Thanh toán trực tuyến thành công");
            }

        }

        // 🔹 Lưu Payment
        Payment saved = paymentRepository.save(payment);

        // 🔹 Clear giỏ hàng sau khi tạo đơn
        cart.getItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        return paymentMapper.toDTO(saved);
    }


    @Override
    public PaymentDTO confirmPayment(Long paymentId, String transactionId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.valueOf(PaymentStatus.PAID.name()));
        payment.setPaidAt(LocalDateTime.now());
        return paymentMapper.toDTO(paymentRepository.save(payment));
    }

    @Override
    public PaymentDTO cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.valueOf(PaymentStatus.CANCELLED.name()));
        return paymentMapper.toDTO(paymentRepository.save(payment));
    }

    @Override
    public List<PaymentDTO> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toDTO)
                .toList();
    }

    @Override
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        return paymentMapper.toDTO(payment);
    }
}
