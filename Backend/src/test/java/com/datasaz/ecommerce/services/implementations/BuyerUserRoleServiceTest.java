package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private BuyerUserRoleService buyerUserRoleService;

    private User user;
    private Roles sellerRole;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .userRoles(new HashSet<>())
                .build();
        sellerRole = Roles.builder()
                .id(1L)
                .role(RoleTypes.SELLER)
                .build();
        // Ensure emailService mock does not throw MessagingException by default
        //doNothing().when(emailService).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void becomeIndividualSeller_Success() throws MessagingException {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(auditLogService).logAction(anyString(), anyString(), anyString());
        doNothing().when(refreshTokenRepository).deleteByUserEmail(anyString());

        // Act
        buyerUserRoleService.becomeIndividualSeller();

        // Assert
        verify(userRepository).save(user);
        verify(auditLogService).logAction("test@example.com", "BECOME_INDIVIDUAL_SELLER", "User assigned SELLER role");
        try {
            verify(emailService).sendEmail("test@example.com", "Assigned Individual Seller Role", "You have been assigned the SELLER role.");
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
        assertTrue(user.getUserRoles().contains(sellerRole));
    }

    @Test
    void becomeIndividualSeller_AlreadyHasSellerRole_NoAction() throws MessagingException {
        // Arrange
        user.getUserRoles().add(sellerRole);
        when(currentUserService.getCurrentUser()).thenReturn(user);

        // Act
        buyerUserRoleService.becomeIndividualSeller();

        // Assert
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogService, never()).logAction(anyString(), anyString(), anyString());
        try {
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        verify(refreshTokenRepository, never()).deleteByUserEmail(anyString());
    }

    @Test
    void becomeIndividualSeller_UserNotFound_ThrowsException() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenThrow(UserNotFoundException.builder().message("User not found").build());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> buyerUserRoleService.becomeIndividualSeller());
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogService, never()).logAction(anyString(), anyString(), anyString());
        try {
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        verify(refreshTokenRepository, never()).deleteByUserEmail(anyString());
    }

    @Test
    void becomeIndividualSeller_RoleRepositoryError_ThrowsBadRequestException() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> buyerUserRoleService.becomeIndividualSeller());
        assertEquals("Error updating user.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verify(auditLogService, never()).logAction(anyString(), anyString(), anyString());
        try {
            verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        verify(refreshTokenRepository, never()).deleteByUserEmail(anyString());
    }

    @Test
    void becomeIndividualSeller_NewRoleCreated_Success() throws MessagingException {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.empty());
        when(rolesRepository.save(any(Roles.class))).thenReturn(sellerRole);
        when(userRepository.save(any(User.class))).thenReturn(user);
        doNothing().when(auditLogService).logAction(anyString(), anyString(), anyString());
        doNothing().when(refreshTokenRepository).deleteByUserEmail(anyString());

        // Act
        buyerUserRoleService.becomeIndividualSeller();

        // Assert
        verify(rolesRepository).save(argThat(role -> role.getRole() == RoleTypes.SELLER));
        verify(userRepository).save(user);
        verify(auditLogService).logAction("test@example.com", "BECOME_INDIVIDUAL_SELLER", "User assigned SELLER role");
        try {
            verify(emailService).sendEmail("test@example.com", "Assigned Individual Seller Role", "You have been assigned the SELLER role.");
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
        assertTrue(user.getUserRoles().contains(sellerRole));
    }

    @Test
    void becomeIndividualSeller_EmailServiceThrowsMessagingException_ThrowsBadRequestException() throws MessagingException {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        doThrow(new MessagingException("Email service error")).when(emailService).sendEmail(anyString(), anyString(), anyString());
        doNothing().when(auditLogService).logAction(anyString(), anyString(), anyString());

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class, () -> buyerUserRoleService.becomeIndividualSeller());
        assertEquals("Error updating user.", exception.getMessage());
        verify(userRepository).save(user);
        verify(auditLogService).logAction("test@example.com", "BECOME_INDIVIDUAL_SELLER", "User assigned SELLER role");
        try {
            verify(emailService).sendEmail("test@example.com", "Assigned Individual Seller Role", "You have been assigned the SELLER role.");
        } catch (MessagingException e) {
            fail("MessagingException should not be thrown by mock");
        }
        // Do not verify refreshTokenRepository.deleteByUserEmail, as it is not called due to the exception
        assertTrue(user.getUserRoles().contains(sellerRole));
    }
}
