package com.datasaz.ecommerce.configs;

import com.datasaz.ecommerce.filters.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private OAuth2AuthorizedClientService authorizedClientService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        // Reset mocks if needed
    }

//    @Test
//    void testPublicEndpointsAccessibleWithoutAuthentication() throws Exception {
//        // Test public endpoints like /auth/**, /api/product/**, etc.
//        String[] publicEndpoints = {
//                "/auth/register",
//                "/auth/login/submit",
//                "/api/product/list",
//                "/api/category/list",
//                "/swagger-ui/index.html",
//                "/v3/api-docs"
//        };
//
//        for (String endpoint : publicEndpoints) {
//            mockMvc.perform(get(endpoint))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string(org.hamcrest.Matchers.anyOf(
//                            org.hamcrest.Matchers.containsString(""),
//                            org.hamcrest.Matchers.containsString("swagger"),
//                            org.hamcrest.Matchers.containsString("api-docs")
//                    )));
//        }
//    }
//
//    @Test
//    @WithMockUser(authorities = "ROLE_BUYER")
//    void testBuyerEndpointWithBuyerRole() throws Exception {
//        mockMvc.perform(get("/buyer/orders"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    @WithMockUser(authorities = "ROLE_SELLER")
//    void testSellerEndpointWithSellerRole() throws Exception {
//        mockMvc.perform(get("/seller/products"))
//                .andExpect(status().isOk());
//    }
//
//    @Test
//    @WithMockUser(authorities = "ROLE_APP_ADMIN")
//    void testAdminEndpointWithAdminRole() throws Exception {
//        mockMvc.perform(get("/admin/users"))
//                .andExpect(status().isOk());
//    }

//    @Test
//    void testProtectedEndpointWithoutAuthentication() throws Exception {
//        mockMvc.perform(get("/buyer/orders"))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.error").value("Unauthorized"));
//    }
//
//    @Test
//    @WithMockUser(authorities = "ROLE_BUYER")
//    void testSellerEndpointWithBuyerRole() throws Exception {
//        mockMvc.perform(get("/seller/products"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    void testOAuth2LoginRedirect() throws Exception {
//        mockMvc.perform(get("/auth2/google"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrlPattern("**/ecommerce/auth2/google"));
//    }

//    @Test
//    void testLoginPageAccessible() throws Exception {
//        mockMvc.perform(get("/ecommerce/auth/login/form"))
//                .andExpect(status().isOk());
//    }

//    @Test
//    void testLogoutRedirect() throws Exception {
//        mockMvc.perform(post("/logout"))
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/ecommerce/auth/logout"));
//    }
//
//    @Test
//    void testUnauthorizedAccessToLoginForm() throws Exception {
//        mockMvc.perform(get("/ecommerce/auth/login/form"))
//                .andExpect(status().isOk()); // Login page should be accessible
//        mockMvc.perform(get("/protected"))
//                .andExpect(status().isUnauthorized())
//                .andExpect(jsonPath("$.error").value("Unauthorized"));
//    }

//    @Test
//    void testCsrfDisabled() throws Exception {
//        mockMvc.perform(post("/auth/register")
//                        .contentType("application/json")
//                        .content("{\"emailAddress\":\"test@example.com\",\"password\":\"Password123\"}"))
//                .andExpect(status().isOk()); // CSRF is disabled, so POST should not require CSRF token
//    }
}