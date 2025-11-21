package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import com.datasaz.ecommerce.utilities.FileStorageService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
import org.springframework.cache.annotation.CacheEvict;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryService implements IAdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final GroupConfig groupConfig;
    private final Tika tika;

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public CategoryResponse saveCategory(CategoryRequest categoryRequest) {
        log.info("Save category: {}", categoryRequest.getName());
        if (categoryRequest == null) {
            log.error("Category request cannot be null");
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category request cannot be null").build();
        }

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }

        Category category = categoryMapper.toEntity(categoryRequest, parent);
        category.setCreatedAt(LocalDateTime.now());

        // Handle image from CategoryRequest (Base64)
        if (categoryRequest.getImageContent() != null && !categoryRequest.getImageContent().isEmpty()) {
            uploadCategoryImage(category, categoryRequest);
        }

        Category savedCategory = categoryRepository.save(category);
        auditLogService.logAction("SYSTEM", "SAVE_CATEGORY", "Saved category: " + category.getName());
        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        log.info("Update category with id: {}", id);
        if (id == null || categoryRequest == null) {
            log.error("Category ID {} or request {} cannot be null", id, categoryRequest);
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category ID and request cannot be null").build();
        }

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("No categories found matching id: {}", id);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        existingCategory.setName(categoryRequest.getName());
        existingCategory.setDescription(categoryRequest.getDescription());

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }
        existingCategory.setParent(parent);

        // Handle image from CategoryRequest (Base64)
        if (categoryRequest.getImageContent() != null && !categoryRequest.getImageContent().isEmpty()) {
            uploadCategoryImage(existingCategory, categoryRequest);
        }

        Category updatedCategory = categoryRepository.save(existingCategory);
        auditLogService.logAction("SYSTEM", "UPDATE_CATEGORY", "Updated category: " + existingCategory.getName());
        return categoryMapper.toResponse(updatedCategory);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Delete by id: {}", id);
        if (!categoryRepository.existsById(id)) {
            log.error("Category not found with id: {}", id);
            throw CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
        }
        categoryRepository.deleteById(id);
        auditLogService.logAction("SYSTEM", "DELETE_CATEGORY", "Deleted category with id: " + id);
    }

    @Override
    @Transactional
    @RateLimiter(name = "adminCategoryService")
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public String uploadCategoryImage(MultipartFile image, Long categoryId) {
        log.info("Uploading category image for category ID: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", categoryId);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        uploadCategoryImage(category, image);
        categoryRepository.save(category);
        return category.getImageUrl() != null ? category.getImageUrl() : "";
    }

    private void uploadCategoryImage(Category category, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            log.error("Image file is required");
            throw BadRequestException.builder().message("Image file is required").build();
        }

        validateImage(image);

        String fileName = generateFileName(image.getOriginalFilename());
        String extension = getFileExtension(image.getOriginalFilename());

        try {
            byte[] imageBytes = image.getBytes();
            if (groupConfig.imageStorageMode.equals("database")) {
                // Resize and store in database
                byte[] resizedImage = resizeImage(imageBytes, extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
                if (resizedImage.length == 0) {
                    log.error("Resized image is empty for file: {}", fileName);
                    throw TechnicalException.builder().message("Failed to resize image: empty result").build();
                }
                if (resizedImage.length > groupConfig.MAX_FILE_SIZE) {
                    log.error("Image size {} bytes exceeds limit {} MB", resizedImage.length, groupConfig.maxFileSizeMb);
                    throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
                }

                category.setImageContent(resizedImage);
                category.setImageContentType(image.getContentType());
                category.setImageFileExtension(extension);
                category.setImageUrl(null); // Clear imageUrl for database mode
                auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE", "Uploaded image to database for category: " + category.getName());
            } else {
                // Store in file system
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "category_images");
                if (!fileStorageService.exists(uploadDir)) {
                    fileStorageService.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                try (var inputStream = image.getInputStream()) {
                    fileStorageService.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                category.setImageUrl("/Uploads/category_images/" + fileName);
                category.setImageContent(null);
                category.setImageContentType(null);
                category.setImageFileExtension(null);
                auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE", "Uploaded image to file system for category: " + category.getName() + " at: " + filePath);
            }

            // Delete old image if exists
            if (groupConfig.imageStorageMode.equals("file system") && category.getImageUrl() != null) {
                deleteOldImage(category.getImageUrl());
            }
        } catch (IOException e) {
            log.error("Failed to upload image for category {}: {}", category.getName(), e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    private void uploadCategoryImage(Category category, CategoryRequest categoryRequest) {
        if (categoryRequest.getImageContent() == null || categoryRequest.getImageContent().isEmpty()) {
            log.error("Image content is required");
            throw BadRequestException.builder().message("Image content is required").build();
        }

        validateImageRequest(categoryRequest);

        String fileName = generateFileName(categoryRequest.getName() + ".jpg");
        String extension = getFileExtension(categoryRequest.getName() + ".jpg");

        try {
            byte[] imageBytes = Base64.getDecoder().decode(categoryRequest.getImageContent());
            if (groupConfig.imageStorageMode.equals("database")) {
                // Resize and store in database
                byte[] resizedImage = resizeImage(imageBytes, extension, false, groupConfig.resizeWidth, groupConfig.resizeHeight);
                if (resizedImage.length == 0) {
                    log.error("Resized image is empty for file: {}", fileName);
                    throw TechnicalException.builder().message("Failed to resize image: empty result").build();
                }
                if (resizedImage.length > groupConfig.MAX_FILE_SIZE) {
                    log.error("Image size {} bytes exceeds limit {} MB", resizedImage.length, groupConfig.maxFileSizeMb);
                    throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
                }

                category.setImageContent(resizedImage);
                category.setImageContentType(categoryRequest.getImageContentType());
                category.setImageFileExtension(extension);
                category.setImageUrl(null);
                auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE", "Uploaded image to database for category: " + category.getName());
            } else {
                // Store in file system
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "category_images");
                if (!fileStorageService.exists(uploadDir)) {
                    fileStorageService.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                fileStorageService.write(filePath, imageBytes);
                category.setImageUrl("/Uploads/category_images/" + fileName);
                category.setImageContent(null);
                category.setImageContentType(null);
                category.setImageFileExtension(null);
                auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE", "Uploaded image to file system for category: " + category.getName() + " at: " + filePath);
            }

            // Delete old image if exists
            if (groupConfig.imageStorageMode.equals("file system") && category.getImageUrl() != null) {
                deleteOldImage(category.getImageUrl());
            }
        } catch (IOException e) {
            log.error("Failed to upload image for category {}: {}", category.getName(), e.getMessage());
            throw TechnicalException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BadRequestException.builder().message("File is empty or null").build();
        }
        if (file.getSize() > groupConfig.MAX_FILE_SIZE) {
            log.error("File size {} bytes exceeds limit {} MB ({} bytes)", file.getSize(), groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("File size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }
        String mimeType;
        try {
            mimeType = tika.detect(file.getInputStream());
            if (!groupConfig.getALLOWED_IMAGE_TYPES().contains(mimeType)) {
                log.error("Unsupported file type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported file type: " + mimeType).build();
            }
            try (ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes())) {
                BufferedImage img = ImageIO.read(bais);
                if (img == null) {
                    log.error("Invalid image content: cannot read image for {}", file.getOriginalFilename());
                    throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
                }
                if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                    log.warn("Image has non-RGB colorspace: {} for {}", img.getColorModel().getColorSpace().getType(), file.getOriginalFilename());
                    // RGB conversion will happen in resizeImage
                }
            }
        } catch (IOException e) {
            log.error("Error validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private void validateImageRequest(CategoryRequest request) {
        if (request.getImageContent() == null || request.getImageContent().isBlank()) {
            throw BadRequestException.builder().message("Image request is not complete").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.getImageContent());
        } catch (IllegalArgumentException e) {
            log.error("Invalid Base64 content for image: {}", request.getName());
            throw BadRequestException.builder().message("Invalid Base64 content for image: " + request.getName()).build();
        }

        if (imageBytes.length > groupConfig.MAX_FILE_SIZE) {
            log.error("Image size {} bytes exceeds limit {} MB ({} bytes)", imageBytes.length, groupConfig.maxFileSizeMb, groupConfig.MAX_FILE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds " + groupConfig.maxFileSizeMb + " MB limit").build();
        }

        String mimeType;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            mimeType = tika.detect(bais);
            if (!groupConfig.getALLOWED_IMAGE_TYPES().contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
            bais.reset();
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.error("Invalid image content: cannot read image for {}", request.getName());
                throw BadRequestException.builder().message("Invalid image content: cannot read image").build();
            }
            if (img.getColorModel().getColorSpace().getType() != java.awt.color.ColorSpace.TYPE_RGB) {
                log.warn("Image has non-RGB colorspace: {} for {}", img.getColorModel().getColorSpace().getType(), request.getName());
                // RGB conversion will happen in resizeImage
            }
        } catch (IOException e) {
            log.error("Error detecting image type or validating image: {}", e.getMessage());
            throw TechnicalException.builder().message("Error validating image: " + e.getMessage()).build();
        }
    }

    private byte[] resizeImage(byte[] originalImage, String extension, boolean forceJpeg, int targetWidth, int targetHeight) throws IOException {
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

            BufferedImage rgbImage = resized;
            String outputFormat = forceJpeg ? "jpg" : (extension != null && !extension.isEmpty() ? extension.toLowerCase() : "jpg");
            String formatName;
            switch (outputFormat) {
                case "jpg":
                case "jpeg":
                    formatName = "jpeg";
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

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            try {
                fileStorageService.deleteIfExists(oldFilePath);
                log.info("Deleted old category image: {}", oldImageUrl);
            } catch (IOException e) {
                log.warn("Failed to delete old category image {}: {}", oldImageUrl, e.getMessage());
            }
        }
    }

    private String generateFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        return UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "jpg" : fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}

/*
import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.request.CategoryRequest;
import com.datasaz.ecommerce.models.response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import com.datasaz.ecommerce.utilities.FileStorageService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryService implements IAdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final AuditLogService auditLogService;
    private final FileStorageService fileStorageService;
    private final GroupConfig groupConfig;
    private final Tika tika;

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public CategoryResponse saveCategory(CategoryRequest categoryRequest) {
        log.info("Save category");
        if (categoryRequest == null) {
            log.error("Category request cannot be null");
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category request cannot be null").build();
        }

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }

        Category category = categoryMapper.toEntity(categoryRequest, parent);
        category.setCreatedAt(LocalDateTime.now());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        log.info("Update category with id: {}", id);
        if (id == null || categoryRequest == null) {
            log.error("Category ID {} or request {} cannot be null", id, categoryRequest);
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category ID and request cannot be null").build();
        }

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("No categories found matching id: {}", id);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        existingCategory.setName(categoryRequest.getName());
        existingCategory.setDescription(categoryRequest.getDescription());

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }
        existingCategory.setParent(parent);

        return categoryMapper.toResponse(categoryRepository.save(existingCategory));
    }

    @Override
    @Transactional
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public void deleteCategory(Long id) {
        log.info("Delete by id: {}", id);
        if (!categoryRepository.existsById(id)) {
            log.error("Category not found with id: {}", id);
            throw CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    @RateLimiter(name = "adminCategoryService")
    @CacheEvict(value = {"categories", "categoriesByName", "allCategories", "subcategories", "parentCategories", "categoryExists", "products", "productsByCategory", "allProducts"}, allEntries = true)
    public String uploadCategoryImage(MultipartFile image, Long categoryId) {
        log.info("Uploading category image for category ID: {}", categoryId);

        // Check for null or empty image first
        if (image == null || image.isEmpty()) {
            log.error("Image file is required");
            throw BadRequestException.builder().message("Image file is required").build();
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", categoryId);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        if (image.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
            log.error("Image size {} exceeds limit {}", image.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds limit").build();
        }

        String mimeType;
        try (var inputStream = image.getInputStream()) {
            mimeType = tika.detect(inputStream);
            if (mimeType == null || !GroupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type: {}", e.getMessage());
            throw BadRequestException.builder().message("Error detecting image type: " + e.getMessage()).build();
        }

        try (var inputStream = image.getInputStream()) {
            Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "category_images");
            if (!fileStorageService.exists(uploadDir)) {
                fileStorageService.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            String fileExtension = getFileExtension(image.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
            Path filePath = uploadDir.resolve(fileName);

            if (category.getImageUrl() != null) {
                deleteOldImage(category.getImageUrl());
            }
            log.info("Image uploaded for category {}: {}", categoryId, filePath);

            fileStorageService.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            category.setImageUrl(filePath.toString());
            categoryRepository.save(category);

            auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE",
                    "Uploaded image for category: " + category.getName() + " at file path: " + filePath + " with file name: " + fileName);

            return category.getImageUrl();
        } catch (IOException e) {
            log.error("Failed to upload image for category {}: {}", categoryId, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            try {
                fileStorageService.deleteIfExists(oldFilePath);
                log.info("Deleted old category image: {}", oldImageUrl);
            } catch (IOException e) {
                log.warn("Failed to delete old category image {}: {}", oldImageUrl, e.getMessage());
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "jpg" : fileName.substring(lastDotIndex + 1);
    }
}*/



/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.CategoryNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.CategoryMapper;
import com.datasaz.ecommerce.models.Request.CategoryRequest;
import com.datasaz.ecommerce.models.Response.CategoryResponse;
import com.datasaz.ecommerce.repositories.CategoryRepository;
import com.datasaz.ecommerce.repositories.entities.Category;
import com.datasaz.ecommerce.utilities.FileStorageService;
import com.datasaz.ecommerce.services.interfaces.IAdminCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCategoryService implements IAdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private final AuditLogService auditLogService;
    private static final Tika tika = new Tika();

    private final FileStorageService fileStorageService;
    //private final Tika tika;


    @Override
    public CategoryResponse saveCategory(CategoryRequest categoryRequest) {
        log.info("Save category");
        if (categoryRequest == null) {
            log.error("Category request cannot be null");
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category request cannot be null").build();
        }

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }

        Category category = categoryMapper.toEntity(categoryRequest, parent);
        category.setCreatedDate(LocalDateTime.now());

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(Long id, CategoryRequest categoryRequest) {
        log.info("Update category with id: {}", id);
        if (id == null || categoryRequest == null) {
            log.error("Category ID {} or request {} cannot be null", id, categoryRequest);
            throw BadRequestException.builder().message(ExceptionMessages.BAD_REQUEST + "Category ID and request cannot be null").build();
        }

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("No categories found matching id: {}", id);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        existingCategory.setName(categoryRequest.getName());
        existingCategory.setDescription(categoryRequest.getDescription());

        Category parent = null;
        if (categoryRequest.getParentId() != null) {
            parent = categoryRepository.findById(categoryRequest.getParentId())
                    .orElseThrow(() -> {
                        log.error("Parent category not found with id: {}", categoryRequest.getParentId());
                        return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Parent category not found").build();
                    });
        }
        existingCategory.setParent(parent);

        return categoryMapper.toResponse(categoryRepository.save(existingCategory));
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Delete by id: {}", id);
        if (!categoryRepository.existsById(id)) {
            log.error("Category not found with id: {}", id);
            throw CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
        }
        categoryRepository.deleteById(id);
    }

    public String uploadCategoryImage(MultipartFile image, Long categoryId) {
        log.info("Uploading category image for category ID: {}", categoryId);

        // Check for null or empty image first
        if (image == null || image.isEmpty()) {
            log.error("Image file is required");
            throw BadRequestException.builder().message("Image file is required").build();
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", categoryId);
                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
                });

        if (image.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
            log.error("Image size {} exceeds limit {}", image.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds limit").build();
        }

        String mimeType;
        try {
            mimeType = tika.detect(image.getInputStream());
            if (!GroupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type: {}", e.getMessage());
            throw BadRequestException.builder().message("Error detecting image type: " + e.getMessage()).build();
        }

        try (var inputStream = image.getInputStream()) {
            Path uploadDir = Path.of(GroupConfig.UPLOAD_DIR, "category_images");
            if (!fileStorageService.exists(uploadDir)) {
                fileStorageService.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            String fileExtension = getFileExtension(image.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
            Path filePath = uploadDir.resolve(fileName);

            if (category.getImageUrl() != null) {
                deleteOldImage(category.getImageUrl());
            }
            log.info("Image uploaded for category {}: {}", categoryId, filePath);

            fileStorageService.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            category.setImageUrl(filePath.toString());
            categoryRepository.save(category);

            auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE",
                    "Uploaded image for category: " + category.getName() + " at file path: " + filePath + " with file name: " + fileName);

            return category.getImageUrl();
        } catch (IOException e) {
            log.error("Failed to upload image for category {}: {}", categoryId, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload image: " + e.getMessage()).build();
        }
    }

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            try {
                fileStorageService.deleteIfExists(oldFilePath);
                log.info("Deleted old category image: {}", oldImageUrl);
            } catch (IOException e) {
                log.warn("Failed to delete old category image {}: {}", oldImageUrl, e.getMessage());
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "jpg";
        }
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex == -1 ? "jpg" : fileName.substring(lastDotIndex + 1);
    }

//    public String uploadCategoryImage(MultipartFile image, Long categoryId) {
//        log.info("Uploading category image for category ID: {}", categoryId);
//        Category category = categoryRepository.findById(categoryId)
//                .orElseThrow(() -> {
//                    log.error("Category not found with id: {}", categoryId);
//                    return CategoryNotFoundException.builder().message(ExceptionMessages.CATEGORY_NOT_FOUND + "Category not found.").build();
//                });
//
//        if (image.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
//            log.error("Image size {} exceeds limit {}", image.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
//            throw BadRequestException.builder().message("Image size exceeds limit").build();
//        }
//
//        String mimeType;
//        try {
//            mimeType = tika.detect(image.getInputStream());
//            if (!GroupConfig.isAllowedImage(image)) {
//                log.error("Unsupported image type: {}", mimeType);
//                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
//            }
//        } catch (IOException e) {
//            log.error("Error detecting image type: {}", e.getMessage());
//            throw BadRequestException.builder().message("Error detecting image type: " + e.getMessage()).build();
//        }
//
//        try (var inputStream = image.getInputStream()) {
//            Path uploadDir = Path.of(GroupConfig.UPLOAD_DIR, "category_images");
//            if (!Files.exists(uploadDir)) {
//                Files.createDirectories(uploadDir);
//                log.info("Created upload directory: {}", uploadDir);
//            }
//
//            String fileExtension = getFileExtension(image.getOriginalFilename());
//            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
//            Path filePath = uploadDir.resolve(fileName);
//
//            if (category.getImageUrl() != null) {
//                deleteOldImage(category.getImageUrl());
//            }
//            log.info("Image uploaded for category {}: {}", categoryId, filePath);
//
//            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
//            category.setImageUrl(filePath.toString());
//            categoryRepository.save(category);
//
//            auditLogService.logAction("SYSTEM", "UPLOAD_CATEGORY_IMAGE",
//                    "Uploaded image for category: " + category.getName() + " at file path: " + filePath + " with file name: " + fileName);
//
//            return category.getImageUrl();
//        } catch (IOException e) {
//            log.error("Failed to upload image for category {}: {}", categoryId, e.getMessage());
//            throw BadRequestException.builder().message("Failed to upload image: " + e.getMessage()).build();
//        }
//    }
//
//
//    private void deleteOldImage(String oldImageUrl) {
//        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
//            Path oldFilePath = Path.of(oldImageUrl);
//            if (Files.exists(oldFilePath)) {
//                try {
//                    Files.deleteIfExists(oldFilePath);
//                    log.info("Deleted old category image: {}", oldImageUrl);
//                } catch (IOException e) {
//                    log.warn("Failed to delete old category image {}: {}", oldImageUrl, e.getMessage());
//                }
//            }
//        }
//    }
//
//    private String getFileExtension(String fileName) {
//        if (fileName == null || fileName.lastIndexOf(".") == -1) {
//            return "jpg";
//        }
//        return fileName.substring(fileName.lastIndexOf(".") + 1);
//    }

}

*/