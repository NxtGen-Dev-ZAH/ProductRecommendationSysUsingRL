//package com.datasaz.ecommerce.services;
//
//import com.pxs.cws.ordering.cwscosordering.exceptions.ContentTooLargeException;
//import com.pxs.cws.ordering.cwscosordering.model.AttachFileRequest;
//import com.pxs.cws.ordering.cwscosordering.model.entities.CosOrderFileAttachTemp;
//import com.pxs.cws.ordering.cwscosordering.repositories.FileAttachRepository;
//import com.pxs.cws.ordering.cwscosordering.resources.product.Product;
//import com.pxs.cws.ordering.cwscosordering.resources.product.ProductDef;
//import com.pxs.cws.ordering.cwscosordering.utils.ConverterUtils;
//import com.pxs.exceptionsutils.exceptions.BadRequestException;
//import com.pxs.exceptionsutils.exceptions.ResourceNotFoundException;
//import com.pxs.exceptionsutils.exceptions.TechnicalException;
//import org.apache.tika.Tika;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.dao.DataAccessException;
//import org.springframework.test.util.ReflectionTestUtils;
//import org.springframework.web.client.RestClientException;
//
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.util.Base64;
//
//import static org.junit.Assert.assertThrows;
//import static org.junit.Assert.assertTrue;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AttachFileServiceTest {
//
//    @Mock
//    private FileAttachRepository fileAttachRepository;
//
//    @Mock
//    private CosProductService cosProductService;
//
//    @Mock
//    private Tika tika;
//
//    @InjectMocks
//    private AttachFileService attachFileService;
//
//    @BeforeEach
//    void setup() {
//        ReflectionTestUtils.setField(attachFileService, "daysToKeep", 1);
//    }
//
//    @Test
//    @DisplayName("Test saveTempFile method when file size is valid")
//    void test_SaveTempFile() {
//        String onsProductJsonString = ConverterUtils.loadAnyTextFileIntoString("src/test/resources/files/product_call_response.json");
//        ProductDef mockProduct = (ProductDef) ConverterUtils.jsonToObject(onsProductJsonString, ProductDef.class);
//        Product product = Product.builder()
//                .id(123L)
//                .productName("testProduct")
//                .productDef(mockProduct)
//                .build();
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("dGVzdENvbnRlbnQ=")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        CosOrderFileAttachTemp cosOrderFileAttachTemp = CosOrderFileAttachTemp.builder()
//                .id(1L)
//                .fileName(attachFileRequest.getFileName())
//                .content(attachFileRequest.getFileContent().getBytes())
//                .fileType(attachFileRequest.getFileType()).idToken(attachFileRequest.getToken()).build();
//        byte[] fileContent = Base64.getDecoder().decode(attachFileRequest.getFileContent());
//        String expectedFileType = "application/vnd.ms-outlook";
//
//        doReturn(expectedFileType).when(tika).detect(fileContent);
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(null);
//        when(cosProductService.getProductByName(anyString(), anyInt())).thenReturn(product);
//        when(fileAttachRepository.save(any())).thenReturn(cosOrderFileAttachTemp);
//        attachFileService.saveTempFile(attachFileRequest);
//        Assertions.assertEquals("testFile.txt",attachFileRequest.getFileName());
//
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(cosOrderFileAttachTemp);
//        when(cosProductService.getProductByName(anyString(), anyInt())).thenReturn(product);
//        when(fileAttachRepository.save(any())).thenReturn(cosOrderFileAttachTemp);
//        attachFileService.saveTempFile(attachFileRequest);
//        Assertions.assertEquals("testFile.txt",attachFileRequest.getFileName());
//
//    }
//
//    @Test
//    @DisplayName("Technical Exception saveTempFile when file size is valid")
//    void test_SaveTempFile_TechnicalException() {
//        String onsProductJsonString = ConverterUtils.loadAnyTextFileIntoString("src/test/resources/files/product_call_response.json");
//        ProductDef mockProduct = (ProductDef) ConverterUtils.jsonToObject(onsProductJsonString, ProductDef.class);
//        Product product = Product.builder()
//                .id(123L)
//                .productName("testProduct")
//                .productDef(mockProduct)
//                .build();
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("dGVzdENvbnRlbnQ=")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        CosOrderFileAttachTemp cosOrderFileAttachTemp = CosOrderFileAttachTemp.builder()
//                .id(1L)
//                .fileName(attachFileRequest.getFileName())
//                .content(attachFileRequest.getFileContent().getBytes())
//                .fileType(attachFileRequest.getFileType()).idToken(attachFileRequest.getToken()).build();
//        byte[] fileContent = Base64.getDecoder().decode(attachFileRequest.getFileContent());
//        String expectedFileType = "application/vnd.ms-outlook";
//
//        doReturn(expectedFileType).when(tika).detect(fileContent);
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(null);
//        when(cosProductService.getProductByName(anyString(), anyInt())).thenReturn(product);
//        when(fileAttachRepository.save(any())).thenThrow(new RuntimeException("Technical Exception"));
//        TechnicalException exception = assertThrows(TechnicalException.class, () -> {
//            attachFileService.saveTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Technical Exception"));
//    }
//
//    @Test
//    @DisplayName("Test saveTempFile method when file size is invalid")
//    void test_InvalidFileSize() throws IOException {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("dGVzdENvbnRlbnQ=")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//
//        byte[] fileContent = Base64.getDecoder().decode(attachFileRequest.getFileContent());
//        String expectedFileType = "application/vnd.ms-outlook";
//
//         doReturn(expectedFileType).when(tika).detect(fileContent);
//
//        when(cosProductService.getProductByName(anyString(), anyInt())).thenReturn(null);
//      //  when(tika.detect(mockContent)).thenReturn("text/plain");
//        ContentTooLargeException exception = assertThrows(ContentTooLargeException.class, () -> {
//            attachFileService.saveTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("File size exceeds the 0.0 MB limit"));
//    }
//
//    @Test
//    @DisplayName("Test saveTempFile and deleteTempFile method when CdbId is invalid")
//    void test_InvalidCdbId() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ONS")
//                .cdbId(null)
//                .login("testLogin")
//                .build();
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            attachFileService.saveTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Please provide a valid cdbId"));
//        exception = assertThrows(BadRequestException.class, () -> {
//            attachFileService.deleteTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Please provide a valid cdbId"));
//    }
//
//    @Test
//    @DisplayName("Test saveTempFile and deleteTempFile method when login is invalid")
//    void test_InvalidLogin() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ONS")
//                .cdbId(1234)
//                .login("")
//                .build();
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            attachFileService.saveTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Please provide a valid login"));
//
//        exception = assertThrows(BadRequestException.class, () -> {
//            attachFileService.deleteTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Please provide a valid login"));
//    }
//
//    @Test
//    @DisplayName("Test saveTempFile method when file content is invalid")
//    void test_InvalidFileContent() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("SGVsbG8gd29ybGQh@#")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ONS")
//                .cdbId(1234)
//                .login("login")
//                .build();
//        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
//            attachFileService.saveTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Invalid base64 string"));
//    }
//
//    @Test
//    @DisplayName("Test deleteTempFile method when File exists")
//    void test_deleteTempFile() {
//       AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        CosOrderFileAttachTemp cosOrderFileAttachTemp = CosOrderFileAttachTemp.builder()
//                .id(1L)
//                .fileName(attachFileRequest.getFileName())
//                .content(attachFileRequest.getFileContent().getBytes())
//                .fileType(attachFileRequest.getFileType()).idToken(attachFileRequest.getToken()).build();
//
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(cosOrderFileAttachTemp);
//        doNothing().when(fileAttachRepository).delete(any());
//        attachFileService.deleteTempFile(attachFileRequest);
//        Assertions.assertEquals("testFile.txt",attachFileRequest.getFileName());
//
//    }
//
//    @Test
//    @DisplayName("Test deleteTempFile method when File does not exists")
//    void test_deleteTempFile_ResourceNotFoundException() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(null);
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
//            attachFileService.deleteTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("No record found for the file name"));
//
//    }
//
//    @Test
//    @DisplayName("Technical Exception deleteTempFile when file size is valid")
//    void test_DeleteTempFile_TechnicalException() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        CosOrderFileAttachTemp cosOrderFileAttachTemp = CosOrderFileAttachTemp.builder()
//                .id(1L)
//                .fileName(attachFileRequest.getFileName())
//                .content(attachFileRequest.getFileContent().getBytes())
//                .fileType(attachFileRequest.getFileType()).idToken(attachFileRequest.getToken()).build();
//
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(cosOrderFileAttachTemp);
//        doThrow(new RuntimeException("Technical Exception")).when(fileAttachRepository).delete(any());
//        TechnicalException exception = assertThrows(TechnicalException.class, () -> {
//            attachFileService.deleteTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("Technical Exception"));
//    }
//    @Test
//    @DisplayName("Test getTempFile method when file exists")
//    void test_getTempFile() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        CosOrderFileAttachTemp cosOrderFileAttachTemp = CosOrderFileAttachTemp.builder()
//                .id(1L)
//                .fileName(attachFileRequest.getFileName())
//                .content(attachFileRequest.getFileContent().getBytes())
//                .fileType(attachFileRequest.getFileType()).idToken(attachFileRequest.getToken()).build();
//
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(cosOrderFileAttachTemp);
//        CosOrderFileAttachTemp result = attachFileService.getTempFile(attachFileRequest);
//        Assertions.assertEquals("testFile.txt",result.getFileName());
//    }
//    @Test
//    @DisplayName("Test getTempFile method when file does not exists")
//    void test_getTempFile_ResourceNotFoundException() {
//        AttachFileRequest attachFileRequest = AttachFileRequest.builder()
//                .fileName("testFile.txt")
//                .fileContent("testContent")
//                .orderAction("new")
//                .fileType("text/plain")
//                .product("ons")
//                .cdbId(123)
//                .login("testLogin")
//                .build();
//        attachFileRequest.generateToken();
//        when(fileAttachRepository.findByidTokenAndFileName(anyString(),anyString())).thenReturn(null);
//        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
//            attachFileService.getTempFile(attachFileRequest);
//        });
//        assertTrue(exception.getMessage().contains("No record found for the file name"));
//    }
//
//    @Test
//    void testDeleteOldAttachTemp_ReturnsSuccessWhenRecordsExist() {
//        // Arrange
//        when(fileAttachRepository.deleteByCreationTimeBefore(any(LocalDateTime.class))).thenReturn(5);
//
//        // Act
//        attachFileService.deleteOldAttachTemp();
//
//        // Assert
//        verify(fileAttachRepository, times(1)).deleteByCreationTimeBefore(any(LocalDateTime.class));
//    }
//
//    @Test
//    void testDeleteOldAttachTemp_ReturnsFailureWhenDatabaseErrorOccurs() {
//        when(fileAttachRepository.deleteByCreationTimeBefore(any(LocalDateTime.class))).thenThrow(new DataAccessException("Database error") {});
//
//        Exception exception= assertThrows(RestClientException.class, () -> attachFileService.deleteOldAttachTemp());
//        Assertions.assertEquals("Error occurred during attachment cleanup", exception.getMessage());
//        verify(fileAttachRepository, times(1)).deleteByCreationTimeBefore(any(LocalDateTime.class));
//    }
//}
