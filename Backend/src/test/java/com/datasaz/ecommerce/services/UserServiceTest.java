//package com.datasaz.ecommerce.services;
//
//import com.datasaz.ecommerce.mappers.UserMapper;
//import com.datasaz.ecommerce.models.Request.RegisterRequest;
//import com.datasaz.ecommerce.models.dto.UserDto;
//import com.datasaz.ecommerce.repositories.UserRepository;
//import com.datasaz.ecommerce.repositories.entities.User;
//import com.datasaz.ecommerce.services.implementations.UserService;
//import com.datasaz.ecommerce.utilities.Utility;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.api.io.TempDir;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//public class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private Utility utility;
//
//    @InjectMocks
//    private UserService userService;
//
//    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
//
//    @Mock
//    private UserMapper userMapper;
//
//    @TempDir
//    Path tempDir;
//
//    private User user;
//    private UserDto userDto;
//    private MockMultipartFile file;
//
//    @BeforeEach
//    void setUp() throws IOException, NoSuchFieldException, IllegalAccessException {
//        MockitoAnnotations.openMocks(this);
//
//        // Create a temporary uploads directory
//        Path uploadDir = tempDir.resolve("uploads/profile-pictures");
//        Files.createDirectories(uploadDir);
//        // Use reflection to set UPLOAD_DIR in UserService
//        java.lang.reflect.Field uploadDirField = UserService.class.getDeclaredField("UPLOAD_DIR");
//        uploadDirField.setAccessible(true);
//        uploadDirField.set(null, uploadDir.toString() + "/");
//
//        user = User.builder()
//                .id(1L)
//                .emailAddress("test@example.com")
//                .firstName("John")
//                .password("encodedPassword")
//                .build();
//
//        userDto = UserDto.builder()
//                .id(1L)
//                .emailAddress("test@example.com")
//                .firstName("John")
//                .profilePictureUrl("/uploads/profile-pictures/test.jpg")
//                .build();
//
//        file = new MockMultipartFile(
//                "file",
//                "test.jpg",
//                "image/jpeg",
//                "test image content".getBytes()
//        );
//    }
//
/// /    @Test
/// /    void uploadProfilePicture_validFile_updatesUserAndReturnsDTO() throws IOException {
/// /        // Arrange
/// /        when(userRepository.findByEmailAddress("test@example.com")).thenReturn(Optional.of(user));
/// /        when(userRepository.save(any(User.class))).thenReturn(user);
/// /        when(userMapper.toDto(user)).thenReturn(userDto);
/// /
/// /        // Act
/// /        UserDto result = userService.uploadProfilePicture("test@example.com", file);
/// /
/// /        // Assert
/// /        assertNotNull(result);
/// /        assertEquals(userDto.getEmailAddress(), result.getEmailAddress());
/// /        assertTrue(result.getProfilePictureUrl().startsWith("/uploads/profile-pictures/"));
/// /        assertTrue(Files.exists(tempDir.resolve("uploads/profile-pictures").resolve(result.getProfilePictureUrl().substring(24))));
/// /
/// /        verify(userRepository).findByEmailAddress("test@example.com");
/// /        verify(userRepository).save(any(User.class));
/// /        verify(userMapper).toDto(user);
/// /    }
//
////    @Test
////    void uploadProfilePicture_userNotFound_throwsEntityNotFoundException() {
////        // Arrange
////        when(userRepository.findByEmailAddress("test@example.com")).thenReturn(Optional.empty());
////
////        // Act & Assert
////        assertThrows(EntityNotFoundException.class, () -> {
////            userService.uploadProfilePicture("test@example.com", file);
////        });
////
////        verify(userRepository).findByEmailAddress("test@example.com");
////        verify(userRepository, never()).save(any(User.class));
////    }
//
////    @Test
////    void uploadProfilePicture_fileSaveFails_throwsRuntimeException() throws IOException {
////        // Arrange
////        when(userRepository.findByEmailAddress("test@example.com")).thenReturn(Optional.of(user));
////
////        // Simulate IO failure by making directory read-only (on Unix-like systems)
////        Path uploadDir = tempDir.resolve("uploads/profile-pictures");
////        Files.createDirectories(uploadDir);
////        uploadDir.toFile().setReadOnly();
////
////        // Act & Assert
////        assertThrows(RuntimeException.class, () -> {
////            userService.uploadProfilePicture("test@example.com", file);
////        });
////
////        verify(userRepository).findByEmailAddress("test@example.com");
////        verify(userRepository, never()).save(any(User.class));
////    }
////
//
////    @Test
////    public void testSaveNewClient() {
////        // Given
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .firstName("nom")
////                .confirmPassword("confirmPassword").build();
////
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .resetToken("resetToken")
////                .firstName("nom")
////                .build();
////        when(utility.convertClientRequestToClient(any(RegisterRequest.class), isNull())).thenReturn(mockUser);
////        when(userRepository.save(any())).thenReturn(mockUser);
////        var result = userService.saveNewUser(registerRequest);
////        assertNotNull(result);
////    }
//
//
////    @Test
////    public void testSaveNewClient_WhenSavingWithError() {
////        RegisterRequest registerRequest = RegisterRequest.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .firstName("nom")
////                .confirmPassword("confirmPassword").build();
////
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .resetToken("resetToken")
////                .firstName("nom")
////                .build();
////        when(utility.convertClientRequestToClient(any(RegisterRequest.class), isNull())).thenReturn(mockUser);
////        doThrow(new RuntimeException("Error saving new client")).when(userRepository).save(any());
////        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
////            userService.saveNewUser(registerRequest);
////        });
////        assertEquals("Error saving new client", exception.getMessage());
////
////    }
//
////    @Test
////    public void testSaveExistingClient() {
////        // Given
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .resetToken("resetToken")
////                .firstName("nom")
////                .build();
////        when(userRepository.save(any())).thenReturn(mockUser);
////        var result = userService.saveExistingUser(mockUser);
////        assertNotNull(result);
////        Assertions.assertEquals(mockUser.getEmailAddress(), result.getEmailAddress());
////        Assertions.assertEquals(mockUser.getFirstName(), result.getFirstName());
////    }
//
////    @Test
////    public void testSaveExistingClient_WhenSavingWithError() {
////        // Given
////        User mockUser = User.builder()
////                .emailAddress("email")
////                .password(passwordEncoder.encode("password"))
////                .resetToken("resetToken")
////                .firstName("nom")
////                .build();
////        doThrow(new RuntimeException("Error saving existing client")).when(userRepository).save(any());
////        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
////            userService.saveExistingUser(mockUser);
////        });
////        assertEquals("Error saving existing client", exception.getMessage());
////    }
//
//    @Test
//    public void testUpdateClient() {
//        // Given
//        RegisterRequest registerRequest = RegisterRequest.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .firstName("nom")
//                .confirmPassword("confirmPassword").build();
//
//        User mockUser = User.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .resetToken("resetToken")
//                .firstName("nom")
//                .build();
//        when(utility.convertClientRequestToClient(any(RegisterRequest.class), any())).thenReturn(mockUser);
//        when(userRepository.save(any())).thenReturn(mockUser);
//        var result = userService.updateUserPassword(registerRequest, 1L);
//        assertNotNull(result);
//    }
//
//    @Test
//    public void testUpdateClient_WhenUpdatingWithError() {
//        // Given
//        RegisterRequest registerRequest = RegisterRequest.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .firstName("nom")
//                .confirmPassword("confirmPassword").build();
//
//        User mockUser = User.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .resetToken("resetToken")
//                .firstName("nom")
//                .build();
//        when(utility.convertClientRequestToClient(any(RegisterRequest.class), any())).thenReturn(mockUser);
//        doThrow(new RuntimeException("Error updating client")).when(userRepository).save(any());
//        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            userService.updateUserPassword(registerRequest, 1L);
//        });
//        assertEquals("Error updating client", exception.getMessage());
//    }
//
//    @Test
//    public void testDeleteById() {
//        // Given
//        Long id = 1L;
//        // When
//        doNothing().when(userRepository).deleteById(id);
//        userService.deleteById(id);
//    }
//
//    @Test
//    public void testDeleteById_WhenDeletingWithError() {
//        // Given
//        Long id = 1L;
//        // When
//        doThrow(new RuntimeException("Error deleting client")).when(userRepository).deleteById(id);
//        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            userService.deleteById(id);
//        });
//        assertEquals("Error deleting client", exception.getMessage());
//    }
//
//    @Test
//    public void testFindAll() {
//        // Given
//        User mockUser = User.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .resetToken("resetToken")
//                .firstName("nom")
//                .build();
//        when(userRepository.findAll()).thenReturn(List.of(mockUser));
//        var result = userService.findAll();
//        assertNotNull(result);
//        Assertions.assertEquals(1, result.size());
//    }
//
//    @Test
//    public void testFindAll_WhenFindingWithError() {
//        // Given
//        doThrow(new RuntimeException("Error finding all clients")).when(userRepository).findAll();
//        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            userService.findAll();
//        });
//        assertEquals("Error finding all clients", exception.getMessage());
//    }
//
//    @Test
//    public void testFindById() {
//        // Given
//        Long id = 1L;
//        User mockUser = User.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .resetToken("resetToken")
//                .firstName("nom")
//                .build();
//        when(userRepository.findById(id)).thenReturn(java.util.Optional.of(mockUser));
//        var result = userService.findById(id);
//        assertNotNull(result);
//    }
//
//    @Test
//    public void testFindById_WhenFindingWithError() {
//        // Given
//        Long id = 1L;
//        doThrow(new RuntimeException("Error finding client by ID")).when(userRepository).findById(id);
//        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            userService.findById(id);
//        });
//        assertEquals("Error finding client by ID", exception.getMessage());
//    }
//
//    @Test
//    public void testFindByEmail() {
//        // Given
//        String email = "email";
//        User mockUser = User.builder()
//                .emailAddress("email")
//                .password(passwordEncoder.encode("password"))
//                .resetToken("resetToken")
//                .firstName("nom")
//                .build();
//        when(userRepository.findByEmailAddress(email)).thenReturn(java.util.Optional.of(mockUser));
//        var result = userService.findByEmail(email);
//        assertNotNull(result);
//    }
//
//    @Test
//    public void testFindByEmail_WhenFindingWithError() {
//        // Given
//        String email = "email";
//        doThrow(new RuntimeException("Error finding client by email")).when(userRepository).findByEmailAddress(email);
//        Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
//            userService.findByEmail(email);
//        });
//        assertEquals("Error finding client by email", exception.getMessage());
//    }
//
//
//}
