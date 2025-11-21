package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.services.interfaces.IBuyerUserRoleService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BuyerUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class BuyerUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBuyerUserRoleService buyerUserRoleService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    @BeforeEach
    void setUp() {
        reset(buyerUserRoleService, userDetailsService, jwtUtil, jwtBlacklistService);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_Success() throws Exception {
        doNothing().when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully became an individual seller"));
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_UserNotFound_ReturnsNotFound() throws Exception {
        doThrow(UserNotFoundException.builder().message("User not found").build())
                .when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.USER_NOT_FOUND + "User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("UserNotFound"))
                .andExpect(jsonPath("$.path").value("/buyer/user/role/become-seller"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Error updating user.").build())
                .when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ExceptionMessages.BAD_REQUEST + "Error updating user."))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BadRequest"))
                .andExpect(jsonPath("$.path").value("/buyer/user/role/become-seller"))
                .andExpect(jsonPath("$.errors").isEmpty());
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    void becomeIndividualSeller_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(buyerUserRoleService, never()).becomeIndividualSeller();
    }
}

/*
package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.services.interfaces.IBuyerUserRoleService;
import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = BuyerUserRoleController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.datasaz.ecommerce.filters.JwtAuthenticationFilter.class)
})
class BuyerUserRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBuyerUserRoleService buyerUserRoleService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    @BeforeEach
    void setUp() {
        reset(buyerUserRoleService, userDetailsService, jwtUtil, jwtBlacklistService);
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_Success() throws Exception {
        doNothing().when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully became an individual seller"));
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_UserNotFound_ReturnsUnauthorized() throws Exception {
        doThrow(UserNotFoundException.builder().message("User not found").build())
                .when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("{\"message\":\"User not found\"}"));
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "BUYER")
    void becomeIndividualSeller_BadRequestException_ReturnsBadRequest() throws Exception {
        doThrow(BadRequestException.builder().message("Error updating user.").build())
                .when(buyerUserRoleService).becomeIndividualSeller();

        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"message\":\"Error updating user.\"}"));
        verify(buyerUserRoleService).becomeIndividualSeller();
    }

    @Test
    void becomeIndividualSeller_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/buyer/user/role/become-seller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(""));
        verify(buyerUserRoleService, never()).becomeIndividualSeller();
    }
}*/
