package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.TechnicalException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.CompanyAdminRightsRepository;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.CompanyAdminRights;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.imgscalr.Scalr;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService implements ICompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyAdminRightsRepository adminRightsRepository;
    private final AuditLogService auditLogService;
    private final GroupConfig groupConfig;
    private static final Tika tika = new Tika();

    @Override
    @Transactional
    public Company registerCompany(CompanyRequest companyRequest, MultipartFile image, User user) {
        Company company = companyRepository.findByName(companyRequest.getName())
                .orElseGet(() -> companyRepository.save(getNewCompany(companyRequest, image, user)));

        if (company.isDeleted()) {
            log.error("Error: Company already exists but deleted.");
            throw BadRequestException.builder().message("Error: Company was deleted, it must be Revoked.").build();
        }

        return company;
    }

    private Company getNewCompany(CompanyRequest companyRequest, MultipartFile image, User user) {
        if (companyRequest.getName() == null || companyRequest.getName().isEmpty()) {
            log.error("Error: Company name is not valid.");
            throw BadRequestException.builder().message("Company name can not be null or empty.").build();
        }

        Company company = new Company();
        company.setName(companyRequest.getName());

        // Handle logo upload (file system or database based on groupConfig.imageStorageMode)
        if (image != null) {
            uploadLogoPicture(company, image);
        } else if (companyRequest.getLogoContent() != null && !companyRequest.getLogoContent().isEmpty()) {
            uploadLogoPicture(company, companyRequest);
        }

        company.setRegistrationNumber(companyRequest.getRegistrationNumber() == null ? "null" : companyRequest.getRegistrationNumber());
        company.setVatNumber(companyRequest.getVatNumber());
        //company.setAddress(companyRequest.getAddress());
        company.setContactEmail(companyRequest.getContactEmail());
        company.setDeleted(false);
        company.setPrimaryAdmin(user);

        auditLogService.logAction(user.getEmailAddress(), "REGISTER_COMPANY", "Registered new company: " + company.getName());

        return company;
    }

    public List<CompanyAdminRights> getCompanyAdmins(Company company) {
        return adminRightsRepository.findByCompanyAndApprovedTrue(company);
    }

    @Override
    @Transactional
    public CompanyAdminRights assignAdminRights(User user, Company company, boolean canManageAdmins, boolean approved) {
        CompanyAdminRights rights = new CompanyAdminRights();
        rights.setUser(user);
        rights.setCompany(company);
        rights.setCanAddRemoveSellers(canManageAdmins);
        rights.setCanPromoteDemoteAdmins(canManageAdmins);
        rights.setCanDelegateAdminRights(canManageAdmins);
        rights.setApproved(approved);
        rights = adminRightsRepository.save(rights);
        auditLogService.logAction(user.getEmailAddress(), "ASSIGN_ADMIN_RIGHTS", "Assigned admin rights for company: " + company.getName() + ", approved: " + approved);
        return rights;
    }

    @Override
    @Transactional
    public void updateAdminRights(Long rightsId, boolean approved, User approvingUser) {
        CompanyAdminRights rights = adminRightsRepository.findById(rightsId)
                .orElseThrow(() -> new IllegalArgumentException("Admin rights not found"));
        rights.setApproved(approved);
        adminRightsRepository.save(rights);
        auditLogService.logAction(approvingUser.getEmailAddress(), "UPDATE_ADMIN_RIGHTS",
                "Updated admin rights for user " + rights.getUser().getEmailAddress() +
                        " in company " + rights.getCompany().getName() + ": " + (approved ? "Approved" : "Denied"));
    }

    @Override
    @Transactional
    @RateLimiter(name = "pictureUpload")
    public String uploadLogoPicture(MultipartFile image, String companyName) {
        Company company = companyRepository.findByNameAndDeletedFalse(companyName)
                .orElseThrow(() -> UserNotFoundException.builder().message("Company not found with name: " + companyName).build());
        uploadLogoPicture(company, image);
        companyRepository.save(company);
        return company.getLogoUrl();
    }

    private void uploadLogoPicture(Company company, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw BadRequestException.builder().message("Image is empty or null").build();
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

                company.setLogoContent(resizedImage);
                company.setLogoContentType(image.getContentType());
                company.setLogoFileExtension(extension);
                company.setLogoUrl(null); // Clear logoUrl for database mode
                auditLogService.logAction(company.getName(), "UPLOAD_LOGO_PICTURE", "Uploaded logo to database for company: " + company.getName());
            } else {
                // Store in file system
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "company_logos");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                try (var inputStream = image.getInputStream()) {
                    Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                company.setLogoUrl(groupConfig.UPLOAD_DIR + "/company_logos/" + fileName);
                company.setLogoContent(null);
                company.setLogoContentType(null);
                company.setLogoFileExtension(null);
                auditLogService.logAction(company.getName(), "UPLOAD_LOGO_PICTURE", "Uploaded logo to file system for company: " + company.getName() + " at: " + filePath);
            }

            // Delete old logo if exists
            if (groupConfig.imageStorageMode.equals("file system") && company.getLogoUrl() != null) {
                deleteOldImage(company.getLogoUrl());
            }
        } catch (IOException e) {
            log.error("Failed to upload logo for company {}: {}", company.getName(), e.getMessage());
            throw TechnicalException.builder().message("Failed to upload logo: " + e.getMessage()).build();
        }
    }

    private void uploadLogoPicture(Company company, CompanyRequest companyRequest) {
        if (companyRequest.getLogoContent() == null || companyRequest.getLogoContent().isEmpty()) {
            throw BadRequestException.builder().message("Logo content is empty or null").build();
        }

        validateImageRequest(companyRequest);

        String fileName = generateFileName(companyRequest.getName() + ".jpg");
        String extension = getFileExtension(companyRequest.getName() + ".jpg");

        try {
            byte[] imageBytes = Base64.getDecoder().decode(companyRequest.getLogoContent());
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

                company.setLogoContent(resizedImage);
                company.setLogoContentType(companyRequest.getLogoContentType());
                company.setLogoFileExtension(extension);
                company.setLogoUrl(null);
                auditLogService.logAction(company.getName(), "UPLOAD_LOGO_PICTURE", "Uploaded logo to database for company: " + company.getName());
            } else {
                // Store in file system
                Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "company_logos");
                if (!Files.exists(uploadDir)) {
                    Files.createDirectories(uploadDir);
                    log.info("Created upload directory: {}", uploadDir);
                }
                Path filePath = uploadDir.resolve(fileName);
                Files.write(filePath, imageBytes);
                company.setLogoUrl("/Uploads/company_logos/" + fileName);
                company.setLogoContent(null);
                company.setLogoContentType(null);
                company.setLogoFileExtension(null);
                auditLogService.logAction(company.getName(), "UPLOAD_LOGO_PICTURE", "Uploaded logo to file system for company: " + company.getName() + " at: " + filePath);
            }

            // Delete old logo if exists
            if (groupConfig.imageStorageMode.equals("file system") && company.getLogoUrl() != null) {
                deleteOldImage(company.getLogoUrl());
            }
        } catch (IOException e) {
            log.error("Failed to upload logo for company {}: {}", company.getName(), e.getMessage());
            throw TechnicalException.builder().message("Failed to upload logo: " + e.getMessage()).build();
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

    private void validateImageRequest(CompanyRequest request) {
        if (request.getLogoContent() == null || request.getLogoContent().isBlank()) {
            throw BadRequestException.builder().message("Image request is not complete").build();
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(request.getLogoContent());
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

    public void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            if (Files.exists(oldFilePath)) {
                try {
                    Files.deleteIfExists(oldFilePath);
                    log.info("Deleted old profile image: {}", oldImageUrl);
                } catch (IOException e) {
                    log.warn("Failed to delete old profile image {}: {}", oldImageUrl, e.getMessage());
                }
            }
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
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.CompanyAdminRightsRepository;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.CompanyAdminRights;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService implements ICompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyAdminRightsRepository adminRightsRepository;
    private final AuditLogService auditLogService;

    private final GroupConfig groupConfig;

    private static final Tika tika = new Tika();

    @Override
    @Transactional
    public Company registerCompany(CompanyRequest companyRequest, MultipartFile image, User user) {
        Company company = companyRepository.findByName(companyRequest.getName())
                .orElseGet(() -> companyRepository.save(getNewCompany(companyRequest, image, user)));
        //ResourceNotFoundException.builder().message("Company not exists").build()

        if (company.isDeleted()) {
            log.error("Error : Company already exists but deleted.");
            throw BadRequestException.builder().message("Error : Company was deleted, it must be Revoked.").build();
        }

        return company;
    }

    //TODO: Write Delete and Revoke company Methods

    private Company getNewCompany(CompanyRequest companyRequest, MultipartFile image, User user) {
        if (companyRequest.getName() == null || companyRequest.getName().isEmpty()) {
            log.error("Error : Company name is not valid.");
            throw BadRequestException.builder().message("Company name can not be null or empty.").build();
        }
        Company company = new Company();
        company.setName(companyRequest.getName());

        company.setLogoUrl(image != null ? uploadLogoPicture(image, companyRequest.getName()) : null);

        company.setRegistrationNumber(companyRequest.getRegistrationNumber() == null ? "null" : companyRequest.getRegistrationNumber());
        company.setVatNumber(companyRequest.getVatNumber() != null ? companyRequest.getVatNumber() : null);
        company.setAddress(companyRequest.getAddress() != null ? companyRequest.getAddress() : null);
        company.setContactEmail(companyRequest.getContactEmail() != null ? companyRequest.getContactEmail() : null);
        company.setDeleted(false);
        company.setPrimaryAdmin(user);

        auditLogService.logAction(user.getEmailAddress(), "REGISTER_COMPANY", "Registered new company: " + company.getName());

        return company;
    }

    public List<CompanyAdminRights> getCompanyAdmins(Company company) {
        return adminRightsRepository.findByCompanyAndApprovedTrue(company);
    }

    @Override
    @Transactional
    public CompanyAdminRights assignAdminRights(User user, Company company, boolean canManageAdmins, boolean approved) {
        CompanyAdminRights rights = new CompanyAdminRights();
        rights.setUser(user);
        rights.setCompany(company);
        rights.setCanAddRemoveSellers(canManageAdmins);
        rights.setCanPromoteDemoteAdmins(canManageAdmins);
        rights.setCanDelegateAdminRights(canManageAdmins);
        //rights.setCanManageAdmins(canManageAdmins);
        rights.setApproved(approved);
        rights = adminRightsRepository.save(rights);
        auditLogService.logAction(user.getEmailAddress(), "ASSIGN_ADMIN_RIGHTS", "Assigned admin rights for company: " + company.getName() + ", approved: " + approved);
        return rights;
    }

    @Override
    @Transactional
    public void updateAdminRights(Long rightsId, boolean approved, User approvingUser) {
        CompanyAdminRights rights = adminRightsRepository.findById(rightsId)
                .orElseThrow(() -> new IllegalArgumentException("Admin rights not found"));
        rights.setApproved(approved);
        adminRightsRepository.save(rights);
        auditLogService.logAction(approvingUser.getEmailAddress(), "UPDATE_ADMIN_RIGHTS",
                "Updated admin rights for user " + rights.getUser().getEmailAddress() +
                        " in company " + rights.getCompany().getName() + ": " + (approved ? "Approved" : "Denied"));
    }

    //TODO: add uploadLogo for groupConfig.imageStorageMode.equals("database")
    @Override
    @RateLimiter(name = "pictureUpload")
    public String uploadLogoPicture(MultipartFile image, String companyName) {
        log.info("uploadLogoPicture: Uploading logo picture for company: {}", companyName);
        Company company = companyRepository.findByNameAndDeletedFalse(companyName)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + companyName).build());

        if (image.getSize() > GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE) {
            log.error("Image size {} exceeds limit {}", image.getSize(), GroupConfig.DEFAULT_MAX_PROFILE_IMAGE_SIZE);
            throw BadRequestException.builder().message("Image size exceeds limit").build();
        }

        String mimeType;
        try {
            mimeType = tika.detect(image.getInputStream()); // Using tika-core:2.9.2
            if (mimeType == null || !GroupConfig.ALLOWED_IMAGE_TYPES.contains(mimeType)) {
                log.error("Unsupported image type: {}", mimeType);
                throw BadRequestException.builder().message("Unsupported image type: " + mimeType).build();
            }
        } catch (IOException e) {
            log.error("Error detecting image type: {}", e.getMessage());
            throw BadRequestException.builder().message("Error detecting image type: " + e.getMessage()).build();
        }

        try (var inputStream = image.getInputStream()) {
            Path uploadDir = Path.of(groupConfig.UPLOAD_DIR, "company_logos");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
                log.info("Created upload directory: {}", uploadDir);
            }

            String fileExtension = getFileExtension(image.getOriginalFilename());
            String fileName = UUID.randomUUID() + "-" + LocalDateTime.now().toString().replace(":", "-") + "." + fileExtension;
            Path filePath = uploadDir.resolve(fileName);

            if (company.getLogoUrl() != null) {
                String oldImageUrl = company.getLogoUrl();
                deleteOldImage(oldImageUrl);
            }
            log.info("Logo uploaded for company {}: {}", companyName, filePath);

            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            //company.setLogoUrl("/Uploads/company_logos/" + fileName);
            company.setLogoUrl(filePath.toString());
            companyRepository.save(company);


            auditLogService.logAction(companyName, "UPLOAD_LOGO_PICTURE", "Uploaded logo for company: " + companyName + " at file path: " + filePath + " with file Name: " + fileName);

            return company.getLogoUrl();

        } catch (IOException e) {
            log.error("Failed to upload profile picture for user {}: {}", companyName, e.getMessage());
            throw BadRequestException.builder().message("Failed to upload profile picture: " + e.getMessage()).build();
        }
    }

    private void deleteOldImage(String oldImageUrl) {
        if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
            Path oldFilePath = Path.of(oldImageUrl);
            if (Files.exists(oldFilePath)) {
                try {
                    Files.deleteIfExists(oldFilePath);
                    log.info("Deleted old profile image: {}", oldImageUrl);
                } catch (IOException e) {
                    log.warn("Failed to delete old profile image {}: {}", oldImageUrl, e.getMessage());
                }
            }
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

}

 */

