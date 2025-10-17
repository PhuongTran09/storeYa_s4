package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.*;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.mapper.PaymentMapper;
import com.storeya.shop.repository.CartRepository;
import com.storeya.shop.repository.PaymentRepository;
import com.storeya.shop.repository.ProductRepository;
import com.storeya.shop.repository.UserRepository;
import com.storeya.shop.service.vnpay.IVnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.storeya.shop.utils.StringRandom.generateStringRandom;

@Service
@RequiredArgsConstructor
public class PayService implements IPayService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final IVnPayService vnPayService;

    @Override
    @Transactional
    public Map<String, Object> createPayment(PaymentDTO dto, HttpServletRequest request) {
        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow(() -> new RuntimeException("Cart not found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double total = cart.getItems().stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
        Payment payment = paymentMapper.toEntity(dto);
        payment.setUser(user);
        payment.setAmount(BigDecimal.valueOf(total));
        payment.setSetCode(generateCodeId());
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

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            product.setStock(cartItem.getQuantity());
            productRepository.save(product);
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setProductName(product.getName());
            orderItem.setPrice(cartItem.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPayment(payment);
            payment.getItems().add(orderItem);
        }

        Map<String, Object> response = new HashMap<>();

        if (dto.getMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaidAt(null);
            payment.setDetails("Thanh toán khi nhận hàng (COD)");
            Payment savedPayment = paymentRepository.save(payment);
            clearUserCart(user.getId());
            response.put("payment", paymentMapper.toDTO(savedPayment));
        } else if (dto.getMethod() == PaymentMethod.VNPay) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDetails("Chờ thanh toán qua cổng VNPay");
            Payment pendingPayment = paymentRepository.save(payment);
            String paymentUrl = vnPayService.createVnPayPayment(request, pendingPayment.getAmount().longValue(),
                    "Thanh toan don hang #" + pendingPayment.getSetCode(),String.valueOf(pendingPayment.getId()));
            response.put("paymentUrl", paymentUrl);
        }

        return response;
    }

    @Override
    @Transactional
    public void processVnPayIPN(Map<String, String> vnPayParams) {
        String orderIdStr = vnPayParams.get("vnp_TxnRef");
        String transactionIdFromVnPay = vnPayParams.get("vnp_TransactionNo");
        String responseCode = vnPayParams.get("vnp_ResponseCode");
        long amountFromVnPay = Long.parseLong(vnPayParams.get("vnp_Amount")) / 100;
        Payment payment = paymentRepository.findById(Long.parseLong(orderIdStr)).orElse(null);

        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return; // Payment not found or already processed
        }
        if (payment.getAmount().longValue() != amountFromVnPay) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setDetails("Payment failed: Amount mismatch.");

            paymentRepository.save(payment);
            return;
        }

        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setDetails("Thanh toán thành công qua VNPay");
            clearUserCart(payment.getUser().getId());
            payment.setTransactionId(transactionIdFromVnPay);
        } else {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setDetails("Thanh toán qua VNPay thất bại");

        }
        paymentRepository.save(payment);
    }

    @Override
    public PaymentDTO confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        return paymentMapper.toDTO(paymentRepository.save(payment));
    }

    @Override
    public PaymentDTO cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        if(!payment.getId().equals(payment.getUser().getId())) {
             throw new SecurityException("Dont have permission to cancel this Payment");
        }
        if(payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is not completed yet");
        }
        payment.setStatus(PaymentStatus.CANCELLED);
        return paymentMapper.toDTO(paymentRepository.save(payment));
    }

    @Override
    public List<PaymentDTO> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId)
                .stream()
                .map(paymentMapper::toDTO)
                .toList();
    }

    private void clearUserCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart != null) {
            cart.getItems().clear();
            cart.setTotalPrice(0.0);
            cartRepository.save(cart);
        }
    }

    private String generateCodeId() {
        String random = generateStringRandom(4);
        return "HD"+random;
    }
}