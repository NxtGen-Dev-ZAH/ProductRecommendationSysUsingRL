package com.datasaz.ecommerce.utilities;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
public class FileStorageService {
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public Path createDirectories(Path path) throws IOException {
        return Files.createDirectories(path);
    }

    public long copy(InputStream inputStream, Path target, StandardCopyOption... options) throws IOException {
        return Files.copy(inputStream, target, options);
    }

    public boolean deleteIfExists(Path path) throws IOException {
        return Files.deleteIfExists(path);
    }

    public void write(Path path, byte[] bytes) throws IOException {
        Files.write(path, bytes);
    }
    
}