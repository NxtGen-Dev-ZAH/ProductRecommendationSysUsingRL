package com.datasaz.ecommerce.configs;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Set;

@Slf4j
@Configuration
public class GroupConfig {

    //private static final Tika tika = new Tika();


    @Value("${file.upload-dir}")
    public String UPLOAD_DIR;
//    public String getUploadDir() {
//        return UPLOAD_DIR;
//    }

    //public static final String UPLOAD_PROD_DIR = "uploads/product-pictures/";

    @Value("${image.storage-mode:database}") //  'file' for file-based storage, 'database' for database storage
    public String imageStorageMode;

    public static final Long PROD_IMG_DAYS_TO_KEEP = 5 * 365L; // 5 years default value to keep product images

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/vnd.wap.wbmp", "image/webp"
    );

    @Value("${image.max-size-mb:5}")
    public int maxFileSizeMb;
    public int MAX_FILE_SIZE;

    @Value("${image.max-count-per-product:10}")
    public int maxFileCountPerProduct;

    @Value("${image.resize-width:1600}")
    public int resizeWidth;

    @Value("${image.resize-height:1600}")
    public int resizeHeight;

    @Value("${image.thumbnail-resize-width:150}")
    public int thumbnailResizeWidth;

    @Value("${image.thumbnail-resize-height:150}")
    public int thumbnailResizeHeight;

    @Value("${image.quality:0.9}")
    public float imageQuality;


    public final int TOKEN_EXPIRATION_TIME = 60 * 60 * 24 * 365 * 1000; // 365 Days; 1-day = 86400000 milliseconds

    public static final BigDecimal CAT1_VAT_RATE = new BigDecimal("0.20"); // 20% VAT

    public static final int DEFAULT_MAX_PROFILE_IMAGE_SIZE = 1024 * 1024; // 1MB

    //public static final int DEFAULT_FILE_SIZE = 250 * 1024; // 250KB size in bytes
    public static final int DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    // public static final int DEFAULT_PROD_IMG_SIZE = 1024 * 1024;
    public static final int DEFAULT_MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    public final int DEFAULT_MAX_FILE_COUNT = 10;
    public final int DEFAULT_MAX_FILE_COUNT_PER_PRODUCT = 10;
    //private final int DEFAULT_MAX_FILE_SIZE_PER_PRODUCT = 1024 * 1024 * 10;

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    public static final String DEFAULT_FILE_EXTENSION = "bin";
    public static final String DEFAULT_FILE_NAME = "unknown";
    public static final String DEFAULT_FILE_NAME_WITH_EXTENSION = DEFAULT_FILE_NAME + "." + DEFAULT_FILE_EXTENSION;


    @PostConstruct
    public void init() {
        if (maxFileSizeMb <= 0) {
            maxFileSizeMb = 5;
            log.warn("Invalid maxFileSizeMb value detected ({}), using default: 5 MB", maxFileSizeMb);
        }
        MAX_FILE_SIZE = maxFileSizeMb * 1024 * 1024;
        if (imageQuality <= 0 || imageQuality > 1) {
            imageQuality = 0.9f;
            log.warn("Invalid imageQuality value detected, using default: 0.7");
        }
        if (!imageStorageMode.equals("file") && !imageStorageMode.equals("database")) {
            imageStorageMode = "database";
            log.warn("Invalid imageStorageMode ({}), defaulting to 'database'", imageStorageMode);
        }
        if (maxFileCountPerProduct <= 0) {
            maxFileCountPerProduct = 10;
            log.warn("Invalid maxFileCountPerProduct ({}), defaulting to 10", maxFileCountPerProduct);
        }
        if (resizeWidth <= 0) {
            resizeWidth = 1600;
            log.warn("Invalid resizeWidth ({}), defaulting to 1600", resizeWidth);
        }
        if (resizeHeight <= 0) {
            resizeHeight = 1600;
            log.warn("Invalid resizeHeight ({}), defaulting to 1600", resizeHeight);
        }
        if (thumbnailResizeWidth <= 0) {
            thumbnailResizeWidth = 150;
            log.warn("Invalid thumbnailResizeWidth ({}), defaulting to 150", thumbnailResizeWidth);
        }
        if (thumbnailResizeHeight <= 0) {
            thumbnailResizeHeight = 150;
            log.warn("Invalid thumbnailResizeHeight ({}), defaulting to 150", thumbnailResizeHeight);
        }
        log.info("GroupConfig initialized: imageStorageMode={}, maxFileSizeMb={}, MAX_FILE_SIZE={} bytes, maxFileCountPerProduct={}, resizeWidth={}, resizeHeight={}, thumbnailResizeWidth={}, thumbnailResizeHeight={}, imageQuality={}",
                imageStorageMode, maxFileSizeMb, MAX_FILE_SIZE, maxFileCountPerProduct, resizeWidth, resizeHeight, thumbnailResizeWidth, thumbnailResizeHeight, imageQuality);
    }

    // Getter for ALLOWED_IMAGE_TYPES to support Mockito stubbing
    public Set<String> getALLOWED_IMAGE_TYPES() {
        return ALLOWED_IMAGE_TYPES;
    }

}


//    public static boolean isAllowedImage(MultipartFile file, Tika tika) throws IOException {
//        try (InputStream is = file.getInputStream()) {
//            String mimeType = tika.detect(is);
//            return ALLOWED_IMAGE_TYPES.contains(mimeType);
//        }
//    }


//public static final Set<String> ALLOWED_FILE_TYPES = Set.of("application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

//    public static boolean isAllowedImage(MultipartFile file, Tika tika) throws IOException {
//        try (InputStream is = file.getInputStream()) {
//            String mimeType = tika.detect(is);
//            return Set.of("image/jpeg", "image/png", "image/gif").contains(mimeType);
//        }
//    }

//    public static boolean isAllowedImage(MultipartFile file) {
//        String contentType = file.getContentType();
//        return contentType != null && contentType.equals("image/jpeg");
//    }

//    public static boolean isAllowedImage(MultipartFile file) throws IOException {
//        byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
//        byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
//
//        byte[] header = new byte[4];
//        try (InputStream is = file.getInputStream()) {
//            is.read(header);
//        }
//
//        return Arrays.equals(header, 0, 4, PNG_MAGIC, 0, 4) ||
//                Arrays.equals(header, 0, 2, JPEG_MAGIC, 0, 2);
//    }

//    public static boolean isAllowedImage(MultipartFile file) throws IOException {
//        Tika tika = new Tika();
//        String mimeType = tika.detect(file.getInputStream());
//        return Set.of("image/jpeg", "image/png", "image/gif").contains(mimeType);
//    }