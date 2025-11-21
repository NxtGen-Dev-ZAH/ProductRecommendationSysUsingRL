package com.datasaz.ecommerce.utilities;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class FileStorageServiceTest {
    // Stub implementation for testing
    public boolean exists(Path path) throws IOException {
        return false; // Simulate no existing file
    }

    public Path createDirectories(Path path) throws IOException {
        return path; // Simulate directory creation
    }

    public long copy(InputStream inputStream, Path target, StandardCopyOption option) throws IOException {
        return 1024L; // Simulate successful copy
    }

    public boolean deleteIfExists(Path path) throws IOException {
        return true; // Simulate successful deletion
    }
}