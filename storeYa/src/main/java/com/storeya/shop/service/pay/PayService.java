package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.Payment;
import com.storeya.shop.entity.User;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.mapper.PaymentMapper;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.PaymentRepository;
import com.storeya.shop.repository.UserRepository;
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
    public PaymentDTO createPayment(PaymentDTO dto) {
        // 🔹 Lấy thông tin user
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 🔹 Lấy giỏ hàng của user
        var cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // 🔹 Tính tổng tiền giỏ hàng
        double total = cart.getTotalPrice();

        // 🔹 Map sang entity Payment
        Payment payment = paymentMapper.toEntity(dto);
        payment.setUser(user);
        payment.setAmount(BigDecimal.valueOf(total));
        payment.setPaidAt(LocalDateTime.now());

        // 🔹 Gán thông tin người nhận từ user
        payment.setRecipientName(user.getFirstName() + " " + user.getLastName());
        payment.setRecipientPhone(user.getPhone());
        payment.setRecipientAddress(user.getAddress());
        payment.setRecipientEmail(user.getEmail());

        // 🔹 Nếu là COD
        if (dto.getMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDetails("Thanh toán khi nhận hàng (COD)");
        }

        // 🔹 Lưu Payment
        Payment saved = paymentRepository.save(payment);

        // 🔹 Clear giỏ hàng sau khi thanh toán
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
