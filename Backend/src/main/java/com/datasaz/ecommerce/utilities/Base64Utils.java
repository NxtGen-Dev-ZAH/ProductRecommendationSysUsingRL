package com.datasaz.ecommerce.utilities;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class Base64Utils {
    private Base64Utils() {
    }

    public static void validateBase64Content(String base64Content) {
        if (base64Content == null || base64Content.isEmpty()) {
            throw BadRequestException.builder().message("Html content is required.").build();

        }

        try {
            Base64.getDecoder().decode(base64Content);
        } catch (IllegalArgumentException e) {
            log.error("Failed to validate Base64 content", e);
            throw BadRequestException.builder().message("Invalid Base64 content.").build();
        }
    }

    public static String decodeBase64ToHtml(String base64Content) {
        // Assuming validation happened before calling this method
        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }
}
