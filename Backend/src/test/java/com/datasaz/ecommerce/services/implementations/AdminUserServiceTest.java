package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtBlacklistService jwtBlacklistService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User user;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .password("encodedPassword")
                .isActivated(true)
                .isBlocked(false)
                .deleted(false)
                .build();

        userDto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .isActivated(true)
                .isBlocked(false)
                .deleted(false)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void findAll_returnsPagedUsers() {
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        Page<UserDto> userDtoPage = new PageImpl<>(Collections.singletonList(userDto));
        when(userRepository.findAllWithAllCollections(any(PageRequest.class))).thenReturn(userPage);
        when(userMapper.toDto(user)).thenReturn(userDto);

        Page<UserDto> result = adminUserService.findAll(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(userDto, result.getContent().get(0));
        verify(userRepository).findAllWithAllCollections(any(PageRequest.class));
        verify(userMapper).toDto(user);
    }

    @Test
    void findAll_throwsBadRequestExceptionOnError() {
        when(userRepository.findAllWithAllCollections(any(PageRequest.class))).thenThrow(new RuntimeException("DB error"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserService.findAll(0, 10));
        assertEquals("Error finding all users.", exception.getMessage());
    }

    @Test
    void findById_returnsUserDto() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        Optional<UserDto> result = adminUserService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(userDto, result.get());
        verify(userRepository).findByIdWithAllCollections(1L);
    }

    @Test
    void findById_throwsIllegalArgumentExceptionForNullId() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminUserService.findById(null));
        assertEquals("ID cannot be null", exception.getMessage());
    }

    @Test
    void findById_throwsUserNotFoundException() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserService.findById(1L));
        assertEquals("User not found with id: 1", exception.getMessage());
    }

    @Test
    void findByEmail_returnsUserDto() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        Optional<UserDto> result = adminUserService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(userDto, result.get());
        verify(userRepository).findByEmailAddressWithAllCollections("test@example.com");
    }

    @Test
    void findByEmail_throwsBadRequestExceptionForInvalidEmail() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserService.findByEmail("invalid-email"));
        assertTrue(exception.getMessage().contains("Invalid email"));
    }

    @Test
    void findByEmail_throwsUserNotFoundException() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserService.findByEmail("test@example.com"));
        assertEquals("User not found with email: test@example.com", exception.getMessage());
    }

    @Test
    void deleteById_deletesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adminUserService.deleteById(1L);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("DELETE_USER_ADMIN"), any(), any());
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
        assertTrue(user.getDeleted());
        assertFalse(user.getIsActivated());
        assertNull(user.getDeletionToken());
    }

    @Test
    void deleteById_throwsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserService.deleteById(1L));
        assertEquals("User not found with id: 1", exception.getMessage());
    }

    @Test
    void deleteById_throwsRuntimeExceptionForAlreadyDeleted() {
        user.setDeleted(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminUserService.deleteById(1L));
        assertEquals("User is already deleted", exception.getMessage());
    }

    @Test
    void updateUserPassword_updatesPassword() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder().build();
        request.setEmail("test@example.com");
        request.setNewPassword("newPassword123");
        when(userRepository.findByEmailAddressAndDeletedFalse("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserService.updateUserPassword(request);

        assertEquals(userDto, result);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("UPDATE_PASSWORD_ADMIN"), any(), any());
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void updateUserPassword_throwsBadRequestExceptionForShortPassword() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder().build();
        request.setEmail("test@example.com");
        request.setNewPassword("short");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserService.updateUserPassword(request));
        assertEquals("New password must be at least 8 characters", exception.getMessage());
    }

    @Test
    void restoreUser_restoresUser() {
        user.setDeleted(true);
        when(userRepository.findByEmailAddress("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserService.restoreUser("test@example.com");

        assertEquals(userDto, result);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("RESTORE_USER"), any(), any());
        assertFalse(user.getDeleted());
        assertTrue(user.getIsActivated());
        assertNull(user.getDeletionToken());
    }

    @Test
    void blockUser_blocksUser() {
        when(userRepository.findByEmailAddressAndDeletedFalse("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String result = adminUserService.blockUser("test@example.com", "Test reason");

        assertEquals("User blocked successfully.", result);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("BLOCK_USER"), any(), any());
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
        assertTrue(user.getIsBlocked());
    }

    @Test
    void unblockUser_unblocksUser() {
        user.setIsBlocked(true);
        when(userRepository.findByEmailAddressAndDeletedFalse("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        String result = adminUserService.unblockUser("test@example.com");

        assertEquals("User unblocked successfully.", result);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("UNBLOCK_USER"), any(), any());
        assertFalse(user.getIsBlocked());
    }
}