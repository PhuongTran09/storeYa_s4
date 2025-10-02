package com.storeya.shop.service.cloudinary;

import org.springframework.web.multipart.MultipartFile;

public interface ICloudinaryService {


    boolean deleteFile(String publicId);

    String getPublicIdFromUrl(String url);
}
