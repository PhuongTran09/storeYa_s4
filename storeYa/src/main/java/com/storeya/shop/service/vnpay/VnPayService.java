package com.storeya.shop.service.vnpay;

import com.storeya.shop.config.VnPayConfig;
import com.storeya.shop.utils.StringRandom;
import com.storeya.shop.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service

public class VnPayService implements IVnPayService {
    private final VnPayConfig config;

    public VnPayService(VnPayConfig config) {
        this.config = config;
    }

    @Override
    public String createVnPayPayment(HttpServletRequest request, long amount, String orderInfo, String orderId) {
        String vnp_IpAddr = request.getRemoteAddr();
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", config.getVnp_Version());
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", config.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", config.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnp_Params.put("vnp_CreateDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(config.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return config.getVnp_PayUrl() + "?" + queryUrl;
    }

    @Override
    public Map<String, String> handleVnPayIPN(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            Map<String, String> fields = getVnPayParamsAsMap(request);
            String vnp_SecureHash = request.getParameter("vnp_SecureHash");
            fields.remove("vnp_SecureHash");

            List<String> fieldNames = new ArrayList<>(fields.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = fields.get(fieldName);
                if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String calculatedHash = VNPayUtil.hmacSHA512(config.getVnp_HashSecret(), hashData.toString());

            if (calculatedHash.equals(vnp_SecureHash)) {
                response.put("RspCode", "00");
                response.put("Message", "Confirm Success");
            } else {
                response.put("RspCode", "97");
                response.put("Message", "Invalid Signature");
            }
        } catch (Exception e) {
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
        }
        return response;
    }

    @Override
    public Map<String, String> getVnPayParamsAsMap(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String fieldName = parameterNames.nextElement();
            String fieldValue = request.getParameter(fieldName);
            params.put(fieldName, fieldValue);
        }
        return params;
    }

    @Override
    public Map<String, String> refundPayment(HttpServletRequest request,
                                             long amount,
                                             String orderInfo,
                                             String orderId,
                                             String originalTransactionNo,
                                             String originalTransactionDate) {
        Map<String, String> result = new HashMap<>();
        try {
            log.info("Initiating VNPay refund for SetCode: {}", orderId);

            Map<String, String> vnp_Params = new HashMap<>();
            String vnp_RequestId = StringRandom.generateStringRandom(8);
            String vnp_Version = config.getVnp_Version();
            String vnp_Command = "refund";
            String vnp_TmnCode = config.getVnp_TmnCode();
            String vnp_TransactionType = "02";
            String vnp_Amount = String.valueOf(amount);
            String vnp_CreateBy = "system";
            String vnp_CreateDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String vnp_IpAddr = getIpAddress(request);

            vnp_Params.put("vnp_RequestId", vnp_RequestId);
            vnp_Params.put("vnp_Version", vnp_Version);
            vnp_Params.put("vnp_Command", vnp_Command);
            vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
            vnp_Params.put("vnp_TransactionType", vnp_TransactionType);
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_Amount", vnp_Amount);
            vnp_Params.put("vnp_TransactionNo", originalTransactionNo);
            vnp_Params.put("vnp_CreateBy", vnp_CreateBy);
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_TransactionDate", originalTransactionDate);
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
            vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

            // === Build hashData (not URL-encoded!) ===
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    if (!hashData.isEmpty()) {
                        hashData.append("&");
                        query.append("&");
                    }
                    hashData.append(fieldName).append("=").append(fieldValue); 
                    query.append(fieldName).append("=")
                            .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                }
            }

            String vnp_SecureHash = VNPayUtil.hmacSHA512(config.getVnp_HashSecret(), hashData.toString());
            vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

            log.info("=== VNPay Refund hashData ===\n{}", hashData);
            log.info("=== VNPay Refund SecureHash === {}", vnp_SecureHash);

            // === Gửi request ===
            String response = VNPayUtil.httpPost(config.getVnp_ApiUrl(), vnp_Params);
            log.info("=== VNPay Refund Raw Response ===\n{}", response);

            Map<String, String> responseData = VNPayUtil.parseResponse(response);
            log.info("=== VNPay Refund Parsed Response ===\n{}", responseData);

            result.putAll(responseData);
        } catch (Exception e) {
            log.error("Error during VNPay refund process for SetCode: {}", orderId, e);
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (ipAddress == null) ipAddress = request.getRemoteAddr();
        return ipAddress;
    }

}