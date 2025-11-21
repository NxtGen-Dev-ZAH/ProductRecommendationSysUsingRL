package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class AdminUserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private String jwtToken;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        // Clear database
        userRepository.deleteAll();

        // Create test user
        user = User.builder()
                .emailAddress("test@example.com")
                .password("$2a$10$exampleEncodedPassword") // BCrypt encoded
                .firstName("John")
                .isActivated(true)
                .build();
        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String loginRequest = "{\"emailAddress\":\"test@example.com\",\"password\":\"password\"}";
        HttpEntity<String> loginEntity = new HttpEntity<>(loginRequest, headers);
        ResponseEntity<String> loginResponse = restTemplate.postForEntity("/auth/login", loginEntity, String.class);

        // Extract JWT token (adjust based on your response structure)
        jwtToken = objectMapper.readTree(loginResponse.getBody()).get("token").asText();

        // Create uploads directory
        Path uploadDir = Paths.get("uploads/profile-pictures");
        Files.createDirectories(uploadDir);
    }

//    @Test
//    void uploadProfilePicture_authenticatedUser_success() throws Exception {
//        // Arrange
//        File tempFile = File.createTempFile("test", ".jpg");
//        Files.write(tempFile.toPath(), "test image content".getBytes());
//        FileSystemResource resource = new FileSystemResource(tempFile);
//
//        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add("file", resource);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        headers.setBearerAuth(jwtToken);
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//        // Act
//        ResponseEntity<UserDto> response = restTemplate.exchange(
//                "/api/users/profile-picture",
//                HttpMethod.POST,
//                requestEntity,
//                UserDto.class
//        );
//
//        // Assert
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        UserDto result = response.getBody();
//        assertNotNull(result);
//        assertEquals("test@example.com", result.getEmailAddress());
//        assertTrue(result.getProfilePictureUrl().startsWith("/uploads/profile-pictures/"));
//        assertTrue(Files.exists(Paths.get("." + result.getProfilePictureUrl())));
//
//        // Verify database
//        User updatedUser = userRepository.findByEmailAddress("test@example.com").orElseThrow();
//        assertEquals(result.getProfilePictureUrl(), updatedUser.getProfilePictureUrl());
//    }

//    @Test
//    void uploadProfilePicture_unauthenticatedUser_returnsUnauthorized() {
//        // Arrange
//        File tempFile = new File("test.jpg");
//        FileSystemResource resource = new FileSystemResource(tempFile);
//
//        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//        body.add("file", resource);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
//        // No JWT token
//        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
//
//        // Act
//        ResponseEntity<String> response = restTemplate.exchange(
//                "/api/users/profile-picture",
//                HttpMethod.POST,
//                requestEntity,
//                String.class
//        );
//
//        // Assert
//        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
//    }
}