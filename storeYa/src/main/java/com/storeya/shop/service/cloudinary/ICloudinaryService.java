package com.storeya.shop.service.cloudinary;


public interface ICloudinaryService {


    boolean deleteFile(String publicId);

    String getPublicIdFromUrl(String url);
}
