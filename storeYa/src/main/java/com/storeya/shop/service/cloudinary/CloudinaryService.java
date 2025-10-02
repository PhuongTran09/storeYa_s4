package com.storeya.shop.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryService implements ICloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }
    @Override
    public boolean deleteFile(String publicId) {
        try {
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            return "ok".equals(result.get("result"));
        } catch (IOException e) {
            log.error("Xóa file lỗi: {}", e.getMessage(), e);
            return false;
        }
    }

    // Optional: Lấy publicId từ URL
    public String getPublicIdFromUrl(String url) {
        // https://res.cloudinary.com/<cloud_name>/image/upload/v<timestamp>/<folder>/<name>.jpg
        String[] parts = url.split("/");
        int len = parts.length;
        if(len < 2) return url;
        String fileNameWithExt = parts[len - 1]; // name.jpg
        String folder = parts[len - 2];          // folder
        return folder + "/" + fileNameWithExt.replace(".jpg", ""); // publicId
    }
}
