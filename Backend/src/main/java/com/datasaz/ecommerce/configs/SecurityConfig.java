package com.datasaz.ecommerce.configs;

import com.datasaz.ecommerce.filters.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(
                                        "/api/**",
                                        "/oauth2/**",
                                        "/oauth2/google",
                                        "/auth/**",
                                        "/auth/login/submit",
                                        "/profile/visit/**",
                                        "/swagger-ui/**",
                                        "/api-docs",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/webjars/**",
                                        "/v3/api-docs.yaml",
                                        "/swagger-resources/**",
                                        "/swagger-resources/configuration/ui",
                                        "/swagger-resources/configuration/security",
                                        "/swagger-ui.html/**",
                                        "/register/user",
                                        "/api/user",
                                        "/api/product/**",
                                        "/api/category/**",
                                        "/api/cart/add",
                                        "/uploads/**",
                                        "/static/**"
                                ).permitAll()
                                .requestMatchers("/buyer/**").hasAnyAuthority("ROLE_BUYER", "ROLE_SELLER", "ROLE_APP_ADMIN")
                                .requestMatchers("/seller/**").hasAnyAuthority("ROLE_SELLER", "ROLE_APP_ADMIN")
                                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_APP_ADMIN")
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oAuth2Login -> {
                    oAuth2Login.successHandler(new AuthenticationSuccessHandler() {
                                @Override
                                public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
                                    log.info("OAuth2 authentication successful for user: {}", authentication.getName());
                                    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
                                    String registrationId = oauthToken.getAuthorizedClientRegistrationId();

                                    response.sendRedirect(frontendUrl + "/oauth2/" + registrationId);
                                }
                            })
                            .loginPage(frontendUrl + "/auth/login/form")
                            .failureHandler((request, response, exception) -> {
                                log.error("OAuth2 authentication failed: {}", exception.getMessage());
                                response.sendRedirect(frontendUrl + "/auth/error?message=" + exception.getMessage());
                            });
                })
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("https://shopora.fr", "https://www.shopora.fr", "https://api.shopora.fr", "http://localhost:3000"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowCredentials(true);
                    return config;
                }))
                .logout(logout -> {
                    logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl(frontendUrl + "/auth/login/form")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID");
                })
                .exceptionHandling(exceptionHandling -> {
                    exceptionHandling
                            .authenticationEntryPoint((request, response, authException) -> {
                                log.warn("Unauthorized access to {}: {}", request.getRequestURI(), authException.getMessage());
                                if (request.getRequestURI().contains("/auth/login/form")) {
                                    response.sendRedirect(frontendUrl + "/auth/error?message=" + authException.getMessage());
                                } else {
                                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                    response.setContentType("application/json");
                                    response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + authException.getMessage() + "\"}");
                                }
                            })
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                log.warn("Access denied to {}: {}", request.getRequestURI(), accessDeniedException.getMessage());
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" + accessDeniedException.getMessage() + "\"}");
                            });
                });
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


//    // associate role based access (manage authorities)
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http.csrf(AbstractHttpConfigurer::disable)
//                .authorizeHttpRequests(authorizeRequests ->
//                        authorizeRequests
//                                .requestMatchers("/login",
//                                        "/login/google", // Google, Facebook, Apple OAuth2 endpoints
//                                        "/oauth2/**", // Google, Facebook, Apple OAuth2 endpoints
//                                        "/auth/**",
//                                        "/register/user", // Login page
//                                        "/api/user", //register or find user
//                                        "/api/product/**",        // Product listings
//                                        "/api/category/**",     // Category listings
//                                        "/cart/add",        // Add to cart
//                                        "/uploads/**",
//                                        "/static/**"             // CSS/JS/images
//                                ).permitAll()
//                                //.requestMatchers("/buyer/**").hasRole("BUYER")
//                                .requestMatchers("/buyer/**").hasAnyAuthority("BUYER")
//                                // Seller endpoints
//                                //.requestMatchers("/buyer/**", "/seller/**").hasRole("SELLER")
//                                .requestMatchers("/buyer/**", "/seller/**").hasAnyAuthority("SELLER")
//                                // Admin endpoints
//                                //.requestMatchers("/buyer/**", "/seller/**", "/admin/**").hasRole("APP_ADMIN")
//                                .requestMatchers("/buyer/**", "/seller/**", "/admin/**").hasAnyAuthority("APP_ADMIN")
//                                // User profile
//                                //.requestMatchers("/api/users/**").authenticated()
//                                // APP_ADMIN and COMPANY_ADMIN have full access
//                                //.anyRequest().hasAnyAuthority("APP_ADMIN")
//                                .anyRequest().hasAnyRole("APP_ADMIN")
//                                //.requestMatchers("/buyer/**").hasRole(RoleTypes.BUYER.name() || RoleTypes.SELLER.name() RoleTypes.APP_ADMIN.name()) //Added 2025051400
//                                //.requestMatchers("/seller/**").hasRole(RoleTypes.SELLER.name() RoleTypes.APP_ADMIN.name()) //Added 2025051400
//                                //.requestMatchers("/admin/**").hasRole(RoleTypes.APP_ADMIN.name()) //Added 2025051400
//                        //.anyRequest().authenticated()
//                ).addFilterBefore(new JwtAuthenticationFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class)
//                .oauth2Login(oAuth2Login -> {
//                    oAuth2Login.successHandler(new AuthenticationSuccessHandler() {
//                        @Override
//                        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
//                            //response.sendRedirect("/ecommerce/login/google");
//                            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
//                            String registrationId = oauthToken.getAuthorizedClientRegistrationId();
//
//                            if ("google".equals(registrationId)) {
//                                response.sendRedirect("/ecommerce/login/google");
//                            } else if ("facebook".equals(registrationId)) {
//                                response.sendRedirect("/ecommerce/login/facebook");
//                            } else {
//                                response.sendRedirect("/ecommerce/login/default");
//                            }
//                        }
//                    }).loginPage("/ecommerce/auth/login/form").failureHandler((request, response, exception) -> {
//                        response.sendRedirect("/ecommerce/auth/error?message=" + exception.getMessage());
//                    });
//                }).logout(logout -> {
//                    logout.logoutSuccessUrl("/ecommerce/auth/logout");
//                }).exceptionHandling(exceptionHandling -> {
//                    exceptionHandling.authenticationEntryPoint((request, response, authException) -> {
//                        if (request.getRequestURI().contains("/ecommerce/auth/login/form")) {
//                            response.sendRedirect("/ecommerce/auth/error?message=" + authException.getMessage());
//                        }
//                    });
//                });
//        return http.build();
//    }