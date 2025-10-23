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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.storeya.shop.utils.StringRandom.generateStringRandom;

@Slf4j
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
    public Map<String, Object> retryPayment(PaymentDTO dto, HttpServletRequest request) {
        Payment payment = paymentRepository.findBySetCode(dto.getSetCode())
                .orElseThrow(() -> new RuntimeException("Payment not found"));


        if (payment.getStatus() == PaymentStatus.PAID)
            throw new RuntimeException("Đơn hàng đã được thanh toán.");
        if (payment.getStatus() == PaymentStatus.CANCELLED)
            throw new RuntimeException("Đơn hàng đã bị hủy.");
        if (payment.getMethod() != PaymentMethod.VNPay)
            throw new RuntimeException("Đơn hàng không hỗ trợ thanh toán lại qua VNPay.");

        if (payment.getStatus() == PaymentStatus.REFUNDED || payment.getStatus() == PaymentStatus.SHIPPING)
            throw new RuntimeException("Đơn hàng không thể thanh toán lại ở trạng thái hiện tại.");


        String paymentUrl = vnPayService.createVnPayPayment(
                request,
                payment.getAmount().longValue(), "Thanh toán lại đơn hàng #" + dto.getSetCode(), String.valueOf(payment.getId()) // Lấy từ entity
        );


        Map<String, Object> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);
        response.put("paymentCode", dto.getSetCode());
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
    public PaymentDTO cancelPayment(PaymentDTO dto, Long userId, HttpServletRequest request) {
        Payment payment = paymentRepository.findBySetCode(dto.getSetCode())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // 1. Check Ownership
        if (!Objects.equals(payment.getUser() == null ? null : payment.getUser().getId(), userId)) {
            throw new SecurityException("User does not have permission to cancel this payment.");
        }

        PaymentStatus currentStatus = payment.getStatus();

        // 2. Nếu đã hủy hoặc đã hoàn tiền thì return luôn
        if (currentStatus == PaymentStatus.CANCELLED || currentStatus == PaymentStatus.REFUNDED) {
            log.warn("Attempted to cancel already cancelled/refunded payment SetCode: {}", dto.getSetCode());
            return paymentMapper.toDTO(payment);
        }

        // 3. VNPay refund
        if (payment.getMethod() == PaymentMethod.VNPay &&
                (currentStatus == PaymentStatus.PAID || currentStatus == PaymentStatus.WAITING_FOR_SHIPPING)) {

            log.info("Initiating VNPay refund for SetCode: {}", dto.getSetCode());
            try {
                String orderId = String.valueOf(payment.getId());
                String originalTransactionNo = payment.getTransactionId();
                LocalDateTime createdAt = payment.getCreatedAt();

                if (originalTransactionNo == null || createdAt == null) {
                    throw new IllegalStateException("Missing original transaction info for refund.");
                }

                String originalCreateDate = createdAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String refundOrderInfo = "Huy don hang " + payment.getSetCode();
                long amountToRefund = payment.getAmount().longValue() * 100; // VNPay expects cents

                Map<String, String> refundResponse = vnPayService.refundPayment(
                        request,
                        amountToRefund,
                        refundOrderInfo,
                        orderId,
                        originalTransactionNo,
                        originalCreateDate
                );

                log.info("VNPay refund response for SetCode {}: {}", payment.getSetCode(), refundResponse);

                String responseCode = refundResponse.getOrDefault("vnp_ResponseCode",
                        refundResponse.getOrDefault("vnp_responsecode", ""));
                log.info("VNPay repsoneseCode: {}", responseCode);
                if ("00".equals(responseCode)) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    payment.setDetails("Hoàn tiền VNPay thành công. TransactionNo: " +
                            refundResponse.getOrDefault("vnp_TransactionNo", "unknown"));
                    payment.setTransactionId(refundResponse.getOrDefault("vnp_TransactionNo", payment.getTransactionId()));
                    log.info("VNPay refund SUCCESS for SetCode: {}", payment.getSetCode());
                } else {
                    String errorMessage = "Hoàn tiền VNPay thất bại: " + refundResponse.getOrDefault("vnp_Message", "Unknown error");
                    payment.setDetails(errorMessage);
                    log.error("VNPay refund FAILED for SetCode: {}. Reason: {}", payment.getSetCode(), errorMessage);
                    throw new RuntimeException(errorMessage);
                }

            } catch (Exception e) {
                log.error("Error during VNPay refund process for SetCode: {}", payment.getSetCode(), e);
                throw new RuntimeException("Lỗi khi xử lý hoàn tiền VNPay: " + e.getMessage(), e);
            }

        } else {
            // 4. Hủy đơn bình thường
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setDetails("Đơn hàng đã được hủy.");
            log.info("Payment SetCode: {} cancelled (Non-VNPay).", payment.getSetCode());
        }

        // 5. Hoàn stock nếu cần
        try {
            restoreProductStock(payment);
        } catch (Exception e) {
            log.error("Error while restoring product stock for SetCode {}: {}", payment.getSetCode(), e.getMessage(), e);
            payment.setDetails((payment.getDetails() == null ? "" : payment.getDetails() + " | ") +
                    "Restore stock failed: " + e.getMessage());
        }

        payment.setUpdateAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        return paymentMapper.toDTO(saved);
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

    private void restoreProductStock(Payment payment) {
        if (payment.getItems() == null) return; // Kiểm tra nếu không có sản phẩm

        log.info("Restoring stock for Payment ID: {}", payment.getId()); // Thêm log

        for (OrderItem item : payment.getItems()) {
            Product product = item.getProduct();
            if (product != null) {
                // Lấy số lượng hiện tại
                int currentStock = product.getStock();
                // Lấy số lượng cần hoàn trả
                int quantityToRestore = item.getQuantity();

                // Cộng trả lại số lượng đã mua
                product.setStock(currentStock + quantityToRestore);
                productRepository.save(product); // Lưu lại thay đổi vào database

                log.info("  - Restored stock for Product ID {}: {} + {} = {}", product.getId(), currentStock, quantityToRestore, product.getStock()); // Log chi tiết
            } else {
                log.warn("  - Could not restore stock for OrderItem ID {} because Product was null.", item.getId()); // Cảnh báo nếu product null
            }
        }
    }
}
