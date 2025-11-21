package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.ProductImageRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.entities.Product;
import com.datasaz.ecommerce.repositories.entities.ProductImage;
import com.datasaz.ecommerce.services.interfaces.IProductS3ClientImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductS3ClientImageService implements IProductS3ClientImageService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final S3Client s3Client;

    //private final AmazonS3 amazonS3;

    //@Value("${spring.s3.bucket}")

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${image.max.size.mb:5}")
    private int maxFileSizeMb;

    @Value("${image.resize.width:800}")
    private int resizeWidth;

    @Value("${image.resize.height:800}")
    private int resizeHeight;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif"
    );

    @Transactional
    @CacheEvict(value = "productImages", key = "#productId")
    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) { //throws Exception
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        validateFile(file);

        ProductImage image = null;
        try {
            // Upload to S3
            //amazonS3.putObject(new PutObjectRequest(bucketName, fileName, tempFile));

            // Resize image
            byte[] resizedImage = resizeImage(file.getBytes());

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(resizedImage));

            image = ProductImage.builder()
                    .fileName(fileName)
                    .fileUrl(String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName))
                    .contentType(file.getContentType())
                    .isPrimary(isPrimary)
                    .product(product)
                    .build();

        } catch (Exception e) {
            log.error("IO Exception: {}", e.getMessage());
            throw new RestClientException("IO Exception: " + e.getMessage());
        }

        if (isPrimary) {
            productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        productImageRepository.save(existing);
                    });
        }

        return productImageRepository.save(image);
    }

    @Transactional
    @CacheEvict(value = "productImages", key = "#result.product.id")
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(image.getFileName())
                .build();
        s3Client.deleteObject(deleteObjectRequest);

        productImageRepository.delete(image);
    }

    @Cacheable(value = "productImages", key = "#productId")
    public List<ProductImage> getImagesByProductId(Long productId) {
        return productImageRepository.findByProductId(productId);
    }

    @Transactional
    //@Scheduled(cron = "0 0 1 * * ?") // Daily at 1 AM
    public void cleanupOldImages() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<ProductImage> orphanedImages = productImageRepository.findByProductIsNullAndCreatedAtBefore(threshold);
        orphanedImages.forEach(image -> {
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(image.getFileName())
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
                productImageRepository.delete(image);
                log.info("Deleted orphaned image: {}", image.getFileName());
            } catch (Exception e) {
                log.error("Failed to delete orphaned image: {}", image.getFileName(), e);
            }
        });
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds " + maxFileSizeMb + "MB");
        }
    }

    private byte[] resizeImage(byte[] originalImage) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(originalImage));
        BufferedImage resized = Scalr.resize(img, Scalr.Method.QUALITY, resizeWidth, resizeHeight);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        return baos.toByteArray();
    }
    /*


    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) {   //throws IOException
        String fileName = UUID.randomUUID()+ "_"+ LocalDateTime.now() + "_" + file.getOriginalFilename();
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(file.getContentType())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        }
        catch (Exception e) {
            log.error("IO Exception: {}", e.getMessage());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        ProductImage image = ProductImage.builder()
                .fileName(fileName)
                .fileUrl(String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileName))
                .contentType(file.getContentType())
                .isPrimary(isPrimary)
                .product(product)
                .build();

        return productImageRepository.save(image);
    }


//    @Transactional
//    @Override
//    @CacheEvict(value = "productImages", key = "#productId")
//    @CachePut(value = "productImages", key = "#result.id")
//    @Cacheable(value = "productImages", key = "#productId")
//    @PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#productId, 'Product', 'UPLOAD_IMAGE')")
//    @AuditLog(action = AuditLog.Action.UPLOAD_IMAGE, resourceType = AuditLog.ResourceType.PRODUCT, resourceId = "#productId")
//    @Transactional(propagation = Propagation.REQUIRES_NEW) // To avoid race condition when saving product images
//    @SuppressWarnings("squid:S2095") // Suppressing "Resources should be closed" warning as the S3Client closes the stream automatically




//    Local Image upload suppressed by the above methode for S3 upload
//    public ProductImage uploadImage(Long productId, MultipartFile file, boolean isPrimary) throws IOException {
//        Product product = productRepository.findById(productId)
//                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
//
//        validateFile(file);
//
//        String fileName = UUID.randomUUID()+ "_" + LocalDateTime.now() + "_" + file.getOriginalFilename();
//        Path filePath = Paths.get(uploadDir, fileName);
//
//        // Create upload directory if it doesn't exist
//        Files.createDirectories(filePath.getParent());
//
//        // Resize image
//        byte[] resizedImage = resizeImage(file.getBytes(), 800, 800); // Max 800x800
//        Files.write(filePath, resizedImage);
//
//        ProductImage image = ProductImage.builder()
//                .fileName(fileName)
//                .fileUrl("/" + uploadDir + "/" + fileName)
//                .contentType(file.getContentType())
//                .isPrimary(isPrimary)
//                .product(product)
//                .build();
//
//        // If setting as primary, unset others
//        if (isPrimary) {
//            productImageRepository.findByProductIdAndIsPrimaryTrue(productId)
//                    .ifPresent(existing -> {
//                        existing.setIsPrimary(false);
//                        productImageRepository.save(existing);
//                    });
//        }
//
//        return productImageRepository.save(image);
//    }

    // doesn't comply for the S3 upload
    public void deleteImage(Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        try {
            Files.deleteIfExists(Paths.get(image.getFileUrl().substring(1)));
        } catch (IOException e) {
            log.error("Failed to delete image file: {}", image.getFileUrl(), e);
        }

        productImageRepository.delete(image);
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

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType());
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds " + maxFileSizeMb + "MB");
        }
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

    private String validateAttachFileType(byte[] fileContent) {
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


    private int getAllowedFileSize(Del_AttachFileRequest delAttachFileRequest) {
        int allowedFileSize = new GroupConfig().DEFAULT_MAX_FILE_SIZE;

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

//    public void deleteTempFile(AttachFileRequest attachFileRequest) {
//        //validateCdbIdAndLogin(attachFileRequest);
//        ProductImage productImage = productImageRepository.findByFileName(attachFileRequest.getFileName());
//        if (productImage != null) {
//            try {
//                productImageRepository.delete(productImage);
//            } catch (Exception ex) {
//                log.error("Error in deleting file {}", ex);
//                throw TechnicalException.builder().message(ExceptionMessages.TECHNICAL_EXCEPTION + ex.getMessage()).build();
//            }
//        } else {
//            log.error("deleteTempFile : No record found for the file name {}", attachFileRequest.getFileName());
//            throw ResourceNotFoundException.builder().message(ExceptionMessages.RESOURCE_NOT_FOUND + "No record found for the file name " + attachFileRequest.getFileName()).build();
//        }
//    }
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

//    private void validateCdbIdAndLogin(AttachFileRequest attachFileRequest) {
//        if (!AttachFileValidator.isValidCdbId(attachFileRequest.getCdbId())) {
//            log.error(PLEASE_PROVIDE_VALID_CDBID);
//            throw BadRequestException.builder().message(PLEASE_PROVIDE_VALID_CDBID).build();
//        }
//        if (!AttachFileValidator.isValidLogin(attachFileRequest.getLogin())) {
//            log.error(PLEASE_PROVIDE_VALID_LOGIN);
//            throw BadRequestException.builder().message(PLEASE_PROVIDE_VALID_LOGIN).build();
//        }
//    }

    @Transactional
    public void deleteOldAttachTemp() {
        try {
            LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysToKeep);
            log.info("Deleting attachments older than {} days", daysToKeep);

            int deletedRecords = productImageRepository.deleteByCreationTimeBefore(thresholdDate);
            log.info("Deleted {} old attachments", deletedRecords);
        } catch (Exception e) {
            log.error("Error occurred during attachment cleanup: ", e);
            throw new RestClientException("Error occurred during attachment cleanup.");
        }
    }

     */
}
