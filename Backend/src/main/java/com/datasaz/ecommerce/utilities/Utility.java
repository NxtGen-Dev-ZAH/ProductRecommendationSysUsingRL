package com.datasaz.ecommerce.utilities;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class Utility {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

//    public static String generateJwtSecretKey() {
//        log.info("generateJwtSecretKey: generate JWT secret key");
//        // Generate a random 256-bit key
//        byte[] randomBytes = new byte[32]; // 256-bit key
//        new SecureRandom().nextBytes(randomBytes);
//        return Base64.getEncoder().encodeToString(randomBytes);
//    }


    public void validateEmail(String email) {
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid email format: {}", email);
            throw BadRequestException.builder().message("Invalid email format").build();
        }
    }

}
