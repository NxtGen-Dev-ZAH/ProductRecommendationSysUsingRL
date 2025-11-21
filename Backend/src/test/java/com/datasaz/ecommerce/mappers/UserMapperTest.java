package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.CompanyRepository;
import com.datasaz.ecommerce.repositories.ProductRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserMapperTest {

    @InjectMocks
    private UserMapper userMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void toDto_mapsBase64ImageContentAndType() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        User user = User.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .imageContent(imageBytes)
                .imageContentType("image/jpeg")
                .build();

        when(userRepository.countFollowersByEmailAddress("test@example.com")).thenReturn(0L);
        when(userRepository.countFollowingByEmailAddress("test@example.com")).thenReturn(0L);

        UserDto dto = userMapper.toDto(user);

        assertEquals(1L, dto.getId());
        assertEquals("test@example.com", dto.getEmailAddress());
        assertEquals(base64Image, dto.getImageContent());
        assertEquals("image/jpeg", dto.getImageContentType());
    }

    @Test
    void toEntity_decodesBase64ImageContent() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        UserDto dto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .imageContent(base64Image)
                .imageContentType("image/jpeg")
                .favoriteProductIds(Collections.emptySet())
                .followingIds(Collections.emptySet())
                .followerIds(Collections.emptySet())
                .userRoles(Collections.emptySet())
                .build();

        User user = userMapper.toEntity(dto);

        assertEquals(1L, user.getId());
        assertEquals("test@example.com", user.getEmailAddress());
        assertArrayEquals(imageBytes, user.getImageContent());
        assertEquals("image/jpeg", user.getImageContentType());
    }

    @Test
    void toSummaryResponse_mapsBase64ImageContentAndType() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        User user = User.builder()
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .profilePictureUrl("http://example.com/profile.jpg")
                .imageContent(imageBytes)
                .imageContentType("image/jpeg")
                .build();

        UserSummaryResponse response = userMapper.toSummaryResponse(user);

        assertEquals("test@example.com", response.getEmailAddress());
        assertEquals("John Doe", response.getDisplayName());
        assertEquals("http://example.com/profile.jpg", response.getProfilePictureUrl());
        assertEquals(base64Image, response.getImageContent());
        assertEquals("image/jpeg", response.getImageContentType());
    }

    @Test
    void toProfileResponse_mapsBase64ImageContentAndType() {
        byte[] imageBytes = new byte[]{1, 2, 3};
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        User user = User.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .profilePictureUrl("http://example.com/profile.jpg")
                .imageContent(imageBytes)
                .imageContentType("image/jpeg")
                .favoriteProducts(Collections.emptySet())
                .followers(Collections.emptySet())
                .following(Collections.emptySet())
                .userRoles(Collections.emptySet())
                .build();

        UserProfileResponse response = userMapper.toProfileResponse(user);

        assertEquals(1L, response.getId());
        assertEquals("test@example.com", response.getEmailAddress());
        assertEquals("http://example.com/profile.jpg", response.getProfilePictureUrl());
        assertEquals(base64Image, response.getImageContent());
        assertEquals("image/jpeg", response.getImageContentType());
    }

    @Test
    void toDto_nullUser_returnsNull() {
        UserDto dto = userMapper.toDto(null);
        assertNull(dto);
    }

    @Test
    void toEntity_nullDto_returnsNull() {
        User user = userMapper.toEntity(null);
        assertNull(user);
    }

    @Test
    void toSummaryResponse_nullUser_returnsNull() {
        UserSummaryResponse response = userMapper.toSummaryResponse(null);
        assertNull(response);
    }

    @Test
    void toProfileResponse_nullUser_returnsNull() {
        UserProfileResponse response = userMapper.toProfileResponse(null);
        assertNull(response);
    }

    @Test
    void toEntity_invalidBase64_throwsException() {
        UserDto dto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .imageContent("invalid-base64-string")
                .imageContentType("image/jpeg")
                .favoriteProductIds(Collections.emptySet())
                .followingIds(Collections.emptySet())
                .followerIds(Collections.emptySet())
                .userRoles(Collections.emptySet())
                .build();

        assertThrows(IllegalArgumentException.class, () -> userMapper.toEntity(dto));
    }
}