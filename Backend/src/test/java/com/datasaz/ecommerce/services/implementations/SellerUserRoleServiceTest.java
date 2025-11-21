package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.*;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private ICompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAdminRightsRepository companyAdminRightsRepository;

    @Mock
    private ApprovalTokenRepository approvalTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SellerUserRoleService sellerUserRoleService;

    private User user;
    private User admin;
    private User seller;
    private Company company;
    private Roles companyAdminSellerRole;
    private Roles sellerRole;
    private CompanyRequest companyRequest;
    private MultipartFile image;
    private CompanyAdminRights adminRights;
    private CompanyAdminRights sellerRights;
    private ApprovalToken approvalToken;
    private String email = "user@example.com";
    private String adminEmail = "admin@example.com";
    private String sellerEmail = "seller@example.com";
    private Long companyId = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress(email);
        user.setUserRoles(new HashSet<>());
        user.setCompany(null);

        admin = new User();
        admin.setId(2L);
        admin.setEmailAddress(adminEmail);
        admin.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build())));

        seller = new User();
        seller.setId(3L);
        seller.setEmailAddress(sellerEmail);
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        company = new Company();
        company.setId(companyId);
        company.setName("Test Company");
        company.setDeleted(false);
        company.setAdminRights(new HashSet<>());

        companyAdminSellerRole = Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build();
        sellerRole = Roles.builder().role(RoleTypes.SELLER).build();

        companyRequest = CompanyRequest.builder()
                .name("Test Company")
                .contactEmail("contact@example.com")
                .build();

        image = mock(MultipartFile.class);

        adminRights = CompanyAdminRights.builder()
                .id(1L)
                .company(company)
                .user(admin)
                .canAddRemoveSellers(true)
                .canPromoteDemoteAdmins(true)
                .canDelegateAdminRights(true)
                .build();

        sellerRights = CompanyAdminRights.builder()
                .id(2L)
                .company(company)
                .user(user)
                .canAddRemoveSellers(false)
                .canPromoteDemoteAdmins(false)
                .canDelegateAdminRights(false)
                .build();

        approvalToken = new ApprovalToken();
        approvalToken.setToken("test-token");
        approvalToken.setRights(sellerRights);
        approvalToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        approvalToken.setRevoked(false);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailAddressAndDeletedFalse(adminEmail)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.of(seller));
    }

//    @Test
//    void requestCompanyAdminSeller_NewCompany_Success() throws MessagingException {
//        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
//        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
//        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
//        when(userRepository.save(user)).thenReturn(user);
//
//        sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image);
//
//        verify(userRepository).save(user);
//        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
//        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
//    }

//    @Test
//    void requestCompanyAdminSeller_ExistingCompany_ThrowsBadRequestException() throws MessagingException {
//        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
//        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
//        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);
//
//        BadRequestException e = assertThrows(BadRequestException.class, () ->
//                sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image));
//        assertEquals("Company found. Admin approval required.", e.getMessage());
//
//        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
//        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
//    }

    @Test
    void requestCompanyAdminSellerV2_NewCompany_Success() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
        when(userRepository.save(user)).thenReturn(user);

        sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
    }

    @Test
    void requestCompanyAdminSellerV2_ExistingCompany_ThrowsConflictFoundException() throws MessagingException {
        // Arrange: existing company with admins
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);
        when(approvalTokenRepository.save(any(ApprovalToken.class))).thenReturn(approvalToken);

        // Act & Assert
        ConflictFoundException e = assertThrows(ConflictFoundException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image));

        // Check the full message with the exception prefix
        assertEquals(
                "Company found. An email has been sent for admin approval.",
                e.getMessage()
        );

        // Verify audit and email actions
        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
        verify(approvalTokenRepository).save(any(ApprovalToken.class));
    }

//    @Test
//    void requestCompanyAdminSellerV2_ExistingCompany_ThrowsConflictFoundException() throws MessagingException {
//        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
//        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
//        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);
//        when(approvalTokenRepository.save(any(ApprovalToken.class))).thenReturn(approvalToken);
//
//        ConflictFoundException e = assertThrows(ConflictFoundException.class, () ->
//                sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image));
//        assertEquals(ExceptionMessages.CONFLICT_EXCEPTION + "Company found. An email has been sent for admin approval.", e.getMessage());
//
//        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
//        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
//        verify(approvalTokenRepository).save(any(ApprovalToken.class));
//    }

    @Test
    void requestCompanyAdminSellerV2_InvalidName_ThrowsConstraintViolationException() {
        CompanyRequest invalidRequest = CompanyRequest.builder()
                .name("") // Blank name
                .contactEmail("contact@example.com")
                .build();
        when(companyService.registerCompany(eq(invalidRequest), any(), any()))
                .thenThrow(new ConstraintViolationException("Company name cannot be empty", Collections.emptySet()));

        assertThrows(ConstraintViolationException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(invalidRequest, image));
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Approve_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Deny_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), false, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, false, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Denied"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_InvalidToken_ThrowsBadRequestException() {
        when(approvalTokenRepository.findByTokenAndRevokedFalse("invalid-token")).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "invalid-token"));
        assertEquals("Invalid or expired token", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_ExpiredToken_ThrowsBadRequestException() {
        approvalToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("Token has expired", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Unauthorized_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(user); // User without admin rights
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("User not authorized to manage admin rights", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdmin_Approve_Success() throws MessagingException {
        company.getAdminRights().add(adminRights);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(List.of(sellerRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        when(userRepository.save(user)).thenReturn(user);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdmin(sellerRights.getId(), true);

        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdmin_RightsNotFound_ThrowsResourceNotFoundException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdmin(1L, true));
    }

    @Test
    void addSellerToCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Added to Company"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("ADD_SELLER_TO_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void addSellerToCompany_AdminNotAuthorized_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(
                CompanyAdminRights.builder().company(company).user(admin).canAddRemoveSellers(false).build()));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Admin lacks permission to add sellers", e.getMessage());
    }

    @Test
    void addSellerToCompany_SellerNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
    }

    @Test
    void addSellerToCompany_SellerNotSellerRole_ThrowsBadRequestException() {
        seller.getUserRoles().clear(); // Remove SELLER role
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("User is not a seller", e.getMessage());
    }

    @Test
    void removeSellerFromCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);

        sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(auditLogService).logAction(eq(sellerEmail), eq("REMOVE_SELLER_FROM_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void removeSellerFromCompany_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot remove the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(company);

        sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).save(any(CompanyAdminRights.class));
        verify(emailService).sendEmail(eq(sellerEmail), eq("Promoted to Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("PROMOTE_TO_COMPANY_ADMIN"), eq(adminEmail), anyString());
    }

    @Test
    void promoteToCompanyAdmin_NotSeller_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.getUserRoles().clear(); // Remove SELLER role

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_NotAssociatedWithCompany_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(null); // Not associated with the company

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void demoteCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);
        seller.getUserRoles().add(companyAdminSellerRole);

        sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(emailService).sendEmail(eq(sellerEmail), eq("Demoted from Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("DEMOTE_COMPANY_ADMIN"), anyString());
    }

    @Test
    void demoteCompanyAdmin_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot demote the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void updateAdminRights_Success() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true);

        verify(companyAdminRightsRepository).save(sellerRights);
        verify(auditLogService).logAction(eq(sellerEmail), eq("UPDATE_ADMIN_RIGHTS"), anyString());
    }

    @Test
    void updateAdminRights_PrimaryAdmin_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setPrimaryAdmin(seller);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User lacks permission to update primary admin", e.getMessage());
    }

    @Test
    void updateAdminRights_SellerNotAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("Seller is not a company admin for company: " + companyId, e.getMessage());
    }

    @Test
    void deleteCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(seller));
        when(productRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.deleteCompany(companyId);

        verify(companyRepository).save(company);
        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyId(companyId);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Company Deletion Notification"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("DELETE_COMPANY"), anyString());
    }

    @Test
    void deleteCompany_AlreadyDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setDeleted(true);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.deleteCompany(companyId));
        assertEquals("Company is already deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.revokeCompanyV0(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompanyV0_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_NoAdminRights_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.empty());

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Admin has no rights for company: " + companyId, e.getMessage());
    }

    @Test
    void revokeCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        company.setDeletedByUserId(admin.getId());

        sellerUserRoleService.revokeCompany(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompany_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompany_Unauthorized_ThrowsUnauthorizedException() {
        user.getUserRoles().add(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build());
        when(currentUserService.getCurrentUser()).thenReturn(user); // Not the deleting admin
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        company.setDeleted(true);
        company.setDeletedByUserId(999L); // Different admin ID

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Only the admin who deleted the company can revoke it", e.getMessage());
    }
}


/*package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private ICompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAdminRightsRepository companyAdminRightsRepository;

    @Mock
    private ApprovalTokenRepository approvalTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SellerUserRoleService sellerUserRoleService;

    private User user;
    private User admin;
    private User seller;
    private Company company;
    private Roles companyAdminSellerRole;
    private Roles sellerRole;
    private CompanyRequest companyRequest;
    private MultipartFile image;
    private CompanyAdminRights adminRights;
    private CompanyAdminRights sellerRights;
    private ApprovalToken approvalToken;
    private String email = "user@example.com";
    private String adminEmail = "admin@example.com";
    private String sellerEmail = "seller@example.com";
    private Long companyId = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress(email);
        user.setUserRoles(new HashSet<>());
        user.setCompany(null);

        admin = new User();
        admin.setId(2L);
        admin.setEmailAddress(adminEmail);
        admin.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build())));

        seller = new User();
        seller.setId(3L);
        seller.setEmailAddress(sellerEmail);
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        company = new Company();
        company.setId(companyId);
        company.setName("Test Company");
        company.setDeleted(false);
        company.setAdminRights(new HashSet<>());

        companyAdminSellerRole = Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build();
        sellerRole = Roles.builder().role(RoleTypes.SELLER).build();

        companyRequest = CompanyRequest.builder()
                .name("Test Company")
                .contactEmail("contact@example.com")
                .build();

        image = mock(MultipartFile.class);

        adminRights = CompanyAdminRights.builder()
                .id(1L)
                .company(company)
                .user(admin)
                .canAddRemoveSellers(true)
                .canPromoteDemoteAdmins(true)
                .canDelegateAdminRights(true)
                .build();

        sellerRights = CompanyAdminRights.builder()
                .id(2L)
                .company(company)
                .user(user)
                .canAddRemoveSellers(false)
                .canPromoteDemoteAdmins(false)
                .canDelegateAdminRights(false)
                .build();

        approvalToken = new ApprovalToken();
        approvalToken.setToken("test-token");
        approvalToken.setRights(sellerRights);
        approvalToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        approvalToken.setRevoked(false);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailAddressAndDeletedFalse(adminEmail)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.of(seller));
    }

    @Test
    void requestCompanyAdminSeller_NewCompany_Success() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
        when(userRepository.save(user)).thenReturn(user);

        sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
    }

    @Test
    void requestCompanyAdminSeller_ExistingCompany_ThrowsBadRequestException() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image));
        assertEquals("Company found. Admin approval required.", e.getMessage());

        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
    }

    @Test
    void requestCompanyAdminSellerV2_NewCompany_Success() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
        when(userRepository.save(user)).thenReturn(user);

        sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
    }

    @Test
    void requestCompanyAdminSellerV2_ExistingCompany_ThrowsBadRequestException() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);
        when(approvalTokenRepository.save(any(ApprovalToken.class))).thenReturn(approvalToken);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image));
        assertEquals("Company found. Admin approval required.", e.getMessage());

        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
        verify(approvalTokenRepository).save(any(ApprovalToken.class));
    }

    @Test
    void requestCompanyAdminSellerV2_InvalidName_ThrowsConstraintViolationException() {
        CompanyRequest invalidRequest = CompanyRequest.builder()
                .name("") // Blank name
                .contactEmail("contact@example.com")
                .build();
        when(companyService.registerCompany(eq(invalidRequest), any(), any()))
                .thenThrow(new ConstraintViolationException("Company name cannot be empty", Collections.emptySet()));

        assertThrows(ConstraintViolationException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(invalidRequest, image));
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Approve_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Deny_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), false, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, false, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Denied"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_InvalidToken_ThrowsBadRequestException() {
        when(approvalTokenRepository.findByTokenAndRevokedFalse("invalid-token")).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "invalid-token"));
        assertEquals("Invalid or expired token", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_ExpiredToken_ThrowsBadRequestException() {
        approvalToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("Token has expired", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Unauthorized_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(user); // User without admin rights
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("User not authorized to manage admin rights", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdmin_Approve_Success() throws MessagingException {
        company.getAdminRights().add(adminRights);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(List.of(sellerRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        when(userRepository.save(user)).thenReturn(user); // Explicitly mock save
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdmin(sellerRights.getId(), true);

        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

//    @Test
//    void approveOrDenyCompanyAdmin_Approve_Success() throws MessagingException {
//        company.getAdminRights().add(adminRights);
//        when(currentUserService.getCurrentUser()).thenReturn(admin);
//        when(companyService.getCompanyAdmins(null)).thenReturn(List.of(sellerRights)); // For rights lookup
//        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights)); // For email notifications
//        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
//        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);
//
//        sellerUserRoleService.approveOrDenyCompanyAdmin(sellerRights.getId(), true);
//
//        verify(userRepository).save(user);
//        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
//        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
//    }

//    @Test
//    void approveOrDenyCompanyAdmin_Approve_Success() throws MessagingException {
//        company.getAdminRights().add(adminRights);
//        when(currentUserService.getCurrentUser()).thenReturn(admin);
//        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
//        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
//        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);
//
//        sellerUserRoleService.approveOrDenyCompanyAdmin(sellerRights.getId(), true);
//
//        verify(userRepository).save(user);
//        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
//        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
//    }

    @Test
    void approveOrDenyCompanyAdmin_RightsNotFound_ThrowsResourceNotFoundException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdmin(1L, true));
    }

    @Test
    void addSellerToCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Added to Company"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("ADD_SELLER_TO_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void addSellerToCompany_AdminNotAuthorized_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(
                CompanyAdminRights.builder().company(company).user(admin).canAddRemoveSellers(false).build()));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Admin lacks permission to add sellers", e.getMessage());
    }

    @Test
    void addSellerToCompany_SellerNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
    }

    @Test
    void addSellerToCompany_SellerNotSellerRole_ThrowsBadRequestException() {
        seller.getUserRoles().clear(); // Remove SELLER role
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("User is not a seller", e.getMessage());
    }

    @Test
    void removeSellerFromCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);

        sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(auditLogService).logAction(eq(sellerEmail), eq("REMOVE_SELLER_FROM_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void removeSellerFromCompany_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot remove the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(company);

        sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).save(any(CompanyAdminRights.class));
        verify(emailService).sendEmail(eq(sellerEmail), eq("Promoted to Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("PROMOTE_TO_COMPANY_ADMIN"), eq(adminEmail), anyString());
    }

    @Test
    void promoteToCompanyAdmin_NotSeller_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.getUserRoles().clear(); // Remove SELLER role

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_NotAssociatedWithCompany_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(null); // Not associated with the company

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void demoteCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);
        seller.getUserRoles().add(companyAdminSellerRole);

        sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(emailService).sendEmail(eq(sellerEmail), eq("Demoted from Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("DEMOTE_COMPANY_ADMIN"), anyString());
    }

    @Test
    void demoteCompanyAdmin_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot demote the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void updateAdminRights_Success() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true);

        verify(companyAdminRightsRepository).save(sellerRights);
        verify(auditLogService).logAction(eq(sellerEmail), eq("UPDATE_ADMIN_RIGHTS"), anyString());
    }

    @Test
    void updateAdminRights_PrimaryAdmin_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setPrimaryAdmin(seller);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User lacks permission to update primary admin", e.getMessage());
    }

    @Test
    void updateAdminRights_SellerNotAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("Seller is not a company admin for company: " + companyId, e.getMessage());
    }

    @Test
    void deleteCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(seller));
        when(productRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.deleteCompany(companyId);

        verify(companyRepository).save(company);
        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyId(companyId);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Company Deletion Notification"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("DELETE_COMPANY"), anyString());
    }

    @Test
    void deleteCompany_AlreadyDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setDeleted(true);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.deleteCompany(companyId));
        assertEquals("Company is already deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.revokeCompanyV0(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompanyV0_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_NoAdminRights_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.empty());

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Admin has no rights for company: " + companyId, e.getMessage());
    }

    @Test
    void revokeCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        company.setDeletedByUserId(admin.getId());

        sellerUserRoleService.revokeCompany(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompany_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompany_Unauthorized_ThrowsUnauthorizedException() {
        user.getUserRoles().add(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build());
        when(currentUserService.getCurrentUser()).thenReturn(user); // Not the deleting admin
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        company.setDeleted(true);
        company.setDeletedByUserId(999L); // Different admin ID

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Only the admin who deleted the company can revoke it", e.getMessage());
    }
}*/


/*
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.*;
import com.datasaz.ecommerce.repositories.entities.*;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import com.datasaz.ecommerce.services.interfaces.ICompanyService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.mail.MessagingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerUserRoleServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RolesRepository rolesRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private IEmailService emailService;

    @Mock
    private IAuditLogService auditLogService;

    @Mock
    private ICompanyService companyService;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanyAdminRightsRepository companyAdminRightsRepository;

    @Mock
    private ApprovalTokenRepository approvalTokenRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private SellerUserRoleService sellerUserRoleService;

    private User user;
    private User admin;
    private User seller;
    private Company company;
    private Roles companyAdminSellerRole;
    private Roles sellerRole;
    private CompanyRequest companyRequest;
    private MultipartFile image;
    private CompanyAdminRights adminRights;
    private CompanyAdminRights sellerRights;
    private ApprovalToken approvalToken;
    private String email = "user@example.com";
    private String adminEmail = "admin@example.com";
    private String sellerEmail = "seller@example.com";
    private Long companyId = 1L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmailAddress(email);
        user.setUserRoles(new HashSet<>());
        user.setCompany(null);

        admin = new User();
        admin.setId(2L);
        admin.setEmailAddress(adminEmail);
        admin.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build())));

        seller = new User();
        seller.setId(3L);
        seller.setEmailAddress(sellerEmail);
        seller.setUserRoles(new HashSet<>(Set.of(Roles.builder().role(RoleTypes.SELLER).build())));

        company = new Company();
        company.setId(companyId);
        company.setName("Test Company");
        company.setDeleted(false);
        company.setAdminRights(new HashSet<>());

        companyAdminSellerRole = Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build();
        sellerRole = Roles.builder().role(RoleTypes.SELLER).build();

        companyRequest = CompanyRequest.builder()
                .name("Test Company")
                .contactEmail("contact@example.com")
                .build();

        image = mock(MultipartFile.class);

        adminRights = CompanyAdminRights.builder()
                .id(1L)
                .company(company)
                .user(admin)
                .canAddRemoveSellers(true)
                .canPromoteDemoteAdmins(true)
                .canDelegateAdminRights(true)
                .build();

        sellerRights = CompanyAdminRights.builder()
                .id(2L)
                .company(company)
                .user(user)
                .canAddRemoveSellers(false)
                .canPromoteDemoteAdmins(false)
                .canDelegateAdminRights(false)
                .build();

        approvalToken = new ApprovalToken();
        approvalToken.setToken("test-token");
        approvalToken.setRights(sellerRights);
        approvalToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        approvalToken.setRevoked(false);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        when(rolesRepository.findByRole(RoleTypes.SELLER)).thenReturn(Optional.of(sellerRole));
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        when(userRepository.findByEmailAddressAndDeletedFalse(adminEmail)).thenReturn(Optional.of(admin));
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.of(seller));
    }

    @Test
    void requestCompanyAdminSeller_NewCompany_Success() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
        when(userRepository.save(user)).thenReturn(user);

        sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
    }

    @Test
    void requestCompanyAdminSeller_ExistingCompany_ThrowsBadRequestException() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image));
        assertEquals("Company found. Admin approval required.", e.getMessage());

        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
    }

    @Test
    void requestCompanyAdminSellerV2_NewCompany_Success() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(Collections.emptyList());
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, true, true)).thenReturn(adminRights);
        when(userRepository.save(user)).thenReturn(user);

        sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image);

        verify(userRepository).save(user);
        verify(auditLogService).logAction(eq(email), eq("BECOME_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(email), eq("Company Admin Seller Request Approved"), anyString());
    }

    @Test
    void requestCompanyAdminSellerV2_ExistingCompany_ThrowsBadRequestException() throws MessagingException {
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        when(companyService.registerCompany(any(), any(), any())).thenReturn(company);
        when(companyService.assignAdminRights(user, company, false, false)).thenReturn(sellerRights);
        when(approvalTokenRepository.save(any(ApprovalToken.class))).thenReturn(approvalToken);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image));
        assertEquals("Company found. Admin approval required.", e.getMessage());

        verify(auditLogService).logAction(eq(email), eq("REQUEST_COMPANY_ADMIN_SELLER"), anyString());
        verify(emailService).sendEmail(eq(adminEmail), eq("New Company Admin Seller Request"), anyString());
        verify(approvalTokenRepository).save(any(ApprovalToken.class));
    }

    @Test
    void requestCompanyAdminSellerV2_InvalidName_ThrowsConstraintViolationException() {
        CompanyRequest invalidRequest = CompanyRequest.builder()
                .name("") // Blank name
                .contactEmail("contact@example.com")
                .build();
        when(companyService.registerCompany(eq(invalidRequest), any(), any()))
                .thenThrow(new ConstraintViolationException("Company name cannot be empty", Collections.emptySet()));

        assertThrows(ConstraintViolationException.class, () ->
                sellerUserRoleService.requestCompanyAdminSellerV2(invalidRequest, image));
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Approve_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Deny_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyService.getCompanyAdmins(company)).thenReturn(List.of(adminRights));
        company.getAdminRights().add(adminRights);
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), false, admin);

        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, false, "test-token");

        verify(approvalTokenRepository).save(approvalToken);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Denied"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_InvalidToken_ThrowsBadRequestException() {
        when(approvalTokenRepository.findByTokenAndRevokedFalse("invalid-token")).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "invalid-token"));
        assertEquals("Invalid or expired token", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_ExpiredToken_ThrowsBadRequestException() {
        approvalToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("Token has expired", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdminByCompanyAndUser_Unauthorized_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(user); // User without admin rights
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(companyAdminRightsRepository.findByUserAndCompany(user, company)).thenReturn(Optional.of(sellerRights));
        when(approvalTokenRepository.findByTokenAndRevokedFalse("test-token")).thenReturn(Optional.of(approvalToken));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, 1L, true, "test-token"));
        assertEquals("User not authorized to manage admin rights", e.getMessage());
    }

    @Test
    void approveOrDenyCompanyAdmin_Approve_Success() throws MessagingException {
        company.getAdminRights().add(adminRights);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(List.of(sellerRights));
        when(rolesRepository.findByRole(RoleTypes.COMPANY_ADMIN_SELLER)).thenReturn(Optional.of(companyAdminSellerRole));
        doNothing().when(companyService).updateAdminRights(sellerRights.getId(), true, admin);

        sellerUserRoleService.approveOrDenyCompanyAdmin(sellerRights.getId(), true);

        verify(userRepository).save(user);
        verify(emailService, times(2)).sendEmail(anyString(), eq("Company Admin Seller Request Approved"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("APPROVE_DENY_COMPANY_ADMIN"), anyString());
    }

    @Test
    void approveOrDenyCompanyAdmin_RightsNotFound_ThrowsResourceNotFoundException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyService.getCompanyAdmins(null)).thenReturn(Collections.emptyList());

        assertThrows(ResourceNotFoundException.class, () ->
                sellerUserRoleService.approveOrDenyCompanyAdmin(1L, true));
    }

    @Test
    void addSellerToCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Added to Company"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("ADD_SELLER_TO_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void addSellerToCompany_AdminNotAuthorized_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(
                CompanyAdminRights.builder().company(company).user(admin).canAddRemoveSellers(false).build()));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Admin lacks permission to add sellers", e.getMessage());
    }

    @Test
    void addSellerToCompany_SellerNotFound_ThrowsUserNotFoundException() {
        when(userRepository.findByEmailAddressAndDeletedFalse(sellerEmail)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
    }

    @Test
    void addSellerToCompany_SellerNotSellerRole_ThrowsBadRequestException() {
        seller.getUserRoles().clear(); // Remove SELLER role
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail));
        assertEquals("User is not a seller", e.getMessage());
    }

    @Test
    void removeSellerFromCompany_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);

        sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(auditLogService).logAction(eq(sellerEmail), eq("REMOVE_SELLER_FROM_COMPANY"), eq(adminEmail), anyString());
    }

    @Test
    void removeSellerFromCompany_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot remove the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(company);

        sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).save(any(CompanyAdminRights.class));
        verify(emailService).sendEmail(eq(sellerEmail), eq("Promoted to Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("PROMOTE_TO_COMPANY_ADMIN"), eq(adminEmail), anyString());
    }

    @Test
    void promoteToCompanyAdmin_NotSeller_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.getUserRoles().clear(); // Remove SELLER role

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void promoteToCompanyAdmin_NotAssociatedWithCompany_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        seller.setCompany(null); // Not associated with the company

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User is not a seller associated with the company", e.getMessage());
    }

    @Test
    void demoteCompanyAdmin_Success() throws MessagingException {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(2L);
        seller.getUserRoles().add(companyAdminSellerRole);

        sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail);

        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyIdAndUserId(companyId, seller.getId());
        verify(emailService).sendEmail(eq(sellerEmail), eq("Demoted from Company Admin"), anyString());
        verify(auditLogService).logAction(eq(sellerEmail), eq("DEMOTE_COMPANY_ADMIN"), anyString());
    }

    @Test
    void demoteCompanyAdmin_OnlyAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.countCompanyAdminsByCompanyId(companyId)).thenReturn(1L);
        company.setPrimaryAdmin(seller);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail));
        assertEquals("Cannot demote the only COMPANY_ADMIN_SELLER", e.getMessage());
    }

    @Test
    void updateAdminRights_Success() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true);

        verify(companyAdminRightsRepository).save(sellerRights);
        verify(auditLogService).logAction(eq(sellerEmail), eq("UPDATE_ADMIN_RIGHTS"), anyString());
    }

    @Test
    void updateAdminRights_PrimaryAdmin_ThrowsUnauthorizedException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setPrimaryAdmin(seller);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.of(sellerRights));

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("User lacks permission to update primary admin", e.getMessage());
    }

    @Test
    void updateAdminRights_SellerNotAdmin_ThrowsBadRequestException() {
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, seller.getId())).thenReturn(Optional.empty());

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, true, true, true));
        assertEquals("Seller is not a company admin for company: " + companyId, e.getMessage());
    }

    @Test
    void deleteCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(List.of(seller));
        when(productRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.deleteCompany(companyId);

        verify(companyRepository).save(company);
        verify(userRepository).save(seller);
        verify(companyAdminRightsRepository).deleteByCompanyId(companyId);
        verify(emailService).sendEmail(eq(sellerEmail), eq("Company Deletion Notification"), anyString());
        verify(auditLogService).logAction(eq(adminEmail), eq("DELETE_COMPANY"), anyString());
    }

    @Test
    void deleteCompany_AlreadyDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        company.setDeleted(true);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.deleteCompany(companyId));
        assertEquals("Company is already deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));
        when(userRepository.findByCompanyId(companyId)).thenReturn(Collections.emptyList());

        sellerUserRoleService.revokeCompanyV0(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompanyV0_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.of(adminRights));

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompanyV0_NoAdminRights_ThrowsUnauthorizedException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        when(companyAdminRightsRepository.findByCompanyIdAndUserId(companyId, admin.getId())).thenReturn(Optional.empty());

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompanyV0(companyId));
        assertEquals("Admin has no rights for company: " + companyId, e.getMessage());
    }

    @Test
    void revokeCompany_Success() throws MessagingException {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(true);
        company.setDeletedByUserId(admin.getId());

        sellerUserRoleService.revokeCompany(companyId);

        verify(companyRepository).save(company);
        verify(auditLogService).logAction(eq(adminEmail), eq("REVOKE_COMPANY"), anyString());
    }

    @Test
    void revokeCompany_NotDeleted_ThrowsBadRequestException() {
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        company.setDeleted(false);

        BadRequestException e = assertThrows(BadRequestException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Company is not deleted", e.getMessage());
    }

    @Test
    void revokeCompany_Unauthorized_ThrowsUnauthorizedException() {
        user.getUserRoles().add(Roles.builder().role(RoleTypes.COMPANY_ADMIN_SELLER).build());
        when(currentUserService.getCurrentUser()).thenReturn(user); // Not the deleting admin
        when(companyRepository.findById(companyId)).thenReturn(Optional.of(company));
        company.setDeleted(true);
        company.setDeletedByUserId(999L); // Different admin ID

        UnauthorizedException e = assertThrows(UnauthorizedException.class, () ->
                sellerUserRoleService.revokeCompany(companyId));
        assertEquals("Only the admin who deleted the company can revoke it", e.getMessage());
    }
}

*/
