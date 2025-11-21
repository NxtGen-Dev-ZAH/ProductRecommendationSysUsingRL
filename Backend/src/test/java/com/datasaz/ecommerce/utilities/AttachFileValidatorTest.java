package com.datasaz.ecommerce.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.util.Base64;

class AttachFileValidatorTest {

    private MockedStatic<Files> mockedFiles;

    @Test
    void testIsValidCdbId_validCdbId() {
        Integer cdbId = 123;
        boolean isValid = AttachFileValidator.isValidCdbId(cdbId);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testIsValidCdbId_invalidCdbId() {
        Integer cdbId = null;
        boolean isValid = AttachFileValidator.isValidCdbId(cdbId);
        Assertions.assertFalse(isValid);
    }

    @Test
    void testIsValidLogin_validLogin() {
        String login = "user";
        boolean isValid = AttachFileValidator.isValidLogin(login);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testIsValidLogin_invalidLogin() {
        String login = "";
        boolean isValid = AttachFileValidator.isValidLogin(login);
        Assertions.assertFalse(isValid);
    }

    @Test
    void testIsValidFileSize_validFileSize() {
        Long product = 1L;
        String fileContent = Base64.getEncoder().encodeToString("file content".getBytes());
        int allowedSize = 1024;
        boolean isValid = AttachFileValidator.isValidFileSize(product, fileContent, allowedSize);
        Assertions.assertTrue(isValid);
    }

    @Test
    void testIsValidFileSize_invalidFileSize() {
        Long product = 1L;
        String fileContent = Base64.getEncoder().encodeToString(new byte[22048]);
        int allowedSize = 10;
        boolean isValid = AttachFileValidator.isValidFileSize(product, fileContent, allowedSize);
        Assertions.assertFalse(isValid);
    }
}


