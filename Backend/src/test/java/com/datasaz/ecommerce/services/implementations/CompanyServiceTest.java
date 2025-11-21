package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.GroupConfig;
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.CompanyAdminRightsRepository;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAdminRightsRepository adminRightsRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private GroupConfig groupConfig;

    @InjectMocks
    private CompanyService companyService;

    private byte[] testJpeg;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Load test image or create mock image
        try {
            testJpeg = Files.readAllBytes(Paths.get("src/test/resources/test.jpg"));
        } catch (Exception e) {
            testJpeg = createMockImage("jpeg");
        }

        // Configure GroupConfig
        ReflectionTestUtils.setField(groupConfig, "resizeWidth", 400);
        ReflectionTestUtils.setField(groupConfig, "resizeHeight", 400);
        ReflectionTestUtils.setField(groupConfig, "imageQuality", 0.7f);
        ReflectionTestUtils.setField(groupConfig, "maxFileSizeMb", 5);
        ReflectionTestUtils.setField(groupConfig, "MAX_FILE_SIZE", 5_242_880);
        ReflectionTestUtils.setField(groupConfig, "UPLOAD_DIR", "./uploads");
        when(groupConfig.getALLOWED_IMAGE_TYPES()).thenReturn(Set.of("image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff", "image/vnd.wap.wbmp", "image/webp"));

        // Setup user
        user = User.builder().id(1L).emailAddress("test@test.com").build();
    }

    private byte[] createMockImage(String format) throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    void registerCompany_WithMultipartFile_DatabaseMode_Success() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        CompanyRequest request = CompanyRequest.builder()
                .name("TestCompany")
                .registrationNumber("12345")
                .build();
        MockMultipartFile file = new MockMultipartFile("image", "logo.jpg", "image/jpeg", testJpeg);
        Company company = Company.builder()
                .id(1L)
                .name("TestCompany")
                .registrationNumber("12345")
                .logoContent(testJpeg)
                .logoContentType("image/jpeg")
                .logoFileExtension("jpg")
                .primaryAdmin(user)
                .deleted(false)
                .build();

        CompanyService spiedService = spy(companyService);
        doReturn(testJpeg).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(false), eq(400), eq(400));
        when(companyRepository.findByName("TestCompany")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = spiedService.registerCompany(request, file, user);

        // Assert
        assertNotNull(result);
        assertEquals("TestCompany", result.getName());
        assertArrayEquals(testJpeg, result.getLogoContent());
        assertEquals("image/jpeg", result.getLogoContentType());
        assertEquals("jpg", result.getLogoFileExtension());
        assertNull(result.getLogoUrl());
        assertEquals(user, result.getPrimaryAdmin());
        verify(companyRepository).save(any(Company.class));
        verify(auditLogService).logAction("test@test.com", "REGISTER_COMPANY", "Registered new company: TestCompany");
        verify(auditLogService).logAction("TestCompany", "UPLOAD_LOGO_PICTURE", "Uploaded logo to database for company: TestCompany");
    }

    @Test
    void registerCompany_WithBase64Logo_DatabaseMode_Success() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        String base64Image = Base64.getEncoder().encodeToString(testJpeg);
        CompanyRequest request = CompanyRequest.builder()
                .name("TestCompany")
                .registrationNumber("12345")
                .logoContent(base64Image)
                .logoContentType("image/jpeg")
                .build();
        Company company = Company.builder()
                .id(1L)
                .name("TestCompany")
                .registrationNumber("12345")
                .logoContent(testJpeg)
                .logoContentType("image/jpeg")
                .logoFileExtension("jpg")
                .primaryAdmin(user)
                .deleted(false)
                .build();

        CompanyService spiedService = spy(companyService);
        doReturn(testJpeg).when(spiedService).resizeImage(eq(testJpeg), eq("jpg"), eq(false), eq(400), eq(400));
        when(companyRepository.findByName("TestCompany")).thenReturn(Optional.empty());
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        Company result = spiedService.registerCompany(request, null, user);

        // Assert
        assertNotNull(result);
        assertEquals("TestCompany", result.getName());
        assertArrayEquals(testJpeg, result.getLogoContent());
        assertEquals("image/jpeg", result.getLogoContentType());
        assertEquals("jpg", result.getLogoFileExtension());
        assertNull(result.getLogoUrl());
        assertEquals(user, result.getPrimaryAdmin());
        verify(companyRepository).save(any(Company.class));
        verify(auditLogService).logAction("test@test.com", "REGISTER_COMPANY", "Registered new company: TestCompany");
        verify(auditLogService).logAction("TestCompany", "UPLOAD_LOGO_PICTURE", "Uploaded logo to database for company: TestCompany");
    }

    @Test
    void uploadLogoPicture_MultipartFile_FileSystemMode_Success() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "file system");
        MockMultipartFile file = new MockMultipartFile("image", "logo.jpg", "image/jpeg", testJpeg);
        Company company = Company.builder()
                .id(1L)
                .name("TestCompany")
                .logoUrl(groupConfig.UPLOAD_DIR + "/company_logos/logo.jpg")
                .deleted(false)
                .build();

        CompanyService spiedService = spy(companyService);
        doNothing().when(spiedService).deleteOldImage(anyString());
        when(companyRepository.findByNameAndDeletedFalse("TestCompany")).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenReturn(company);

        // Act
        String result = spiedService.uploadLogoPicture(file, "TestCompany");

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith(groupConfig.UPLOAD_DIR + "/company_logos/"));
        assertNull(company.getLogoContent());
        assertNull(company.getLogoContentType());
        assertNull(company.getLogoFileExtension());
        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq("TestCompany"), eq("UPLOAD_LOGO_PICTURE"), contains("Uploaded logo to file system"));
    }

    @Test
    void uploadLogoPicture_TooLargeImage_ThrowsBadRequestException() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        byte[] largeImage = new byte[6_000_000]; // 6 MB, exceeds 5 MB limit
        MockMultipartFile file = new MockMultipartFile("image", "logo.jpg", "image/jpeg", largeImage);
        Company company = Company.builder().id(1L).name("TestCompany").deleted(false).build();
        when(companyRepository.findByNameAndDeletedFalse("TestCompany")).thenReturn(Optional.of(company));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> companyService.uploadLogoPicture(file, "TestCompany"));
    }

    @Test
    void uploadLogoPicture_InvalidMimeType_ThrowsBadRequestException() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        MockMultipartFile file = new MockMultipartFile("image", "logo.txt", "text/plain", new byte[]{1, 2, 3});
        Company company = Company.builder().id(1L).name("TestCompany").deleted(false).build();
        when(companyRepository.findByNameAndDeletedFalse("TestCompany")).thenReturn(Optional.of(company));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> companyService.uploadLogoPicture(file, "TestCompany"));
    }

    @Test
    void uploadLogoPicture_CompanyNotFound_ThrowsUserNotFoundException() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        MockMultipartFile file = new MockMultipartFile("image", "logo.jpg", "image/jpeg", testJpeg);
        when(companyRepository.findByNameAndDeletedFalse("TestCompany")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> companyService.uploadLogoPicture(file, "TestCompany"));
    }

    @Test
    void uploadLogoPicture_EmptyImage_ThrowsBadRequestException() {
        // Arrange
        ReflectionTestUtils.setField(groupConfig, "imageStorageMode", "database");
        MockMultipartFile file = new MockMultipartFile("image", "logo.jpg", "image/jpeg", new byte[0]);
        Company company = Company.builder().id(1L).name("TestCompany").deleted(false).build();
        when(companyRepository.findByNameAndDeletedFalse("TestCompany")).thenReturn(Optional.of(company));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> companyService.uploadLogoPicture(file, "TestCompany"));
    }
}
