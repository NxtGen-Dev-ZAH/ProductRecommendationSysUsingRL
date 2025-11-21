package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.request.UserProfileRequest;
import com.datasaz.ecommerce.models.response.ProfilePictureResponse;
import com.datasaz.ecommerce.services.interfaces.IUserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserProfileControllerTest {

    @Mock
    private IUserProfileService userProfileService;

    @InjectMocks
    private UserProfileController userProfileController;

    private String email;
    private byte[] testImage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        email = "test@example.com";
        testImage = new byte[]{1, 2, 3}; // Replace with actual image data in real tests
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }

    @Test
    void testUploadProfilePicture_MultipartFile_Success() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", testImage);
        String profilePictureUrl = "/Uploads/profile-pictures/test.jpg";
        when(userProfileService.uploadProfilePicture(eq(image), eq(email))).thenReturn(profilePictureUrl);

        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(image, null);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(profilePictureUrl, response.getBody().getProfilePictureUrl());
        assertEquals("Profile picture uploaded successfully", response.getBody().getMessage());
        verify(userProfileService).uploadProfilePicture(image, email);
        verify(userProfileService, never()).uploadProfilePicture(any(UserProfileRequest.class), eq(email));
    }

    @Test
    void testUploadProfilePicture_UserProfileRequest_FileSystemMode_Success() {
        String base64Image = Base64.getEncoder().encodeToString(testImage);
        UserProfileRequest request = UserProfileRequest.builder()
                .profilePictureBase64(base64Image)
                .build();
        String profilePictureUrl = "/Uploads/profile-pictures/test.jpg";
        when(userProfileService.uploadProfilePicture(eq(request), eq(email))).thenReturn(profilePictureUrl);

        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(null, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(profilePictureUrl, response.getBody().getProfilePictureUrl());
        assertEquals("Profile picture uploaded successfully", response.getBody().getMessage());
        verify(userProfileService).uploadProfilePicture(request, email);
        verify(userProfileService, never()).uploadProfilePicture(any(MultipartFile.class), eq(email));
    }

    @Test
    void testUploadProfilePicture_UserProfileRequest_DatabaseMode_Success() {
        String base64Image = Base64.getEncoder().encodeToString(testImage);
        UserProfileRequest request = UserProfileRequest.builder()
                .profilePictureBase64(base64Image)
                .build();
        when(userProfileService.uploadProfilePicture(eq(request), eq(email))).thenReturn(null);

        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(null, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("", response.getBody().getProfilePictureUrl());
        assertEquals("Profile picture uploaded successfully", response.getBody().getMessage());
        verify(userProfileService).uploadProfilePicture(request, email);
        verify(userProfileService, never()).uploadProfilePicture(any(MultipartFile.class), eq(email));
    }

    @Test
    void testUploadProfilePicture_NullImageAndRequest_ThrowsBadRequest() {
        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(null, null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or Base64 image content is required", response.getBody().getMessage());
        assertNull(response.getBody().getProfilePictureUrl());
        verifyNoInteractions(userProfileService);
    }

    @Test
    void testUploadProfilePicture_EmptyImage_ThrowsBadRequest() {
        MockMultipartFile emptyImage = new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[0]);

        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(emptyImage, null);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or Base64 image content is required", response.getBody().getMessage());
        assertNull(response.getBody().getProfilePictureUrl());
        verifyNoInteractions(userProfileService);
    }

    @Test
    void testUploadProfilePicture_EmptyUserProfileRequestImageContent_ThrowsBadRequest() {
        UserProfileRequest emptyRequest = UserProfileRequest.builder()
                .profilePictureBase64("")
                .build();

        ResponseEntity<ProfilePictureResponse> response = userProfileController.uploadProfilePicture(null, emptyRequest);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Image file or Base64 image content is required", response.getBody().getMessage());
        assertNull(response.getBody().getProfilePictureUrl());
        verifyNoInteractions(userProfileService);
    }

    @Test
    void testUploadProfilePicture_UserNotFound_ThrowsUserNotFoundException() {
        MockMultipartFile image = new MockMultipartFile("image", "test.jpg", "image/jpeg", testImage);
        when(userProfileService.uploadProfilePicture(eq(image), eq(email)))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: " + email).build());

        assertThrows(UserNotFoundException.class, () -> {
            userProfileController.uploadProfilePicture(image, null);
        });
        verify(userProfileService).uploadProfilePicture(image, email);
        verify(userProfileService, never()).uploadProfilePicture(any(UserProfileRequest.class), eq(email));
    }

    @Test
    void testUploadProfilePicture_UserProfileRequest_UserNotFound_ThrowsUserNotFoundException() {
        String base64Image = Base64.getEncoder().encodeToString(testImage);
        UserProfileRequest request = UserProfileRequest.builder()
                .profilePictureBase64(base64Image)
                .build();
        when(userProfileService.uploadProfilePicture(eq(request), eq(email)))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: " + email).build());

        assertThrows(UserNotFoundException.class, () -> {
            userProfileController.uploadProfilePicture(null, request);
        });
        verify(userProfileService).uploadProfilePicture(request, email);
        verify(userProfileService, never()).uploadProfilePicture(any(MultipartFile.class), eq(email));
    }
}