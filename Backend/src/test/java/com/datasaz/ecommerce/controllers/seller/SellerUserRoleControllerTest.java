package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.ResourceNotFoundException;
import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.ISellerUserRoleService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SellerUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class SellerUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ISellerUserRoleService sellerUserRoleService;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    private CompanyRequest companyRequest;
    private MockMultipartFile image;
    private MockMultipartFile companyRequestPart;

    @BeforeEach
    void setUp() throws Exception {
        reset(sellerUserRoleService, currentUserService, userDetailsService, jwtUtil, jwtBlacklistService);
        companyRequest = CompanyRequest.builder()
                .name("Test Company")
                .contactEmail("contact@example.com")
                .build();
        image = new MockMultipartFile("image", "test.jpg", "image/jpeg", "test image".getBytes());
        companyRequestPart = new MockMultipartFile("companyRequest", "companyRequest.json",
                "application/json", objectMapper.writeValueAsString(companyRequest).getBytes());
        User admin = new User();
        admin.setEmailAddress("admin@example.com");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
    }

    // Tests for becomeCompanyAdminSeller
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void becomeCompanyAdminSeller_Success() throws Exception {
        doNothing().when(sellerUserRoleService).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));

        mockMvc.perform(multipart("/seller/user/role/v2/become-company-admin-seller")
                        .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes())) // Use "file" instead of "image"
                        .file(companyRequestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully became company admin seller"));
        verify(sellerUserRoleService).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void becomeCompanyAdminSeller_IllegalStateException_ReturnsConflict() throws Exception {
        String errorMessage = "Company found, admin approval required";
        doThrow(new IllegalStateException(errorMessage)).when(sellerUserRoleService)
                .requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));

        mockMvc.perform(multipart("/seller/user/role/v2/become-company-admin-seller")
                        .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes()))
                        .file(companyRequestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isConflict())
                .andExpect(content().string(errorMessage)); // Expect the exception message
        verify(sellerUserRoleService).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void becomeCompanyAdminSeller_BadRequestException_ReturnsBadRequest() throws Exception {
        String errorMessage = "Company found. Admin approval required.";
        doThrow(BadRequestException.builder().message(errorMessage).build())
                .when(sellerUserRoleService).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));

        mockMvc.perform(multipart("/seller/user/role/v2/become-company-admin-seller")
                        .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes()))
                        .file(companyRequestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + errorMessage))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/v2/become-company-admin-seller"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void becomeCompanyAdminSeller_ValidationFailure_ReturnsBadRequest() throws Exception {
        CompanyRequest invalidRequest = CompanyRequest.builder().contactEmail("invalid-email").build();
        MockMultipartFile invalidRequestPart = new MockMultipartFile("companyRequest", "companyRequest.json",
                "application/json", objectMapper.writeValueAsString(invalidRequest).getBytes());

        mockMvc.perform(multipart("/seller/user/role/v2/become-company-admin-seller")
                        .file(new MockMultipartFile("file", "test.jpg", "image/jpeg", "test image".getBytes()))
                        .file(invalidRequestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.errors.name").value("Company name is required"))
                .andExpect(jsonPath("$.errors.contactEmail").value("Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/v2/become-company-admin-seller"));
        verify(sellerUserRoleService, never()).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));
    }

    @Test
    void becomeCompanyAdminSeller_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(multipart("/seller/user/role/v2/become-company-admin-seller")
                        .file(image)
                        .file(companyRequestPart)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).requestCompanyAdminSellerV2(any(CompanyRequest.class), any(MultipartFile.class));
    }

    // Tests for approveCompanyAdmin
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void approveCompanyAdmin_Success() throws Exception {
        doNothing().when(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, true, "valid-token");

        mockMvc.perform(post("/seller/user/role/approve-company-admin/company/1/user/1")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Company admin request approved"));
        verify(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, true, "valid-token");
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void approveCompanyAdmin_UserNotFound_ReturnsNotFound() throws Exception {
        doThrow(UserNotFoundException.builder().message("User not found").build())
                .when(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, true, "valid-token");

        mockMvc.perform(post("/seller/user/role/approve-company-admin/company/1/user/1")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/approve-company-admin/company/1/user/1"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, true, "valid-token");
    }

    @Test
    void approveCompanyAdmin_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/approve-company-admin/company/1/user/1")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).approveOrDenyCompanyAdminByCompanyAndUser(anyLong(), anyLong(), anyBoolean(), anyString());
    }

    // Tests for denyCompanyAdmin
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void denyCompanyAdmin_Success() throws Exception {
        doNothing().when(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, false, "valid-token");

        mockMvc.perform(post("/seller/user/role/deny-company-admin/company/1/user/1")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Company admin request denied"));
        verify(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, false, "valid-token");
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void denyCompanyAdmin_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Invalid or expired token").build())
                .when(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, false, "invalid-token");

        mockMvc.perform(post("/seller/user/role/deny-company-admin/company/1/user/1")
                        .param("token", "invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid or expired token"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/deny-company-admin/company/1/user/1"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).approveOrDenyCompanyAdminByCompanyAndUser(1L, 1L, false, "invalid-token");
    }

    @Test
    void denyCompanyAdmin_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/deny-company-admin/company/1/user/1")
                        .param("token", "valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).approveOrDenyCompanyAdminByCompanyAndUser(anyLong(), anyLong(), anyBoolean(), anyString());
    }

    // Tests for addSellerToCompany
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void addSellerToCompany_Success() throws Exception {
        doNothing().when(sellerUserRoleService).addSellerToCompany(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/add-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Seller added to company"));
        verify(sellerUserRoleService).addSellerToCompany(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void addSellerToCompany_UnauthorizedException_ReturnsUnauthorized() throws Exception {
        doThrow(UnauthorizedException.builder().message("Admin lacks permission to add sellers").build())
                .when(sellerUserRoleService).addSellerToCompany(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/add-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.UNAUTHORIZED_ACCESS + "Admin lacks permission to add sellers"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/add-seller/seller@example.com"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).addSellerToCompany(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    void addSellerToCompany_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/add-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).addSellerToCompany(anyLong(), anyString(), anyString());
    }

    // Tests for removeSellerFromCompany
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void removeSellerFromCompany_Success() throws Exception {
        doNothing().when(sellerUserRoleService).removeSellerFromCompany(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/remove-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Seller removed from company"));
        verify(sellerUserRoleService).removeSellerFromCompany(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void removeSellerFromCompany_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Cannot remove the only COMPANY_ADMIN_SELLER").build())
                .when(sellerUserRoleService).removeSellerFromCompany(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/remove-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Cannot remove the only COMPANY_ADMIN_SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/remove-seller/seller@example.com"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).removeSellerFromCompany(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    void removeSellerFromCompany_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/remove-seller/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).removeSellerFromCompany(anyLong(), anyString(), anyString());
    }

    // Tests for promoteToCompanyAdmin
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void promoteToCompanyAdmin_Success() throws Exception {
        doNothing().when(sellerUserRoleService).promoteToCompanyAdmin(1L, "seller@example.com", "admin@example.com", true, true, true);

        mockMvc.perform(post("/seller/user/role/company/1/promote-admin/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Seller promoted to company admin"));
        verify(sellerUserRoleService).promoteToCompanyAdmin(1L, "seller@example.com", "admin@example.com", true, true, true);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void promoteToCompanyAdmin_UnauthorizedException_ReturnsUnauthorized() throws Exception {
        doThrow(UnauthorizedException.builder().message("Admin lacks permission to promote admins").build())
                .when(sellerUserRoleService).promoteToCompanyAdmin(1L, "seller@example.com", "admin@example.com", true, true, true);

        mockMvc.perform(post("/seller/user/role/company/1/promote-admin/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.UNAUTHORIZED_ACCESS + "Admin lacks permission to promote admins"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/promote-admin/seller@example.com"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).promoteToCompanyAdmin(1L, "seller@example.com", "admin@example.com", true, true, true);
    }

    @Test
    void promoteToCompanyAdmin_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/promote-admin/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).promoteToCompanyAdmin(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    // Tests for demoteCompanyAdmin
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void demoteCompanyAdmin_Success() throws Exception {
        doNothing().when(sellerUserRoleService).demoteCompanyAdmin(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/demote-admin/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Company admin demoted"));
        verify(sellerUserRoleService).demoteCompanyAdmin(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void demoteCompanyAdmin_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Cannot demote the only COMPANY_ADMIN_SELLER").build())
                .when(sellerUserRoleService).demoteCompanyAdmin(1L, "seller@example.com", "admin@example.com");

        mockMvc.perform(post("/seller/user/role/company/1/demote-admin/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Cannot demote the only COMPANY_ADMIN_SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/demote-admin/seller@example.com"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).demoteCompanyAdmin(1L, "seller@example.com", "admin@example.com");
    }

    @Test
    void demoteCompanyAdmin_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/demote-admin/seller@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).demoteCompanyAdmin(anyLong(), anyString(), anyString());
    }

    // Tests for updateAdminRights
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void updateAdminRights_Success() throws Exception {
        doNothing().when(sellerUserRoleService).updateAdminRights(1L, "seller@example.com", "admin@example.com", true, true, true);

        mockMvc.perform(post("/seller/user/role/company/1/update-admin-rights/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Admin rights updated"));
        verify(sellerUserRoleService).updateAdminRights(1L, "seller@example.com", "admin@example.com", true, true, true);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void updateAdminRights_UnauthorizedException_ReturnsUnauthorized() throws Exception {
        doThrow(UnauthorizedException.builder().message("Admin lacks permission to delegate admin rights").build())
                .when(sellerUserRoleService).updateAdminRights(1L, "seller@example.com", "admin@example.com", true, true, true);

        mockMvc.perform(post("/seller/user/role/company/1/update-admin-rights/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.UNAUTHORIZED_ACCESS + "Admin lacks permission to delegate admin rights"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/update-admin-rights/seller@example.com"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).updateAdminRights(1L, "seller@example.com", "admin@example.com", true, true, true);
    }

    @Test
    void updateAdminRights_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/update-admin-rights/seller@example.com")
                        .param("canAddRemoveSellers", "true")
                        .param("canPromoteDemoteAdmins", "true")
                        .param("canDelegateAdminRights", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).updateAdminRights(anyLong(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    // Tests for deleteCompany
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void deleteCompany_Success() throws Exception {
        doNothing().when(sellerUserRoleService).deleteCompany(1L);

        mockMvc.perform(post("/seller/user/role/company/1/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Company deleted successfully"));
        verify(sellerUserRoleService).deleteCompany(1L);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void deleteCompany_ResourceNotFoundException_ReturnsNotFound() throws Exception {
        doThrow(ResourceNotFoundException.builder().message("Company not found with id: 1").build())
                .when(sellerUserRoleService).deleteCompany(1L);

        mockMvc.perform(post("/seller/user/role/company/1/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.RESOURCE_NOT_FOUND + "Company not found with id: 1"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ResourceNotFound"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/delete"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).deleteCompany(1L);
    }

    @Test
    void deleteCompany_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).deleteCompany(anyLong());
    }

    // Tests for revokeCompany
    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void revokeCompany_Success() throws Exception {
        doNothing().when(sellerUserRoleService).revokeCompany(1L);

        mockMvc.perform(post("/seller/user/role/company/1/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Company revoked successfully"));
        verify(sellerUserRoleService).revokeCompany(1L);
    }

    @Test
    @WithMockUser(roles = "COMPANY_ADMIN_SELLER")
    void revokeCompany_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Company is not deleted").build())
                .when(sellerUserRoleService).revokeCompany(1L);

        mockMvc.perform(post("/seller/user/role/company/1/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Company is not deleted"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/seller/user/role/company/1/revoke"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(sellerUserRoleService).revokeCompany(1L);
    }

    @Test
    void revokeCompany_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/seller/user/role/company/1/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(sellerUserRoleService, never()).revokeCompany(anyLong());
    }
}