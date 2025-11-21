package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.InvalidRoleException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.ManageRoleRequest;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.services.interfaces.IAdminUserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class AdminUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAdminUserRoleService adminUserRoleService;

    private UserDto userDto;
    private ManageRoleRequest manageRoleRequest;

    @BeforeEach
    void setUp() {
        reset(adminUserRoleService);
        userDto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .build();
        manageRoleRequest = new ManageRoleRequest();
        manageRoleRequest.setEmail("test@example.com");
        manageRoleRequest.setRole("SELLER");
    }

    // Tests for addUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_Success() throws Exception {
        when(adminUserRoleService.addUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.addUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).addUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("Error updating client: user already has role SELLER.").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Error updating client: user already has role SELLER."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    void addUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).addUserRole(anyLong(), anyString());
    }

    // Tests for removeUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_Success() throws Exception {
        when(adminUserRoleService.removeUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.removeUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("User does not have role: SELLER").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User does not have role: SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    void removeUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).removeUserRole(anyLong(), anyString());
    }

    // Tests for getUserRoles
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_Success() throws Exception {
        Set<Roles> roles = Set.of(Roles.builder().role(RoleTypes.SELLER).build());
        when(adminUserRoleService.getUserRoles(1L)).thenReturn(roles);

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("SELLER"));
        verify(adminUserRoleService).getUserRoles(1L);
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.getUserRoles(1L))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/roles"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).getUserRoles(1L);
    }

    @Test
    @WithAnonymousUser
    void getUserRoles_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).getUserRoles(anyLong());
    }

//    @Test
//    void getUserRoles_Unauthenticated_ReturnsUnauthorized() throws Exception {
//        mockMvc.perform(get("/admin/user/role/1/roles")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
//                .andExpect(status().isUnauthorized())
//                .andExpect(content().string(""));
//        verify(adminUserRoleService, never()).getUserRoles(anyLong());
//    }

    // Tests for assignSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_Success() throws Exception {
        when(adminUserRoleService.assignSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.assignSellerRole("invalid-email"))
                .thenThrow(IllegalParameterException.builder().message("Invalid email format").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-email".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/assign-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).assignSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.assignSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/assign-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.assignSellerRole("test@example.com"))
                .thenThrow(BadRequestException.builder().message("User already has role SELLER").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/assign-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).assignSellerRole("test@example.com");
    }

    @Test
    void assignSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).assignSellerRole(anyString());
    }

    // Tests for removeSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_Success() throws Exception {
        when(adminUserRoleService.removeSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.removeSellerRole("invalid-email"))
                .thenThrow(IllegalParameterException.builder().message("Invalid email format").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-email".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.removeSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.removeSellerRole("test@example.com"))
                .thenThrow(BadRequestException.builder().message("User does not have role SELLER").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("test@example.com".toString())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeSellerRole("test@example.com");
    }

    @Test
    void removeSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).removeSellerRole(anyString());
    }

    // Tests for assignRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_Success() throws Exception {
        when(adminUserRoleService.assignRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"));
        verify(adminUserRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_BlankEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.email").value("must not be blank"));
        verify(adminUserRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_BlankRole_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setRole("");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.role").value("must not be blank"));
        verify(adminUserRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidRole_ReturnsBadRequest() throws Exception {
        String requestBody = "{\"email\":\"test@example.com\",\"role\":\"INVALID\"}";

        // Mock the service to throw IllegalParameterException
        when(adminUserRoleService.assignRole("test@example.com", "INVALID"))
                .thenThrow(InvalidRoleException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("InvalidRole"))
                .andExpect(jsonPath("$.message").value(ExceptionMessages.INVALID_ROLE + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());

        verify(adminUserRoleService).assignRole("test@example.com", "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(BadRequestException.builder().message("User already has role SELLER").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    void assignRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).assignRole(anyString(), anyString());
    }

    // Tests for removeRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_Success() throws Exception {
        when(adminUserRoleService.removeRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(adminUserRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"));
        verify(adminUserRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_BlankEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.email").value("must not be blank"));
        verify(adminUserRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_BlankRole_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setRole("");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.role").value("must not be blank"));
        verify(adminUserRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_CannotRemoveBuyerRole_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setRole("BUYER");
        when(adminUserRoleService.removeRole("test@example.com", "BUYER"))
                .thenThrow(InvalidRoleException.builder().message("Cannot remove BUYER role").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.INVALID_ROLE + "Cannot remove BUYER role"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("InvalidRole"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeRole("test@example.com", "BUYER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(adminUserRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(adminUserRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(BadRequestException.builder().message("User does not have role SELLER").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(adminUserRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    void removeRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(adminUserRoleService, never()).removeRole(anyString(), anyString());
    }
}

/*
import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.InvalidRoleException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.ManageRoleRequest;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.services.interfaces.IAdminUserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class AdminUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAdminUserRoleService userRoleService;

    private UserDto userDto;
    private ManageRoleRequest manageRoleRequest;

    @BeforeEach
    void setUp() {
        reset(userRoleService);
        userDto = UserDto.builder()
                .id(1L)
                .emailAddress("test@example.com")
                .userRoles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .build();
        manageRoleRequest = new ManageRoleRequest();
        manageRoleRequest.setEmail("test@example.com");
        manageRoleRequest.setRole("SELLER");
    }

    // Tests for addUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_Success() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.addUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).addUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("Error updating client: user already has role SELLER.").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Error updating client: user already has role SELLER."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/add"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    void addUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).addUserRole(anyLong(), anyString());
    }

    // Tests for removeUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_Success() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("User does not have role: SELLER").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User does not have role: SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/remove"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    void removeUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeUserRole(anyLong(), anyString());
    }

    // Tests for getUserRoles
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_Success() throws Exception {
        Set<Roles> roles = Set.of(Roles.builder().role(RoleTypes.SELLER).build());
        when(userRoleService.getUserRoles(1L)).thenReturn(roles);

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("SELLER"));
        verify(userRoleService).getUserRoles(1L);
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.getUserRoles(1L))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/1/roles"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).getUserRoles(1L);
    }

    @Test
    void getUserRoles_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).getUserRoles(anyLong());
    }

    // Tests for assignSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_Success() throws Exception {
        when(userRoleService.assignSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(userRoleService.assignSellerRole("invalid-email"))
                .thenThrow(IllegalParameterException.builder().message("Invalid email format").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid-email\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/assign-seller-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.assignSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/assign-seller-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(BadRequestException.builder().message("User already has role SELLER").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    void assignSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).assignSellerRole(anyString());
    }

    // Tests for removeSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_Success() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeSellerRole("invalid-email"))
                .thenThrow(IllegalParameterException.builder().message("Invalid email format").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid-email\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com"))
                .thenThrow(BadRequestException.builder().message("User does not have role SELLER").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/remove-seller-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    void removeSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeSellerRole(anyString());
    }

    // Tests for assignRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_Success() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"));
        verify(userRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_BlankEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST +"Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.email").value("must not be blank"));
        verify(userRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_BlankRole_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setRole("");

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST +"Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors.role").value("must not be blank"));
        verify(userRoleService, never()).assignRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidRole_ReturnsBadRequest() throws Exception {
        String requestBody = "{\"email\":\"test@example.com\",\"role\":\"INVALID\"}";

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("InvalidRole"))
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Invalid role: INVALID"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                //.andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignRole("test@example.com", "INVALID");
    }

//    @Test
//    @WithMockUser(roles = "APP_ADMIN")
//    void assignRole_InvalidRole_ReturnsBadRequest() throws Exception {
//        manageRoleRequest.setRole("INVALID");
//        when(userRoleService.assignRole("test@example.com", "INVALID"))
//                .thenThrow(new IllegalArgumentException("Invalid role: INVALID"));
//
//        mockMvc.perform(post("/admin/user/role/admin/assign-role")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(manageRoleRequest))
//                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.message").value("Invalid role: INVALID"))
//                .andExpect(jsonPath("$.status").value(400))
//                .andExpect(jsonPath("$.error").value("IllegalParameter"))
//                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
//                .andExpect(jsonPath("$.errors").isEmpty());
//        verify(userRoleService).assignRole("test@example.com", "INVALID");
//    }
    // .andExpect(jsonPath("$.message").value(ExceptionMessages.INVALID_ROLE + "Invalid role: INVALID"))

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_AlreadyHasRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(new RuntimeException("User already has role SELLER"));

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.TECHNICAL_EXCEPTION + "User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/assign-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    void assignRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).assignRole(anyString(), anyString());
    }

    // Tests for removeRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_Success() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.userRoles[0].role").value("SELLER"));
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.email").value("Invalid email format"));
        verify(userRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_BlankEmail_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("");
        invalidRequest.setRole("SELLER");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.email").value("must not be blank"));
        verify(userRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_BlankRole_ReturnsBadRequest() throws Exception {
        ManageRoleRequest invalidRequest = new ManageRoleRequest();
        invalidRequest.setEmail("test@example.com");
        invalidRequest.setRole("");

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Validation failed for request."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("ValidationError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors.role").value("must not be blank"));
        verify(userRoleService, never()).removeRole(anyString(), anyString());
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_CannotRemoveBuyerRole_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setRole("BUYER");
        when(userRoleService.removeRole("test@example.com", "BUYER"))
                .thenThrow(InvalidRoleException.builder().message("Cannot remove BUYER role").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.INVALID_ROLE + "Cannot remove BUYER role"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("InvalidRole"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.timestamp").isNumber())
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeRole("test@example.com", "BUYER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_DoesNotHaveRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(new RuntimeException("User does not have role SELLER"));

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.TECHNICAL_EXCEPTION + "User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"))
                .andExpect(jsonPath("$.path").value("/admin/user/role/admin/remove-role"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    void removeRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeRole(anyString(), anyString());
    }
}*/

/*

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.ManageRoleRequest;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAdminUserRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AdminUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class AdminUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IAdminUserRoleService userRoleService;

    private UserDto userDto;
    private ManageRoleRequest manageRoleRequest;

    @BeforeEach
    void setUp() {
        reset(userRoleService);
        userDto = UserDto.builder()
                .emailAddress("test@example.com")
                .roles(Set.of(Roles.builder().role(RoleTypes.SELLER).build()))
                .build();
        manageRoleRequest = new ManageRoleRequest();
        manageRoleRequest.setEmail("test@example.com");
        manageRoleRequest.setRole("SELLER");
    }

    // Tests for addUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_Success() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.addUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).addUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void addUserRole_AlreadyHasRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.addUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("Error updating client: user already has role SELLER.").build());

        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error updating client: user already has role SELLER."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"));
        verify(userRoleService).addUserRole(1L, "SELLER");
    }

    @Test
    void addUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/1/add")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).addUserRole(anyLong(), anyString());
    }

    // Tests for removeUserRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_Success() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER")).thenReturn(userDto);

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_InvalidRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeUserRole(1L, "INVALID"))
                .thenThrow(IllegalParameterException.builder().message("Invalid role: INVALID").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "INVALID")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).removeUserRole(1L, "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeUserRole_DoesNotHaveRole_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeUserRole(1L, "SELLER"))
                .thenThrow(BadRequestException.builder().message("User does not have role: SELLER").build());

        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User does not have role: SELLER"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"));
        verify(userRoleService).removeUserRole(1L, "SELLER");
    }

    @Test
    void removeUserRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/admin/user/role/1/remove")
                        .param("role", "SELLER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeUserRole(anyLong(), anyString());
    }

    // Tests for getUserRoles
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_Success() throws Exception {
        Set<Roles> roles = Set.of(Roles.builder().role(RoleTypes.SELLER).build());
        when(userRoleService.getUserRoles(1L)).thenReturn(roles);

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("SELLER"));
        verify(userRoleService).getUserRoles(1L);
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void getUserRoles_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.getUserRoles(1L))
                .thenThrow(UserNotFoundException.builder().message("User not found").build());

        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).getUserRoles(1L);
    }

    @Test
    void getUserRoles_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/user/role/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).getUserRoles(anyLong());
    }

    // Tests for assignSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_Success() throws Exception {
        when(userRoleService.assignSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(userRoleService.assignSellerRole("invalid-email"))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid-email\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).assignSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.assignSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).assignSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignSellerRole_AlreadyHasRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.assignSellerRole("test@example.com"))
                .thenThrow(new RuntimeException("User already has role SELLER"));

        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"));
        verify(userRoleService).assignSellerRole("test@example.com");
    }

    @Test
    void assignSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/assign-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).assignSellerRole(anyString());
    }

    // Tests for removeSellerRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_Success() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        when(userRoleService.removeSellerRole("invalid-email"))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"invalid-email\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).removeSellerRole("invalid-email");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeSellerRole_DoesNotHaveRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.removeSellerRole("test@example.com"))
                .thenThrow(new RuntimeException("User does not have role SELLER"));

        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"));
        verify(userRoleService).removeSellerRole("test@example.com");
    }

    @Test
    void removeSellerRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/remove-seller-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"test@example.com\"")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeSellerRole(anyString());
    }

    // Tests for assignRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_Success() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setEmail("invalid-email");
        when(userRoleService.assignRole("invalid-email", "SELLER"))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).assignRole("invalid-email", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_InvalidRole_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setRole("INVALID");
        when(userRoleService.assignRole("test@example.com", "INVALID"))
                .thenThrow(new IllegalArgumentException("Invalid role: INVALID"));

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid role: INVALID"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).assignRole("test@example.com", "INVALID");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void assignRole_AlreadyHasRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.assignRole("test@example.com", "SELLER"))
                .thenThrow(new RuntimeException("User already has role SELLER"));

        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User already has role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"));
        verify(userRoleService).assignRole("test@example.com", "SELLER");
    }

    @Test
    void assignRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/assign-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).assignRole(anyString(), anyString());
    }

    // Tests for removeRole
    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_Success() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER")).thenReturn(userDto);

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailAddress").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0].role").value("SELLER"));
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_InvalidEmail_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setEmail("invalid-email");
        when(userRoleService.removeRole("invalid-email", "SELLER"))
                .thenThrow(new IllegalArgumentException("Invalid email format"));

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid email format"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).removeRole("invalid-email", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_CannotRemoveBuyerRole_ReturnsBadRequest() throws Exception {
        manageRoleRequest.setRole("BUYER");
        when(userRoleService.removeRole("test@example.com", "BUYER"))
                .thenThrow(new IllegalArgumentException("Cannot remove BUYER role"));

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot remove BUYER role"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("IllegalParameter"));
        verify(userRoleService).removeRole("test@example.com", "BUYER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_UserNotFound_ReturnsNotFound() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(UserNotFoundException.builder().message("User not found with email: test@example.com").build());

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with email: test@example.com"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"));
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    @WithMockUser(roles = "APP_ADMIN")
    void removeRole_DoesNotHaveRole_ReturnsInternalServerError() throws Exception {
        when(userRoleService.removeRole("test@example.com", "SELLER"))
                .thenThrow(new RuntimeException("User does not have role SELLER"));

        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("User does not have role SELLER"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("UnexpectedError"));
        verify(userRoleService).removeRole("test@example.com", "SELLER");
    }

    @Test
    void removeRole_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/admin/user/role/admin/remove-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(manageRoleRequest))
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(userRoleService, never()).removeRole(anyString(), anyString());
    }
}*/
