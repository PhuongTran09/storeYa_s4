package com.storeya.shop.service.vnpay;

import com.storeya.shop.config.VnPayConfig;
import com.storeya.shop.utils.StringRandom;
import com.storeya.shop.utils.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.cloudinary.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
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
        Map<String, String> vnp_Params = new HashMap<>();
        Map<String, String> result = new HashMap<>();

        try {
            String vnp_RequestId = StringRandom.generateStringRandom(8);
            String vnp_CreateDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

            vnp_Params.put("vnp_RequestId", vnp_RequestId);
            vnp_Params.put("vnp_Version", config.getVnp_Version());
            vnp_Params.put("vnp_Command", "refund");
            vnp_Params.put("vnp_TmnCode", config.getVnp_TmnCode());
            vnp_Params.put("vnp_TransactionType", "02"); // Full refund
            vnp_Params.put("vnp_TxnRef", orderId);
            vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
            vnp_Params.put("vnp_OrderInfo", orderInfo);
            vnp_Params.put("vnp_TransactionNo", originalTransactionNo);
            vnp_Params.put("vnp_TransactionDate", originalTransactionDate); // Correct parameter name
            vnp_Params.put("vnp_CreateBy", "system");
            vnp_Params.put("vnp_CreateDate", vnp_CreateDate); // Refund request creation date
            vnp_Params.put("vnp_IpAddr", request.getRemoteAddr());

            // Build hashData
            List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
            Collections.sort(fieldNames);
            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnp_Params.get(fieldName);
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String vnp_SecureHash = VNPayUtil.hmacSHA512(config.getVnp_HashSecret(), hashData.toString());
            vnp_Params.put("vnp_SecureHash", vnp_SecureHash);

            log.info("Sending VNPay Refund Request: {}", vnp_Params);

            // Send request and get response
            String jsonBody = new JSONObject(vnp_Params).toString();
            StringBuilder responseString = getStringBuilder(jsonBody);
            log.info("Raw VNPay Refund Response: {}", responseString);

            // Parse JSON response using org.json
            if (responseString != null && responseString.length() > 0) {
                JSONObject jsonResponse = new JSONObject(responseString.toString());
                Iterator<String> keys = jsonResponse.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    Object value = jsonResponse.opt(key); // Use opt to handle potential nulls gracefully
                    result.put(key, value != null ? String.valueOf(value) : null);
                }
                log.info("Parsed VNPay Refund Response: {}", result);
                // Ensure consistent error keys if needed
                result.putIfAbsent("RspCode", result.get("vnp_ResponseCode"));
                result.putIfAbsent("Message", result.get("vnp_Message"));
            } else {
                log.error("Received empty response from VNPay refund API.");
                result.put("RspCode", "98");
                result.put("Message", "Empty response from VNPay API");
            }

        } catch (Exception e) {
            log.error("Refund VNPay error: {}", e.getMessage(), e);
            result.put("RspCode", "99");
            result.put("Message", "Lỗi hệ thống khi hoàn tiền VNPay: " + e.getMessage());
        }
        return result;
    }

    // Helper method to make the HTTP POST request
    private StringBuilder getStringBuilder(String jsonBody) throws IOException {
        URL url = new URL(config.getVnp_ApiUrl()); // Ensure vnp_ApiUrl points to the transaction API endpoint
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        StringBuilder response = new StringBuilder();
        // Use getErrorStream() if status code is not 2xx to read error details from VNPay
        int responseCode = conn.getResponseCode();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        } finally {
            conn.disconnect(); // Disconnect the connection
        }
        return response;
    }

}