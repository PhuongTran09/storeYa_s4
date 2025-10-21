package com.storeya.shop.service.pay;

import com.storeya.shop.dto.PaymentDTO;
import com.storeya.shop.entity.*;
import com.storeya.shop.enums.PaymentMethod;
import com.storeya.shop.enums.PaymentStatus;
import com.storeya.shop.mapper.PaymentMapper;
import com.storeya.shop.repository.*;
import com.storeya.shop.service.vnpay.IVnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Payment existingPendingPayment = paymentRepository
                .findFirstByUserIdAndStatusAndMethod(user.getId(), PaymentStatus.PENDING, PaymentMethod.VNPay)
                .orElse(null);
        if (existingPendingPayment != null) {
            throw new RuntimeException("Bạn đang có đơn VNPay chưa thanh toán. Hãy thanh toán hoặc hủy trước khi tạo đơn mới.");
        }

        // Lấy giỏ hàng
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Giỏ hàng trống");
        }

        double total = cart.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        // Tạo Payment entity
        Payment payment = paymentMapper.toEntity(dto);
        payment.setUser(user);
        payment.setAmount(BigDecimal.valueOf(total));
        payment.setSetCode(generateCodeId());
        payment.setRecipientName(
                (dto.getRecipientName() != null && !dto.getRecipientName().isBlank())
                        ? dto.getRecipientName()
                        : user.getFirstName() + " " + user.getLastName()
        );
        payment.setRecipientPhone(
                (dto.getRecipientPhone() != null && !dto.getRecipientPhone().isBlank())
                        ? dto.getRecipientPhone()
                        : user.getPhone()
        );
        payment.setRecipientAddress(
                (dto.getRecipientAddress() != null && !dto.getRecipientAddress().isBlank())
                        ? dto.getRecipientAddress()
                        : user.getAddress()
        );
        payment.setRecipientEmail(
                (dto.getRecipientEmail() != null && !dto.getRecipientEmail().isBlank())
                        ? dto.getRecipientEmail()
                        : user.getEmail()
        );

        // 🧩 Gắn order items (chưa trừ stock tại đây)
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = getOrderItem(cartItem, payment);
            payment.getItems().add(orderItem);
        }

        Map<String, Object> response = new HashMap<>();

        //  COD: chỉ trừ stock sau khi xác nhận thanh toán (confirmPayment)
        if (dto.getMethod() == PaymentMethod.COD) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaidAt(null);
            payment.setDetails("Thanh toán khi nhận hàng (COD)");
            Payment savedPayment = paymentRepository.save(payment);
            clearUserCart(payment.getUser().getId());
            response.put("payment", paymentMapper.toDTO(savedPayment));
        }

        // 💳 VNPay: chưa trừ stock ở đây — chỉ trừ khi IPN confirm
        else if (dto.getMethod() == PaymentMethod.VNPay) {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setDetails("Chờ thanh toán qua VNPay");
            Payment pendingPayment = paymentRepository.save(payment);

            String paymentUrl = vnPayService.createVnPayPayment(
                    request,
                    pendingPayment.getAmount().longValue(),
                    "Thanh toán đơn hàng #" + pendingPayment.getSetCode(),
                    String.valueOf(pendingPayment.getId())
            );
            response.put("paymentUrl", paymentUrl);
        }

        return response;
    }

    private static OrderItem getOrderItem(CartItem cartItem, Payment payment) {
        Product product = cartItem.getProduct();
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setProductName(product.getName());
        orderItem.setPrice(cartItem.getPrice());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPayment(payment);
        return orderItem;
    }


    @Override
    @Transactional
    public void processVnPayIPN(Map<String, String> vnPayParams) {
        String orderIdStr = vnPayParams.get("vnp_TxnRef");
        String transactionId = vnPayParams.get("vnp_TransactionNo");
        String responseCode = vnPayParams.get("vnp_ResponseCode");
        long amountFromVnPay = Long.parseLong(vnPayParams.get("vnp_Amount")) / 100;

        Payment payment = paymentRepository.findById(Long.parseLong(orderIdStr))
                .orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }

        if (payment.getAmount().longValue() != amountFromVnPay) {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setDetails("VNPay amount mismatch.");
            paymentRepository.save(payment);
            return;
        }

        // Thanh toán thành công
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            payment.setTransactionId(transactionId);
            payment.setDetails("Thanh toán thành công qua VNPay");

            // Chỉ trừ stock tại đây — khi chắc chắn VNPay trả thành công
            for (OrderItem orderItem : payment.getItems()) {
                Product product = orderItem.getProduct();
                product.setStock(product.getStock() - orderItem.getQuantity());
                productRepository.save(product);
            }

            //  Clear cart sau khi thanh toán thành công
            clearUserCart(payment.getUser().getId());
        } else {
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setDetails("Thanh toán qua VNPay thất bại");
        }

        paymentRepository.save(payment);
    }


    @Override
    @Transactional
    public Map<String, Object> retryPayment(Long paymentId, HttpServletRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.PAID)
            throw new RuntimeException("Đơn hàng đã được thanh toán.");
        if (payment.getStatus() == PaymentStatus.CANCELLED)
            throw new RuntimeException("Đơn hàng đã bị hủy.");
        if (payment.getMethod() != PaymentMethod.VNPay)
            throw new RuntimeException("Đơn hàng không hỗ trợ thanh toán lại qua VNPay.");

        String paymentUrl = vnPayService.createVnPayPayment(
                request,
                payment.getAmount().longValue(),
                "Thanh toán lại đơn hàng #" + payment.getSetCode(),
                String.valueOf(payment.getId())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("paymentCode", payment.getSetCode());
        return response;
    }


    @Override
    @Transactional
    public PaymentDTO confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.PAID)
            throw new RuntimeException("Đơn hàng đã được thanh toán.");

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setDetails("Xác nhận thanh toán COD");

        // Trừ stock tại đây (COD)
        for (OrderItem orderItem : payment.getItems()) {
            Product product = orderItem.getProduct();
            product.setStock(product.getStock() - orderItem.getQuantity());
            productRepository.save(product);
        }

        clearUserCart(payment.getUser().getId());
        return paymentMapper.toDTO(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentDTO cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setDetails("Đơn hàng bị hủy bởi người dùng");
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
        return "HD" + generateStringRandom(4);
    }
}
