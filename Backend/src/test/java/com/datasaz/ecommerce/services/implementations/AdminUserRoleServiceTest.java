package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserRoleServiceTest {

    @InjectMocks
    private AdminUserRoleService adminUserRoleService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private User user;
    private UserDto userDto;
    private Roles role;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .isActivated(true)
                .isBlocked(false)
                .deleted(false)
                .userRoles(new HashSet<>())
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

        role = Roles.builder()
                .id(1L)
                .role(RoleTypes.SELLER)
                .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("admin@example.com");
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addUserRole_addsRoleSuccessfully() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.addUserRole(1L, "SELLER");

        assertEquals(userDto, result);
        assertTrue(user.getUserRoles().contains(role));
        verify(userRepository).findByIdWithAllCollections(1L);
        verify(rolesRepository).findByRole(RoleTypes.SELLER);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("ADD_USER_ROLE"), anyString(), anyString());
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void addUserRole_throwsUserNotFoundException() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserRoleService.addUserRole(1L, "SELLER"));
        assertEquals("User not found with id: 1", exception.getMessage());
    }

    @Test
    void addUserRole_throwsIllegalParameterExceptionForInvalidRole() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));

        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.addUserRole(1L, "INVALID_ROLE"));
        assertEquals("Invalid role: INVALID_ROLE", exception.getMessage());
    }

    @Test
    void addUserRole_throwsBadRequestExceptionForExistingRole() {
        user.getUserRoles().add(role);
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserRoleService.addUserRole(1L, "SELLER"));
        assertEquals("User already has role SELLER", exception.getMessage());
    }

    @Test
    void addUserRole_throwsBadRequestExceptionOnSaveError() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB error"));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserRoleService.addUserRole(1L, "SELLER"));
        assertEquals("Error updating user.", exception.getMessage());
    }

    @Test
    void removeUserRole_removesRoleSuccessfully() {
        user.getUserRoles().add(role);
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeUserRole(1L, "SELLER");

        assertEquals(userDto, result);
        assertFalse(user.getUserRoles().contains(role));
        verify(userRepository).findByIdWithAllCollections(1L);
        verify(rolesRepository).findByRole(RoleTypes.SELLER);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("REMOVE_USER_ROLE"), anyString(), anyString());
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void removeUserRole_throwsUserNotFoundException() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
        assertEquals("User not found with id: 1", exception.getMessage());
    }

    @Test
    void removeUserRole_throwsIllegalParameterExceptionForInvalidRole() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));

        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.removeUserRole(1L, "INVALID_ROLE"));
        assertEquals("Invalid role: INVALID_ROLE", exception.getMessage());
    }

    @Test
    void removeUserRole_throwsResourceNotFoundExceptionForMissingRole() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
        assertEquals("Role not found: SELLER", exception.getMessage());
    }

    @Test
    void removeUserRole_throwsBadRequestExceptionForNonExistingRole() {
        when(userRepository.findByIdWithAllCollections(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
        assertEquals("User does not have role: SELLER", exception.getMessage());
    }

    @Test
    void getUserRoles_returnsRoles() {
        Set<Roles> roles = new HashSet<>();
        roles.add(role);
        when(userRepository.findRolesByUserId(1L)).thenReturn(roles);

        Set<Roles> result = adminUserRoleService.getUserRoles(1L);

        assertEquals(roles, result);
        verify(userRepository).findRolesByUserId(1L);
    }

    @Test
    void assignSellerRole_assignsRoleSuccessfully() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.assignSellerRole("test@example.com");

        assertEquals(userDto, result);
        assertTrue(user.getUserRoles().contains(role));
        verify(userRepository).findByEmailAddressWithAllCollections("test@example.com");
        verify(rolesRepository).findByRole(RoleTypes.SELLER);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("ASSIGN_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq("test@example.com"), eq("SELLER"), eq("ASSIGN_ROLE"), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void removeSellerRole_removesRoleSuccessfully() {
        user.getUserRoles().add(role);
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeSellerRole("test@example.com");

        assertEquals(userDto, result);
        assertFalse(user.getUserRoles().contains(role));
        verify(userRepository).findByEmailAddressWithAllCollections("test@example.com");
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("REMOVE_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq("test@example.com"), eq("SELLER"), eq("REMOVE_ROLE"), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void assignRole_assignsRoleSuccessfully() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.assignRole("test@example.com", "SELLER");

        assertEquals(userDto, result);
        assertTrue(user.getUserRoles().contains(role));
        verify(userRepository).findByEmailAddressWithAllCollections("test@example.com");
        verify(rolesRepository).findByRole(RoleTypes.SELLER);
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("ASSIGN_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq("test@example.com"), eq("SELLER"), eq("ASSIGN_ROLE"), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void assignRole_throwsIllegalParameterExceptionForInvalidEmail() {
        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.assignRole("invalid-email", "SELLER"));
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void assignRole_throwsIllegalParameterExceptionForInvalidRole() {
        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.assignRole("test@example.com", "INVALID_ROLE"));
        assertEquals("Invalid role: INVALID_ROLE", exception.getMessage());
    }

    @Test
    void assignRole_throwsUserNotFoundException() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserRoleService.assignRole("test@example.com", "SELLER"));
        assertEquals("User not found with email: test@example.com", exception.getMessage());
    }

    @Test
    void assignRole_throwsBadRequestExceptionForExistingRole() {
        user.getUserRoles().add(role);
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserRoleService.assignRole("test@example.com", "SELLER"));
        assertEquals("User already has role SELLER", exception.getMessage());
    }

    @Test
    void removeRole_removesRoleSuccessfully() {
        user.getUserRoles().add(role);
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeRole("test@example.com", "SELLER");

        assertEquals(userDto, result);
        assertFalse(user.getUserRoles().contains(role));
        verify(userRepository).findByEmailAddressWithAllCollections("test@example.com");
        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq("test@example.com"), eq("REMOVE_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq("test@example.com"), eq("SELLER"), eq("REMOVE_ROLE"), any(LocalDateTime.class));
        verify(refreshTokenRepository).deleteByUserEmail("test@example.com");
    }

    @Test
    void removeRole_throwsIllegalParameterExceptionForInvalidEmail() {
        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.removeRole("invalid-email", "SELLER"));
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    void removeRole_throwsIllegalParameterExceptionForInvalidRole() {
        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.removeRole("test@example.com", "INVALID_ROLE"));
        assertEquals("Invalid role: INVALID_ROLE", exception.getMessage());
    }

    @Test
    void removeRole_throwsIllegalParameterExceptionForBuyerRole() {
        IllegalParameterException exception = assertThrows(IllegalParameterException.class,
                () -> adminUserRoleService.removeRole("test@example.com", "BUYER"));
        assertEquals("Cannot remove BUYER role", exception.getMessage());
    }

    @Test
    void removeRole_throwsUserNotFoundException() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                () -> adminUserRoleService.removeRole("test@example.com", "SELLER"));
        assertEquals("User not found with email: test@example.com", exception.getMessage());
    }

    @Test
    void removeRole_throwsBadRequestExceptionForNonExistingRole() {
        when(userRepository.findByEmailAddressWithAllCollections("test@example.com")).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> adminUserRoleService.removeRole("test@example.com", "SELLER"));
        assertEquals("User does not have role SELLER", exception.getMessage());
    }
}



/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AdminUserRoleService adminUserRoleService;

    private User user;
    private UserDto userDto;
    private Roles sellerRole;
    private String email = "test@example.com";
    private String roleName = "SELLER";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress(email);
        user.setUserRoles(new HashSet<>());

        userDto = UserDto.builder()
                .id(1L)
                .emailAddress(email)
                .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .build();

        sellerRole = Roles.builder().role(RoleTypes.SELLER).build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@example.com");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addUserRole_Success() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.addUserRole(1L, "SELLER");

        assertEquals(userDto, result);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("ADD_USER_ROLE"), anyString());
    }

    @Test
    void addUserRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.addUserRole(1L, "SELLER"));
    }

    @Test
    void addUserRole_InvalidRole_ThrowsIllegalParameterException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.addUserRole(1L, "INVALID"));
    }


    @Test
    void addUserRole_AlreadyHasRole_ThrowsBadRequestException() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.addUserRole(1L, "SELLER"));
        assertEquals("Error updating client: user already has role SELLER.", e.getMessage());
    }

    @Test
    void addUserRole_ExceptionInTryBlock_ThrowsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenThrow(new RuntimeException("Test exception"));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.addUserRole(1L, "SELLER"));
        assertEquals("Error updating user.", e.getMessage());
    }

    @Test
    void removeUserRole_Success() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeUserRole(1L, "SELLER");

        assertEquals(userDto, result);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("REMOVE_USER_ROLE"), anyString());
    }

    @Test
    void removeUserRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
    }

    @Test
    void removeUserRole_InvalidRole_ThrowsIllegalParameterException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.removeUserRole(1L, "INVALID"));
    }

    @Test
    void removeUserRole_RoleNotFound_ThrowsResourceNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
    }

    @Test
    void removeUserRole_DoesNotHaveRole_ThrowsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.removeUserRole(1L, "SELLER"));
        assertEquals("User does not have role: SELLER", e.getMessage());
    }

    @Test
    void getUserRoles_Success() {
        Set<Roles> roles = new HashSet<>();
        roles.add(sellerRole);
        when(userRepository.findRolesByUserId(1L)).thenReturn(roles);

        Set<Roles> result = adminUserRoleService.getUserRoles(1L);

        assertEquals(roles, result);
    }

    @Test
    void assignSellerRole_Success() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.assignSellerRole(email);

        assertEquals(userDto, result);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("ASSIGN_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq(email), eq("SELLER"), eq("ASSIGN_ROLE"), any(LocalDateTime.class));
    }

    @Test
    void addMultipleRoles_Success() {
        Roles buyerRole = Roles.builder().role(RoleTypes.BUYER).build();
        user.getUserRoles().add(buyerRole); // User starts with BUYER role
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.addUserRole(1L, "SELLER");

        assertEquals(userDto, result);
        assertTrue(user.getUserRoles().contains(buyerRole), "User should still have BUYER role");
        assertTrue(user.getUserRoles().contains(sellerRole), "User should have SELLER role");
        verify(userRepository).save(user);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("ADD_USER_ROLE"), anyString());
    }

    @Test
    void assignSellerRole_InvalidEmail_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.assignSellerRole("invalid"));
    }

    @Test
    void assignSellerRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.assignSellerRole(email));
    }

    @Test
    void assignSellerRole_AlreadyHasRole_ThrowsBadRequestException() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.assignSellerRole(email));
        assertEquals("User already has role SELLER", e.getMessage());
    }

    @Test
    void removeSellerRole_Success() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeSellerRole(email);

        assertEquals(userDto, result);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("REMOVE_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq(email), eq("SELLER"), eq("REMOVE_ROLE"), any(LocalDateTime.class));
    }

    @Test
    void removeSellerRole_InvalidEmail_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.removeSellerRole("invalid"));
    }

    @Test
    void removeSellerRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.removeSellerRole(email));
    }

    @Test
    void removeSellerRole_DoesNotHaveRole_ThrowsBadRequestException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.removeSellerRole(email));
        assertEquals("User does not have role SELLER", e.getMessage());
    }

    @Test
    void assignRole_Success() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.assignRole(email, "SELLER");

        assertEquals(userDto, result);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("ASSIGN_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq(email), eq("SELLER"), eq("ASSIGN_ROLE"), any(LocalDateTime.class));
    }

    @Test
    void assignRole_InvalidEmail_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.assignRole("invalid", "SELLER"));
    }

    @Test
    void assignRole_InvalidRole_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.assignRole(email, "INVALID"));
    }

    @Test
    void assignRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.assignRole(email, "SELLER"));
    }

    @Test
    void assignRole_AlreadyHasRole_ThrowsBadRequestException() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.assignRole(email, "SELLER"));
        assertEquals("User already has role SELLER", e.getMessage());
    }

    @Test
    void removeRole_Success() {
        user.getUserRoles().add(sellerRole);
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        UserDto result = adminUserRoleService.removeRole(email, "SELLER");

        assertEquals(userDto, result);
        verify(refreshTokenRepository).deleteByUserEmail(email);
        verify(auditLogService).logAction(eq(email), eq("REMOVE_ROLE"), anyString(), eq("SELLER"), anyString());
        verify(emailService).sendRoleChangeNotification(eq(email), eq("SELLER"), eq("REMOVE_ROLE"), any(LocalDateTime.class));
    }

    @Test
    void removeRole_InvalidEmail_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.removeRole("invalid", "SELLER"));
    }

    @Test
    void removeRole_InvalidRole_ThrowsIllegalParameterException() {
        assertThrows(IllegalParameterException.class, () -> adminUserRoleService.removeRole(email, "INVALID"));
    }

    @Test
    void removeRole_CannotRemoveBuyerRole_ThrowsIllegalParameterException() {
        IllegalParameterException e = assertThrows(IllegalParameterException.class, () -> adminUserRoleService.removeRole(email, "BUYER"));
        assertEquals("Cannot remove BUYER role", e.getMessage());
    }

    @Test
    void removeRole_UserNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminUserRoleService.removeRole(email, "SELLER"));
    }

    @Test
    void removeRole_DoesNotHaveRole_ThrowsBadRequestException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(email)).thenReturn(Optional.of(user));

        BadRequestException e = assertThrows(BadRequestException.class, () -> adminUserRoleService.removeRole(email, "SELLER"));
        assertEquals("User does not have role SELLER", e.getMessage());
    }
}*/

