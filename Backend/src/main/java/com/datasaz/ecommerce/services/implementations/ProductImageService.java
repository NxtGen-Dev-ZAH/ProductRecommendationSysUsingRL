package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageAttachRepository;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.repositories.entities.ProductImageAttach;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService implements IProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageAttachRepository productImageAttachRepository;
    private final GroupConfig groupConfig;
    private final Tika tika = new Tika();

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, ProductImageRequest imageRequest, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateImageRequest(imageRequest);

        String fileName = generateFileName(imageRequest.getFileName());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
            byte[] resizedImage = resizeImage(imageBytes, getFileExtension(imageRequest.getFileName()), false, groupConfig.resizeWidth, groupConfig.resizeHeight);
            if (resizedImage.length == 0) {
                log.error("Resized image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            Files.write(filePath, resizedImage);
            log.info("Saved image file: {}, size: {} bytes", filePath, resizedImage.length);

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(imageRequest.getContentType())
                    .fileSize(resizedImage.length)
                    .fileExtension(getFileExtension(imageRequest.getFileName()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(imageRequest.getDisplayOrder() != null ? imageRequest.getDisplayOrder() : 0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] resizedImage = resizeImage(file.getBytes(), getFileExtension(file.getOriginalFilename()), false, groupConfig.resizeWidth, groupConfig.resizeHeight);
            if (resizedImage.length == 0) {
                log.error("Resized image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            Files.write(filePath, resizedImage);
            log.info("Saved image file: {}, size: {} bytes", filePath, resizedImage.length);

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(resizedImage.length)
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImageAttach uploadImageAttach(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String extension = getFileExtension(file.getOriginalFilename());
        try {
            byte[] resizedImage = resizeImage(file.getBytes(), extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
            byte[] thumbnailImage = resizeImage(file.getBytes(), extension, false, groupConfig.thumbnailResizeWidth, groupConfig.thumbnailResizeHeight);
            if (resizedImage.length == 0 || thumbnailImage.length == 0) {
                log.error("Resized or thumbnail image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            if (resizedImage.length > groupConfig.MAX_FILE_SIZE || thumbnailImage.length > groupConfig.MAX_FILE_SIZE) {
                log.error("Image size {} bytes or thumbnail size {} bytes exceeds database limit of 5 MB", resizedImage.length, thumbnailImage.length);
                throw BadRequestException.builder().message("Image or thumbnail size exceeds " + groupConfig.maxFileSizeMb + " MB ( " + groupConfig.MAX_FILE_SIZE + " Bytes) limit").build();
            }

            ProductImageAttach image = ProductImageAttach.builder()
                    .fileName(fileName)
                    .contentType(file.getContentType())
                    .fileSize(resizedImage.length)
                    .fileExtension(extension)
                    .fileContent(resizedImage)
                    .thumbnailContent(thumbnailImage)
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageAttachRepository.save(existing);
                        });
            }

            return productImageAttachRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image attach {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImageAttach uploadImageAttach(Long productId, ProductImageRequest imageRequest, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateImageRequest(imageRequest);

        String fileName = generateFileName(imageRequest.getFileName());
        String extension = getFileExtension(imageRequest.getFileName());
        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
            byte[] resizedImage = resizeImage(imageBytes, extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
            byte[] thumbnailImage = resizeImage(imageBytes, extension, false, groupConfig.thumbnailResizeWidth, groupConfig.thumbnailResizeHeight);
            if (resizedImage.length == 0 || thumbnailImage.length == 0) {
                log.error("Resized or thumbnail image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            if (resizedImage.length > groupConfig.MAX_FILE_SIZE || thumbnailImage.length > groupConfig.MAX_FILE_SIZE) {
                log.error("Image size {} bytes or thumbnail size {} bytes exceeds database limit of 5 MB", resizedImage.length, thumbnailImage.length);
                throw BadRequestException.builder().message("Image or thumbnail size exceeds " + groupConfig.maxFileSizeMb + " MB ( " + groupConfig.MAX_FILE_SIZE + " Bytes) limit").build();
            }

            ProductImageAttach image = ProductImageAttach.builder()
                    .fileName(fileName)
                    .contentType(imageRequest.getContentType())
                    .fileSize(resizedImage.length)
                    .fileExtension(extension)
                    .fileContent(resizedImage)
                    .thumbnailContent(thumbnailImage)
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(imageRequest.getDisplayOrder() != null ? imageRequest.getDisplayOrder() : 0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageAttachRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageAttachRepository.save(existing);
                        });
            }

            return productImageAttachRepository.save(image);
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image: {}", fileName);
            throw BadRequestException.builder().message("Invalid Base64 content for image: " + fileName).build();
        } catch (IOException e) {
            log.error("Error while calling resizeImage() for : {}", fileName);
            throw TechnicalException.builder().message("Failed to resize image: " + e.getMessage()).build();
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#imageId")
    public void deleteImageById(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        try {
            Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
            Files.deleteIfExists(filePath);
            log.info("Deleted image file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
        }

        productImageRepository.delete(image);
        log.info("Deleted image from database: ID {}", imageId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public void deleteImagesByProductId(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        images.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                log.info("Deleted image file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
            }
            productImageRepository.delete(image);
            log.info("Deleted image from database: ID {}", image.getId());
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#imageId")
    public void deleteImageAttachById(Long imageId) {
        ProductImageAttach image = productImageAttachRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image attach not found with ID: " + imageId).build());
        productImageAttachRepository.delete(image);
        log.info("Deleted image attach from database: ID {}", imageId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public void deleteImageAttachesByProductId(Long productId) {
        List<ProductImageAttach> images = productImageAttachRepository.findByProductId(productId);
        images.forEach(image -> {
            productImageAttachRepository.delete(image);
            log.info("Deleted image attach from database: ID {}", image.getId());
        });
    }

    @Override
    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    @Override
    @Cacheable(value = "productImages", key = "#productId + '-attach'")
    public List<ProductImageAttach> getImageAttachesByProductId(Long productId) {
        return productImageAttachRepository.findByProductId(productId);
    }

    @Override
    @Transactional
    public void cleanupOldImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(groupConfig.PROD_IMG_DAYS_TO_KEEP);
        List<ProductImage> orphanedImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        orphanedImages.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                productImageRepository.delete(image);
                log.info("Deleted orphaned image: {}", image.getFileName());
            } catch (IOException e) {
                log.warn("Failed to delete orphaned image: {}: {}", image.getFileName(), e.getMessage());
            }
        });
        log.info("Cleaned up {} orphaned images older than {} days", orphanedImages.size(), groupConfig.PROD_IMG_DAYS_TO_KEEP);
    }

    @Override
    @Transactional
    public void cleanupOldImageAttaches() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(groupConfig.PROD_IMG_DAYS_TO_KEEP);
        List<ProductImageAttach> orphanedImages = productImageAttachRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        orphanedImages.forEach(image -> {
            productImageAttachRepository.delete(image);
            log.info("Deleted orphaned image attach: ID {}", image.getId());
        });
        log.info("Cleaned up {} orphaned image attaches older than {} days", orphanedImages.size(), groupConfig.PROD_IMG_DAYS_TO_KEEP);
    }

    private void validateImageRequest(ProductImageRequest imageRequest) {
        if (imageRequest == null || imageRequest.getFileContent() == null ||
                imageRequest.getFileName() == null || imageRequest.getFileName().isBlank() || imageRequest.getFileName().isEmpty()) {
            throw BadRequestException.builder().message("Image request is not complete").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image: {}", imageRequest.getFileName());
            throw BadRequestException.builder().message("Invalid Base64 content for image: " + imageRequest.getFileName()).build();
        }

        if (imageBytes.length > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", imageBytes.length, groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }

        String mimeType;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            mimeType = tika.detect(bais);
            if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
            bais.reset();
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Invalid image content: cannot read image for {}", imageRequest.getFileName());
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
            // Check colorspace
            if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                log.warn("Image has non-RGB colorspace: {} for {}", img.getColorModel().getColorSpace().getType(), imageRequest.getFileName());
                // RGB conversion will happen in resizeImage, so no need to fail here
            }
        } catch (IOException e) {
            log.error("Error detecting image type or validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.builder().message("File is empty or null").build();
        }
        if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            log.error("Unsupported file type: {}", file.getContentType());
            throw BadRequestException.builder().message("Unsupported file type: " + file.getContentType()).build();
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            log.error("File size {} bytes exceeds limit {} MB ({} bytes)", file.getSize(), groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("File size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes())) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Invalid image content: cannot read image for {}", file.getOriginalFilename());
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
            // Check colorspace
            if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                log.warn("Image has non-RGB colorspace: {} for {}", img.getColorModel().getColorSpace().getType(), file.getOriginalFilename());
                // RGB conversion will happen in resizeImage, so no need to fail here
            }
        } catch (IOException e) {
            log.error("Error validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    public byte[] resizeImage(byte[] originalImage, String extension, boolean forceJpeg, int targetWidth, int targetHeight) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Failed to read image: null BufferedImage");
                throw new IOException("Cannot read image: invalid or corrupt image data");
            }
            log.debug("Original image: width={}, height={}", img.getWidth(), img.getHeight());

            double aspectRatio = (double) img.getWidth() / img.getHeight();
            if (aspectRatio > 1) {
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetWidth = (int) (targetHeight * aspectRatio);
            }
            BufferedImage resized = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
            log.debug("Resized image: width={}, height={}", resized.getWidth(), resized.getHeight());

            // Convert to RGB colorspace if forcing JPEG or output format is JPEG
            BufferedImage rgbImage = resized;
            String outputFormat = forceJpeg ? "jpg" : (extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg");
            String formatName;
            switch (outputFormat) {
                case "jpg":
                case "jpeg":
                    formatName = "jpeg";
                    // Convert to RGB to avoid colorspace issues
                    rgbImage = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImage.getGraphics().drawImage(resized, 0, 0, null);
                    break;
                case "png":
                    formatName = "png";
                    break;
                case "gif":
                    formatName = "gif";
                    break;
                case "bmp":
                    formatName = "bmp";
                    break;
                case "tiff":
                    formatName = "tiff";
                    break;
                case "wbmp":
                    formatName = "wbmp";
                    break;
                case "webp":
                    formatName = "webp";
                    break;
                default:
                    log.warn("Unsupported output format: {}, defaulting to jpeg", outputFormat);
                    formatName = "jpeg";
                    outputFormat = "jpg";
                    // Convert to RGB for default JPEG
                    rgbImage = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_INT_RGB);
                    rgbImage.getGraphics().drawImage(resized, 0, 0, null);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (formatName.equals("png") || formatName.equals("gif") || formatName.equals("bmp") || formatName.equals("tiff") || formatName.equals("wbmp")) {
                    ImageIO.write(rgbImage, formatName, baos);
                } else {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
                    if (!writers.hasNext()) {
                        log.error("No writer available for format: {}", formatName);
                        throw new IOException("No writer available for format: " + formatName);
                    }
                    ImageWriter writer = writers.next();
                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                        writer.setOutput(ios);
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (formatName.equals("jpeg") || formatName.equals("webp")) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(groupConfig.imageQuality);
                        }
                        writer.write(null, new javax.imageio.IIOImage(rgbImage, null, null), param);
                    } finally {
                        writer.dispose();
                    }
                }
                byte[] result = baos.toByteArray();
                log.debug("Resized image size: {} bytes, format: {}", result.length, formatName);
                if (result.length == 0) {
                    log.error("Resized image is empty after writing to ByteArrayOutputStream");
                    throw new IOException("Resized image is empty");
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error resizing image: {}", e.getMessage(), e);
            throw new IOException("Failed to resize image: " + e.getMessage(), e);
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}

/*
import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService implements IProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final GroupConfig groupConfig;
    private final Tika tika = new Tika();

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, ProductImageRequest imageRequest, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateImageRequest(imageRequest);

        String fileName = generateFileName(imageRequest.getFileName());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
            byte[] resizedImage = resizeImage(imageBytes, getFileExtension(imageRequest.getFileName()));
            if (resizedImage.length == 0) {
                log.error("Resized image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            Files.write(filePath, resizedImage);
            log.info("Saved image file: {}, size: {} bytes", filePath, resizedImage.length);

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(imageRequest.getContentType())
                    .fileSize(resizedImage.length) // Use resized size
                    .fileExtension(getFileExtension(imageRequest.getFileName()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(imageRequest.getDisplayOrder() != null ? imageRequest.getDisplayOrder() : 0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] resizedImage = resizeImage(file.getBytes(), getFileExtension(file.getOriginalFilename()));
            if (resizedImage.length == 0) {
                log.error("Resized image is empty for file: {}", fileName);
                throw TechnicalException.builder().message("Failed to resize image: empty result").build();
            }
            Files.write(filePath, resizedImage);
            log.info("Saved image file: {}, size: {} bytes", filePath, resizedImage.length);

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(resizedImage.length) // Use resized size
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#image.product.id")
    public void deleteImageById(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        try {
            Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
            Files.deleteIfExists(filePath);
            log.info("Deleted image file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
        }

        productImageRepository.delete(image);
        log.info("Deleted image from database: ID {}", imageId);
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public void deleteImagesByProductId(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        images.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                log.info("Deleted image file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
            }
            productImageRepository.delete(image);
            log.info("Deleted image from database: ID {}", image.getId());
        });
    }

    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    @Transactional
    public void cleanupOldImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(groupConfig.PROD_IMG_DAYS_TO_KEEP);
        List<ProductImage> orphanedImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        orphanedImages.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                productImageRepository.delete(image);
                log.info("Deleted orphaned image: {}", image.getFileName());
            } catch (IOException e) {
                log.warn("Failed to delete orphaned image: {}: {}", image.getFileName(), e.getMessage());
            }
        });
        log.info("Cleaned up {} orphaned images older than {} days", orphanedImages.size(), groupConfig.PROD_IMG_DAYS_TO_KEEP);
    }

    private void validateImageRequest(ProductImageRequest imageRequest) {
        if (imageRequest == null || imageRequest.getFileContent() == null || imageRequest.getFileContent().isEmpty()) {
            throw BadRequestException.builder().message("Image content is empty or null").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image: {}", imageRequest.getFileName());
            throw BadRequestException.builder().message("Invalid Base64 content for image: " + imageRequest.getFileName()).build();
        }

        if (imageBytes.length > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", imageBytes.length, groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }

        String mimeType;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            mimeType = tika.detect(bais);
            if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
            bais.reset();
            if (ImageIO.read(bais) == null) {
                log.error("Invalid image content: cannot read image for {}", imageRequest.getFileName());
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type or validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.builder().message("File is empty or null").build();
        }
        if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            log.error("Unsupported file type: {}", file.getContentType());
            throw BadRequestException.builder().message("Unsupported file type: " + file.getContentType()).build();
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            log.error("File size {} bytes exceeds limit {} MB ({} bytes)", file.getSize(), groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("File size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes())) {
            if (ImageIO.read(bais) == null) {
                log.error("Invalid image content: cannot read image for {}", file.getOriginalFilename());
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
        } catch (IOException e) {
            log.error("Error validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private byte[] resizeImage(byte[] originalImage, String extension) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Failed to read image: null BufferedImage");
                throw new IOException("Cannot read image: invalid or corrupt image data");
            }
            log.debug("Original image: width={}, height={}", img.getWidth(), img.getHeight());

            // Preserve aspect ratio by fitting to max dimension
            int targetWidth = groupConfig.resizeWidth;
            int targetHeight = groupConfig.resizeHeight;
            double aspectRatio = (double) img.getWidth() / img.getHeight();
            if (aspectRatio > 1) {
                targetHeight = (int) (targetWidth / aspectRatio);
            } else {
                targetWidth = (int) (targetHeight * aspectRatio);
            }
            BufferedImage resized = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
            log.debug("Resized image: width={}, height={}", resized.getWidth(), resized.getHeight());

            String outputFormat = extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg";
            // Map file extensions to ImageIO format names
            String formatName;
            switch (outputFormat) {
                case "jpg":
                case "jpeg":
                    formatName = "jpeg";
                    break;
                case "png":
                    formatName = "png";
                    break;
                case "gif":
                    formatName = "gif";
                    break;
                case "bmp":
                    formatName = "bmp";
                    break;
                case "tiff":
                    formatName = "tiff";
                    break;
                case "wbmp":
                    formatName = "wbmp";
                    break;
                case "webp":
                    formatName = "webp";
                    break;
                default:
                    log.warn("Unsupported output format: {}, defaulting to jpeg", outputFormat);
                    formatName = "jpeg";
                    outputFormat = "jpg";
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                if (formatName.equals("png") || formatName.equals("gif") || formatName.equals("bmp") || formatName.equals("tiff") || formatName.equals("wbmp")) {
                    ImageIO.write(resized, formatName, baos);
                } else {
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
                    if (!writers.hasNext()) {
                        log.error("No writer available for format: {}", formatName);
                        throw new IOException("No writer available for format: " + formatName);
                    }
                    ImageWriter writer = writers.next();
                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                        writer.setOutput(ios);
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        if (formatName.equals("jpeg") || formatName.equals("webp")) {
                            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                            param.setCompressionQuality(groupConfig.imageQuality);
                        }
                        writer.write(null, new javax.imageio.IIOImage(resized, null, null), param);
                    } finally {
                        writer.dispose();
                    }
                }
                byte[] result = baos.toByteArray();
                log.debug("Resized image size: {} bytes, format: {}", result.length, formatName);
                if (result.length == 0) {
                    log.error("Resized image is empty after writing to ByteArrayOutputStream");
                    throw new IOException("Resized image is empty");
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Error resizing image: {}", e.getMessage(), e);
            throw new IOException("Failed to resize image: " + e.getMessage(), e);
        }
    }


    private void saveThumbnail(byte[] originalImage, String fileName, int width, int height) throws IOException {
        //resize for thumbnail witdth and height
        //byte[] thumbnail = resizeImage(originalImage, getFileExtension(fileName), width, height);
        Path thumbnailPath = Path.of(groupConfig.UPLOAD_DIR, "products", "thumbnails", fileName);
        Files.createDirectories(thumbnailPath.getParent());
        //Files.write(thumbnailPath, thumbnail);
        //log.info("Saved thumbnail: {}, size: {} bytes", thumbnailPath, thumbnail.length);
    }



    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}

*/

//    private byte[] resizeImage(byte[] originalImage, String extension) throws IOException {
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage)) {
//            BufferedImage img = ImageIO.read(bais);
//            if (img == null) {
//                log.error("Failed to read image: null BufferedImage");
//                throw new IOException("Cannot read image: invalid or corrupt image data");
//            }
//            log.debug("Original image: width={}, height={}", img.getWidth(), img.getHeight());
//
//            // Preserve aspect ratio by fitting to max dimension
//            int targetWidth = groupConfig.resizeWidth;
//            int targetHeight = groupConfig.resizeHeight;
//            double aspectRatio = (double) img.getWidth() / img.getHeight();
//            if (aspectRatio > 1) {
//                targetHeight = (int) (targetWidth / aspectRatio);
//            } else {
//                targetWidth = (int) (targetHeight * aspectRatio);
//            }
//            BufferedImage resized = Scalr.resize(img, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_EXACT, targetWidth, targetHeight);
//            log.debug("Resized image: width={}, height={}", resized.getWidth(), resized.getHeight());
//
//            String outputFormat = extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg";
//            if (!outputFormat.equals("jpg") && !outputFormat.equals("jpeg") && !outputFormat.equals("png")) {
//                log.warn("Unsupported output format: {}, defaulting to jpg", outputFormat);
//                outputFormat = "jpg";
//            }
//
//            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//                if (outputFormat.equals("png")) {
//                    ImageIO.write(resized, "png", baos);
//                } else {
//                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
//                    if (!writers.hasNext()) {
//                        throw new IOException("No JPEG writer available");
//                    }
//                    ImageWriter writer = writers.next();
//                    try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
//                        writer.setOutput(ios);
//                        ImageWriteParam param = writer.getDefaultWriteParam();
//                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//                        param.setCompressionQuality(groupConfig.imageQuality);
//                        writer.write(null, new javax.imageio.IIOImage(resized, null, null), param);
//                    } finally {
//                        writer.dispose();
//                    }
//                }
//                byte[] result = baos.toByteArray();
//                log.debug("Resized image size: {} bytes", result.length);
//                if (result.length == 0) {
//                    log.error("Resized image is empty after writing to ByteArrayOutputStream");
//                    throw new IOException("Resized image is empty");
//                }
//                return result;
//            }
//        } catch (Exception e) {
//            log.error("Error resizing image: {}", e.getMessage(), e);
//            throw new IOException("Failed to resize image: " + e.getMessage(), e);
//        }
//    }

//    private byte[] resizeImage(byte[] originalImage, String extension) throws IOException {
//        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalImage)) {
//            BufferedImage img = ImageIO.read(bais);
//            if (img == null) {
//                log.error("Failed to read image: null BufferedImage");
//                throw new IOException("Cannot read image: invalid or corrupt image data");
//            }
//            log.debug("Original image: width={}, height={}", img.getWidth(), img.getHeight());
//
//            BufferedImage resized = Scalr.resize(img, Scalr.Method.QUALITY, groupConfig.resizeWidth, groupConfig.resizeHeight);
//            log.debug("Resized image: width={}, height={}", resized.getWidth(), resized.getHeight());
//
//            String outputFormat = extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg";
//            if (!outputFormat.equals("jpg") && !outputFormat.equals("jpeg") && !outputFormat.equals("png")) {
//                log.warn("Unsupported output format: {}, defaulting to jpg", outputFormat);
//                outputFormat = "jpg";
//            }
//
//            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
//                ImageIO.write(resized, outputFormat, baos);
//                byte[] result = baos.toByteArray();
//                log.debug("Resized image size: {} bytes", result.length);
//                if (result.length == 0) {
//                    log.error("Resized image is empty after writing to ByteArrayOutputStream");
//                    throw new IOException("Resized image is empty");
//                }
//                return result;
//            }
//        } catch (Exception e) {
//            log.error("Error resizing image: {}", e.getMessage(), e);
//            throw new IOException("Failed to resize image: " + e.getMessage(), e);
//        }
//    }


/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService implements IProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final GroupConfig groupConfig;
    private final Tika tika = new Tika();

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, ProductImageRequest imageRequest, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateImageRequest(imageRequest);

        String fileName = generateFileName(imageRequest.getFileName());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
//            byte[] resizedImage = resizeImage(imageBytes);
//            Files.write(filePath, resizedImage);
            Files.write(filePath, imageBytes);

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(imageRequest.getContentType())
                    .fileSize(imageBytes.length)
                    .fileExtension(getFileExtension(imageRequest.getFileName()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(imageRequest.getDisplayOrder() != null ? imageRequest.getDisplayOrder() : 0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Product not found with ID: " + productId).build());

        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "products");
        Path filePath = uploadDir.resolve(fileName);

        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            byte[] resizedImage = resizeImage(file.getBytes());
            //Files.write(filePath, resizedImage);
            Files.write(filePath, file.getBytes());

            ProductImage image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl("/Uploads/products/" + fileName)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .fileExtension(getFileExtension(file.getOriginalFilename()))
                    .createdAt(LocalDateTime.now())
                    .isPrimary(isPrimary)
                    .displayOrder(0)
                    .product(product)
                    .build();

            if (isPrimary) {
                productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                        .ifPresent(existing -> {
                            existing.setPrimary(false);
                            productImageRepository.save(existing);
                        });
            }

            return productImageRepository.save(image);
        } catch (IOException e) {
            log.error("Failed to upload image {} for product {}: {}", fileName, productId, e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#image.product.id")
    public void deleteImageById(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> ResourceNotFoundException.builder().message("Image not found with ID: " + imageId).build());

        try {
            Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
            Files.deleteIfExists(filePath);
            log.info("Deleted image file: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
        }

        productImageRepository.delete(image);
        log.info("Deleted image from database: ID {}", imageId);
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public void deleteImagesByProductId(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductId(productId);
        images.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                log.info("Deleted image file: {}", filePath);
            } catch (IOException e) {
                log.warn("Failed to delete image file {}: {}", image.getFileUrl(), e.getMessage());
            }
            productImageRepository.delete(image);
            log.info("Deleted image from database: ID {}", image.getId());
        });
    }

    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    @Transactional
    public void cleanupOldImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(groupConfig.PROD_IMG_DAYS_TO_KEEP);
        List<ProductImage> orphanedImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        orphanedImages.forEach(image -> {
            try {
                Path filePath = Path.of(groupConfig.UPLOAD_DIR, "products", image.getFileName());
                Files.deleteIfExists(filePath);
                productImageRepository.delete(image);
                log.info("Deleted orphaned image: {}", image.getFileName());
            } catch (IOException e) {
                log.warn("Failed to delete orphaned image: {}: {}", image.getFileName(), e.getMessage());
            }
        });
        log.info("Cleaned up {} orphaned images older than {} days", orphanedImages.size(), groupConfig.PROD_IMG_DAYS_TO_KEEP);
    }

    private void validateImageRequest(ProductImageRequest imageRequest) {
        if (imageRequest == null || imageRequest.getFileContent() == null || imageRequest.getFileContent().isEmpty()) {
            throw BadRequestException.builder().message("Image content is empty or null").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(imageRequest.getFileContent());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image: {}", imageRequest.getFileName());
            throw BadRequestException.builder().message("Invalid Base64 content for image: " + imageRequest.getFileName()).build();
        }

        if (imageBytes.length > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", imageBytes.length, groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }

        String mimeType;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            mimeType = tika.detect(bais);
            if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type: {}", e.getMessage());
            throw TechnicalException.builder().message("Error detecting image type: " + e.getMessage()).build();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.builder().message("File is empty or null").build();
        }
        if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            log.error("Unsupported file type: {}", file.getContentType());
            throw BadRequestException.builder().message("Unsupported file type: " + file.getContentType()).build();
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", file.getSize(), groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("File size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }
    }

    private byte[] resizeImage(byte[] originalImage) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalImage));
        BufferedImage resized = Scalr.resize(img, Scalr.Method.QUALITY, groupConfig.resizeWidth, groupConfig.resizeHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        return baos.toByteArray();
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}

*/


//--------7/08/2025-------//


/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.request.ProductImageRequest;
import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.services.interfaces.IProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.datasaz.ecommerce.utilities.AttachFileValidator.FILE_TYPE_NOT_SUPPORTED;
import static com.datasaz.ecommerce.utilities.AttachFileValidator.getAllMediaTypes;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageService implements IProductImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final Tika tika = new Tika();
    private final GroupConfig groupConfig;
//    private final S3Client s3Client;

    //private final AmazonS3 amazonS3;

    //@Value("${spring.s3.bucket}")


  //  @Value("${aws.s3.bucket}")
//    private String bucketName;

//    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
//            "image/jpeg", "image/png", "image/gif"
//    );

//    @Transactional
//    @CacheEvict(value = "productImages", key = "#productId")
//    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) { //throws Exception
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//
//        validateFile(file);
//
//        ProductImage image = null;
//        try {
//            // Upload to S3
//            //amazonS3.putObject(new PutObjectRequest(bucketName, fileName, tempFile));
//
//            // Resize image
//            byte[] resizedImage = resizeImage(file.getBytes());
//
//            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
//            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(fileName)
//                    .contentType(file.getContentType())
//                    .build();
//            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(resizedImage));
//
//            image = ProductImage.builder()
//                    .fileName(fileName)
//                    .fileUrl(String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName))
//                    .contentType(file.getContentType())
//                    .isPrimary(isPrimary)
//                    .product(product)
//                    .build();
//
//        } catch (Exception e) {
//            log.error("IO Exception: {}", e.getMessage());
//            throw new RestClientException("IO Exception: " + e.getMessage());
//        }
//
//        if (isPrimary) {
//            productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
//                    .ifPresent(existing -> {
//                        existing.setPrimary(false);
//                        productImageRepository.save(existing);
//                    });
//        }
//
//        return productImageRepository.save(image);
//    }
//
//    @Transactional
//    @CacheEvict(value = "productImages", key = "#result.product.id")
//    public void deleteImage(Long imageId) {
//        ProductImage image = productImageRepository.findById(imageId)
//                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
//
//        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
//                .bucket(bucketName)
//                .key(image.getFileName())
//                .build();
//        s3Client.deleteObject(deleteObjectRequest);
//
//        productImageRepository.delete(image);
//    }

    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

//    @Transactional
//    //@Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
//    public void cleanupOldImages() {
//        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
//        List<ProductImage> orphanedImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
//        orphanedImages.forEach(image -> {
//            try {
//                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
//                        .bucket(bucketName)
//                        .key(image.getFileName())
//                        .build();
//                s3Client.deleteObject(deleteObjectRequest);
//                productImageRepository.delete(image);
//                log.info("Deleted orphaned image: {}", image.getFileName());
//            } catch (Exception e) {
//                log.error("Failed to delete orphaned image: {}", image.getFileName(), e);
//            }
//        });
//    }

    // change to throw IllegalArgumentException
    // and file identity to alert the problem for particular file instead of interrupting the whole process
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!groupConfig.ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds " + groupConfig.maxFileSizeMb + "MB");
        }
    }


    private byte[] resizeImage(byte[] originalImage) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalImage));
        BufferedImage resized = Scalr.resize(img, Scalr.Method.QUALITY, groupConfig.resizeWidth, groupConfig.resizeHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        return baos.toByteArray();
    }



//    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {   //throws IOException
//        String fileName = UUID.randomUUID()+ "_"+ LocalDateTime.now() + "_" + file.getOriginalFilename();
//        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
//                .bucket(bucketName)
//                .key(fileName)
//                .contentType(file.getContentType())
//                .build();
//
//        try {
//            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
//
//        }
//        catch (Exception e) {
//            log.error("IO Exception: {}", e.getMessage());
//        }
//
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//
//        ProductImage image = ProductImage.builder()
//                .fileName(fileName)
//                .fileUrl(String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName))
//                .contentType(file.getContentType())
//                .isPrimary(isPrimary)
//                .product(product)
//                .build();
//
//        return productImageRepository.save(image);
//    }


//    @Transactional
//    @Override
//    @CacheEvict(value = "productImages", key = "#productId")
//    @CachePut(value = "productImages", key = "#result.id")
//    @Cacheable(value = "productImages", key = "#productId")
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#productId, 'Product', 'UPLOAD_IMAGE')")
//    @AuditLog(action = AuditLog.Action.UPLOAD_IMAGE, resourceType = AuditLog.ResourceType.PRODUCT, resourceId = "#productId")
//    @Transactional(propagation = Propagation.REQUIRES_NEW) // To avoid race condition when saving product images
//    @SuppressWarnings("squid:S2095") // Suppressing "Resources should be closed" warning as the S3Client closes the stream automatically


    //Local Image upload suppressed by the above methode for S3 upload
    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        validateFile(file);

        String fileName = UUID.randomUUID() + "_" + LocalDateTime.now() + "_" + file.getOriginalFilename();

        Path uploadPath = Paths.get(groupConfig.UPLOAD_DIR + "/" + fileName);


        Path filePath = Paths.get(uploadPath.toString(), fileName);

        // Create upload directory if it doesn't exist
        try {
            if (!Files.exists(uploadPath)) {
                //Files.createDirectories(uploadPath);
                Files.createDirectories(filePath.getParent());
            }

            // Resize image
            byte[] resizedImage = resizeImage(file.getBytes(), 800, 800); // Max 800x800
            Files.write(filePath, resizedImage);

        } catch (IOException e) {
            log.error("IO Exception: {}", e.getMessage());
        }
        ProductImage image = ProductImage.builder()
                .fileName(fileName)
                .fileUrl("/" + uploadPath + "/" + fileName)
                .contentType(file.getContentType())
                .isPrimary(isPrimary)
                .product(product)
                .build();

        // If setting as primary, unset others
        if (isPrimary) {
            productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        productImageRepository.save(existing);
                    });
        }

        return productImageRepository.save(image);
    }

    // doesn't comply for the S3 upload
    public void deleteImageById(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        try {
            Files.deleteIfExists(Paths.get(image.getFileUrl().substring(1)));
        } catch (IOException e) {
            log.error("Failed to delete image file: {}", image.getFileUrl(), e);
        }

        productImageRepository.delete(image);
    }

    @Transactional
    public void deleteImagesByProductId(Long productId) {
        List<ProductImage> oldImages = productImageRepository.findByProductId(productId);
        oldImages.forEach(image -> {
            try {
                Files.deleteIfExists(Paths.get(image.getFileUrl().substring(1)));
                productImageRepository.delete(image);
                log.info("Deleted old image: {}", image.getFileName());
            } catch (IOException e) {
                log.error("Failed to delete old image: {}", image.getFileName(), e);
            }
        });
    }

    // doesn't comply for the S3 upload'
    //@Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    @Transactional
    public void cleanupOldImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<ProductImage> oldImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        oldImages.forEach(image -> {
            try {
                Files.deleteIfExists(Paths.get(image.getFileUrl().substring(1)));
                productImageRepository.delete(image);
                log.info("Deleted old image: {}", image.getFileName());
            } catch (IOException e) {
                log.error("Failed to delete old image: {}", image.getFileName(), e);
            }
        });
    }

    private byte[] resizeImage(byte[] originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalImage));
        BufferedImage resized = Scalr.resize(img, Scalr.Method.QUALITY, targetWidth, targetHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        return baos.toByteArray();
    }

//    public void saveTempFile(AttachFileRequest attachFileRequest) {
//        if (!isBase64(attachFileRequest.getFileContent())) {
//            log.error("saveTempFile: Invalid base64 string");
//            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Invalid base64 string").build();
//        }
//        //validateCdbIdAndLogin(attachFileRequest);
//        String fileType = validateAttachFileType(AttachFileValidator.decodeBase64(attachFileRequest.getFileContent()));
//        attachFileRequest.setFileType(fileType);
//        int allowedFileSize = getAllowedFileSize(attachFileRequest);
//        Double mbValue = Math.ceil((double) allowedFileSize / (double) AttachFileValidator.MB_CONVERTOR);
//        log.info("Allowed file size is in MB {}", mbValue);
//        if (!AttachFileValidator.isValidFileSize(attachFileRequest.getProductId(), attachFileRequest.getFileContent(), allowedFileSize)) {
//            log.error("saveTempFile : File size exceeds the {} MB limit", mbValue);
//            throw ContentTooLargeException.builder()
//                    .message(ExceptionMessages.CONTENT_TOO_LARGE + "File size exceeds the " + mbValue + " MB limit").build();
//        }
//        try {
//            ProductImage productImage = productImageRepository.findByFileName(attachFileRequest.getFileName());
//            if (productImage != null) {
//                productImage.setFileName(attachFileRequest.getFileName());
//                productImage.setFileType(attachFileRequest.getFileType());
//                productImage.setCreationTime(LocalDateTime.now());
//                productImage.setContent(AttachFileValidator.decodeBase64(attachFileRequest.getFileContent()));
//            } else {
//                productImage = ProductFileAttach.builder()
//                        .content(AttachFileValidator.decodeBase64(attachFileRequest.getFileContent()))
//                        .fileName(attachFileRequest.getFileName())
//                        .fileType(attachFileRequest.getFileType())
//                        .creationTime(LocalDateTime.now())
//                        .build();
//            }
//            productImageRepository.save(productImage);
//        } catch (Exception ex) {
//            log.error("Error in attaching file {}", ex);
//            throw TechnicalException.builder().message(ExceptionMessages.TECHNICAL_EXCEPTION + ex.getMessage()).build();
//        }
//    }

    private String validateAttachImageFileType(byte[] fileContent) {
        String fileType;
        try {
            if (fileContent == null || fileContent.length == 0) {
                log.error("validateAttachFileType: File content is empty");
                throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "File content is empty").build();
            }
            fileType = tika.detect(fileContent);
            if (!getAllMediaTypes().contains(fileType)) {
                log.error("validateAttachFileType: " + FILE_TYPE_NOT_SUPPORTED);
                throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + FILE_TYPE_NOT_SUPPORTED).build();
            }
        } catch (Exception e) {
            log.error("validateAttachFileType: Error in detecting file type {}", e);
            throw TechnicalException.builder().message(ExceptionMessages.TECHNICAL_EXCEPTION + e.getMessage()).build();
        }
        return fileType;
    }


    private int getAllowedFileSize(ProductImageRequest productImageRequest) {
        int allowedFileSize = groupConfig.DEFAULT_MAX_FILE_SIZE;

//        Product product = cosProductService.getProductByName(attachFileRequest.getProduct());
//        if (product == null) {
//            log.error("getAllowedFileSize : No product found for the name {} returning allowedSize as 0", attachFileRequest.getProduct());
//            return 0;
//        }
//        return product.getProductDef().getOrderTypes().stream()
//                .filter(orderType -> orderType.getOrderAction().getName().equalsIgnoreCase(attachFileRequest.getOrderAction()))
//                .flatMap(orderType -> orderType.getWorkflows().stream())
//                .filter(workflow -> workflow.getStepName().equalsIgnoreCase(STEP_NAME_FOR_ALLOWED_FILE_SIZE))
//                .flatMap(workflow -> workflow.getGroupConfigs().stream())
//                .filter(groupConfig -> groupConfig.getGroup().equalsIgnoreCase(GROUP_NAME_FOR_ALLOWED_FILE_SIZE))
//               // .mapToInt(GroupConfig::getMaxSize)
//                .findFirst()
//                .orElse(0);
        return allowedFileSize;
    }

    public void deleteTempFile(ProductImageRequest productImageRequest) {
        //validateCdbIdAndLogin(attachFileRequest);
        ProductImage productImage = productImageRepository.findByFileName(productImageRequest.getFileName());
        if (productImage != null) {
            try {
                productImageRepository.delete(productImage);
            } catch (Exception ex) {
                log.error("Error in deleting file {}", ex);
                throw TechnicalException.builder().message(ExceptionMessages.TECHNICAL_EXCEPTION + ex.getMessage()).build();
            }
        } else {
            log.error("deleteTempFile : No record found for the file name {}", productImageRequest.getFileName());
            throw ResourceNotFoundException.builder().message(ExceptionMessages.RESOURCE_NOT_FOUND + "No record found for the file name " + productImageRequest.getFileName()).build();
        }
    }
//
//    public ProductFileAttach getTempFile(AttachFileRequest attachFileRequest) {
//        //validateCdbIdAndLogin(attachFileRequest);
//        ProductFileAttach productFileAttach = productImageRepository.findByFileName(attachFileRequest.getFileName());
//        if (productFileAttach != null) {
//            return productFileAttach;
//        } else {
//            log.error("getTempFile : No record found for the file name {}", attachFileRequest.getFileName());
//            throw ResourceNotFoundException.builder().message(ExceptionMessages.RESOURCE_NOT_FOUND + "No record found for the file name " + attachFileRequest.getFileName()).build();
//        }
//    }


    @Transactional
    public void deleteOldAttachTemp() {
        try {
            LocalDateTime thresholdDate = LocalDateTime.now().minusDays(groupConfig.PROD_IMG_DAYS_TO_KEEP);
            log.info("Deleting attachments older than {} days", groupConfig.PROD_IMG_DAYS_TO_KEEP);

            int deletedRecords = productImageRepository.deleteByCreationTimeBefore(thresholdDate);
            log.info("Deleted {} old attachments", deletedRecords);
        } catch (Exception e) {
            log.error("Error occurred during attachment cleanup: ", e);
            throw new RestClientException("Error occurred during attachment cleanup.");
        }
    }


}
*/