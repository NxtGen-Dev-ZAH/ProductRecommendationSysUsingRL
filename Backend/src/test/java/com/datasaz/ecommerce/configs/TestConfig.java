package com.datasaz.ecommerce.configs;

import com.datasaz.ecommerce.utilities.FileStorageServiceTest;
import org.apache.tika.Tika;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    @Bean
    public FileStorageServiceTest fileStorageService() { // Note: Return type is FileStorageServiceTest
        return new FileStorageServiceTest();
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }
}