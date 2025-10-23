package com.storeya.shop.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VNPayUtil {
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] bytes = hmac512.doFinal(data.getBytes());
            StringBuilder hash = new StringBuilder();
            for (byte b : bytes) {
                hash.append(String.format("%02x", b));
            }
            return hash.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error while calculating HMAC SHA512", e);
        }
    }
    public static String httpPost(String urlStr, Map<String, String> params) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() > 0) postData.append("&");
            postData.append(param.getKey()).append("=")
                    .append(java.net.URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8));
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(postData.toString().getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = reader.readLine()) != null) response.append(line);
        reader.close();

        return response.toString();
    }

    public static Map<String, String> parseResponse(String response) {
        Map<String, String> map = new HashMap<>();
        response = response.replace("{", "").replace("}", "").replace("\"", "");
        String[] pairs = response.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        // alias
        if (map.containsKey("vnp_ResponseCode")) {
            map.put("RspCode", map.get("vnp_ResponseCode"));
            map.put("Message", map.getOrDefault("vnp_Message", ""));
        }
        return map;
    }

}
