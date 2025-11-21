package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.dto.AuditLogDto;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import com.datasaz.ecommerce.services.interfaces.IAdminUserService;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminUserControllerTest {

    @InjectMocks
    private AdminUserController adminUserController;

    @Mock
    private IAdminUserService adminUserService;

    @Mock
    private IAuditLogService auditLogService;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .isActivated(true)
                .isBlocked(false)
                .deleted(false)
                .build();
    }

    @Test
    void getAllUsers_returnsPagedUsers() {
        Page<UserDto> page = new PageImpl<>(Collections.singletonList(userDto));
        when(adminUserService.findAll(0, 10)).thenReturn(page);

        ResponseEntity<Page<UserDto>> response = adminUserController.getAllUsers(0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
        assertEquals(userDto, response.getBody().getContent().get(0));
    }

    @Test
    void getUserById_returnsUserDto() {
        when(adminUserService.findById(1L)).thenReturn(Optional.of(userDto));

        ResponseEntity<UserDto> response = adminUserController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDto, response.getBody());
    }

    @Test
    void getUserById_returnsNotFound() {
        when(adminUserService.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<UserDto> response = adminUserController.getUserById(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getUserByEmail_returnsUserDto() {
        when(adminUserService.findByEmail("test@example.com")).thenReturn(Optional.of(userDto));

        ResponseEntity<UserDto> response = adminUserController.getUserByEmail("test@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDto, response.getBody());
    }

    @Test
    void getUserByEmail_returnsNotFound() {
        when(adminUserService.findByEmail("test@example.com")).thenReturn(Optional.empty());

        ResponseEntity<UserDto> response = adminUserController.getUserByEmail("test@example.com");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteUser_deletesUser() {
        doNothing().when(adminUserService).deleteById(1L);

        ResponseEntity<Void> response = adminUserController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(adminUserService).deleteById(1L);
    }

    @Test
    void updateUserPassword_updatesPassword() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder().build();
        request.setEmail("test@example.com");
        request.setNewPassword("newPassword123");
        when(adminUserService.updateUserPassword(request)).thenReturn(userDto);

        ResponseEntity<UserDto> response = adminUserController.updateUserPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDto, response.getBody());
    }

    @Test
    void updateUserPassword_throwsBadRequestExceptionForNullFields() {
        UpdatePasswordRequest request = UpdatePasswordRequest.builder().build();
        request.setEmail(null);
        request.setNewPassword(null);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserController.updateUserPassword(request));
        assertEquals("Email and new password are required", exception.getMessage());
    }

    @Test
    void restoreUser_restoresUser() {
        when(adminUserService.restoreUser("test@example.com")).thenReturn(userDto);

        ResponseEntity<UserDto> response = adminUserController.restoreUser("test@example.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userDto, response.getBody());
    }

    @Test
    void blockUser_blocksUser() {
        Map<String, String> request = Map.of("email", "test@example.com", "reason", "Test reason");
        when(adminUserService.blockUser("test@example.com", "Test reason")).thenReturn("User blocked successfully.");

        ResponseEntity<String> response = adminUserController.blockUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User blocked successfully.", response.getBody());
    }

    @Test
    void unblockUser_unblocksUser() {
        Map<String, String> request = Map.of("email", "test@example.com");
        when(adminUserService.unblockUser("test@example.com")).thenReturn("User unblocked successfully.");

        ResponseEntity<String> response = adminUserController.unblockUser(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User unblocked successfully.", response.getBody());
    }

    @Test
    void getAuditLogs_returnsAuditLogs() {
        Page<AuditLogDto> page = new PageImpl<>(Collections.emptyList());
        when(auditLogService.getAuditLogs(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<Page<AuditLogDto>> response = adminUserController.getAuditLogs(0, 10, null, null, null, null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(page, response.getBody());
    }
}