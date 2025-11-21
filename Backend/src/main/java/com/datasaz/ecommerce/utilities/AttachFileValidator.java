package com.datasaz.ecommerce.utilities;


import lombok.extern.slf4j.Slf4j;

import java.util.Base64;
import java.util.Set;

import static com.datasaz.ecommerce.models.dto.OrderFileContentType.*;

@Slf4j
public class AttachFileValidator {

    // del not used or bring all the validation methods here
    public static final String FILE_TYPE_NOT_SUPPORTED = "This file type is not supported.";
    public static final int MB_CONVERTOR = 1024; // in KB


    private AttachFileValidator() {
    }

    public static boolean isValidCdbId(Integer cdbId) {
        // this is equivalent to cws-user service call to validate company
        boolean validCdbId = true;
        if (cdbId == null) {
            validCdbId = false;
        }
        return validCdbId;
    }

    public static boolean isValidLogin(String login) {
        // discuss with Renuad how to validate login
        boolean validLogin = true;
        if (login.isEmpty()) {
            validLogin = false;
        }
        return validLogin;
    }

    public static boolean isValidFileSize(Long product, String fileContent, int allowedSize) {
        boolean validFileSize = true;
        byte[] decodedBytes = decodeBase64(fileContent);

        int fileSizeInBytes = decodedBytes.length;
        log.info("getting file size for product {}", product);
        // call the product config microservice to get the allowed file size for product type
        if (fileSizeInBytes > (allowedSize * MB_CONVERTOR)) {
            validFileSize = false;
        }
        return validFileSize;
    }

    public static Set<String> getAllMediaTypes() {
        return Set.of(
                GIF.getMediaType().toString(),
                JPEG.getMediaType().toString(),
                PNG.getMediaType().toString()
        );
    }

    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }
}
