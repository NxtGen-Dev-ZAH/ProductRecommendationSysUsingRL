package com.datasaz.ecommerce.filters;

import com.datasaz.ecommerce.utilities.JwtBlacklistService;
import com.datasaz.ecommerce.utilities.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        log.info("JwtAuthenticationFilter: Processing request for {}, Method: {}, Time: {}",
                request.getRequestURI(), request.getMethod(), System.currentTimeMillis());

        // Skip authentication and authorization for public endpoints
        if (isPublicEndpoint(request)) {
            log.info("JwtAuthenticationFilter: Public endpoint, skipping authentication");
            chain.doFilter(request, response);
            return;
        }

        // Check for JWT token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ") && !jwtBlacklistService.isBlacklisted(token)) {
            token = token.substring(7); // Remove "Bearer " prefix
            log.info("JwtAuthenticationFilter: Found Bearer token for {}", request.getRequestURI());
            try {
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);
                log.info("JwtAuthenticationFilter: Token extracted - Username: {}, Roles: {}", username, roles);

                if (username != null && !jwtUtil.isTokenExpired(token)) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    log.info("JwtAuthenticationFilter: User details loaded - Username: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());

                    // Perform role-based authorization
                    String path = request.getServletPath();
                    if (path.startsWith("/buyer/") && !(roles.contains("ROLE_BUYER") || roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
                        log.warn("JwtAuthenticationFilter: Insufficient roles for /buyer/ endpoint");
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires BUYER, SELLER, or APP_ADMIN role");
                        return;
                    }

                    if (path.startsWith("/seller/") && !(roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
                        log.warn("JwtAuthenticationFilter: Insufficient roles for /seller/ endpoint");
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires SELLER or APP_ADMIN role");
                        return;
                    }

                    if (path.startsWith("/admin/") && !roles.contains("ROLE_APP_ADMIN")) {
                        log.warn("JwtAuthenticationFilter: Insufficient roles for /admin/ endpoint");
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires APP_ADMIN role");
                        return;
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("JwtAuthenticationFilter: Set JWT authentication for user: {}", username);
                } else {
                    log.warn("JwtAuthenticationFilter: Invalid or expired token for user: {}", username);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            } catch (Exception e) {
                log.error("JwtAuthenticationFilter: Error processing token: {}", e.getMessage());
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
        } else {
            log.warn("JwtAuthenticationFilter: No token or or OAuth2 authentication found; invalid/blacklisted token for {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
            return;
        }

        chain.doFilter(request, response);
    }


    private boolean isPublicEndpoint(HttpServletRequest request) {
        log.info("isPublicEndpoint: {}", request.getServletPath());
        String servletPath = request.getServletPath();
        boolean isPublic = servletPath.startsWith("/api/product") ||
                servletPath.startsWith("/api/category") ||
                servletPath.startsWith("/api/cart/add") ||
                servletPath.startsWith("/auth/login") ||
                servletPath.startsWith("/api") ||
                servletPath.startsWith("/auth/register") ||
                servletPath.startsWith("/register/user") ||
                servletPath.startsWith("/profile/visit") ||
                servletPath.startsWith("/swagger-ui") ||
                servletPath.startsWith("/v3/api-docs/") ||
                servletPath.startsWith("/api-docs") ||
                servletPath.startsWith("/swagger-resources") ||
                servletPath.startsWith("/api/user") ||
                servletPath.startsWith("/login") ||
                servletPath.startsWith("/auth") ||
                servletPath.startsWith("/oauth2") ||
                servletPath.startsWith("/static");
        log.info("JwtAuthenticationFilter: isPublicEndpoint: {} -> {}", servletPath, isPublic);
        return isPublic;
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + (status == HttpServletResponse.SC_UNAUTHORIZED ? "Unauthorized" : "Forbidden") + "\", \"message\": \"" + message + "\"}");
        log.info("JwtAuthenticationFilter: Sent error response - Status: {}, Message: {}", status, message);
    }
}


//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
//            throws ServletException, IOException {
//        log.info("JwtAuthenticationFilter: Processing request for {}, Time: {}", request.getRequestURI(), System.currentTimeMillis());
//
//        // Skip authentication and authorization for public endpoints
//        if (isPublicEndpoint(request)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        // Check for JWT token
//        String token = request.getHeader("Authorization");
//        if (token != null && token.startsWith("Bearer ")) {
//            token = token.substring(7); // Remove "Bearer " prefix
//            log.info("JwtAuthenticationFilter: Found Bearer token");
//            try {
//                String username = jwtUtil.extractUsername(token);
//                List<String> roles = jwtUtil.extractRoles(token);
//                log.info("JwtAuthenticationFilter: Token extracted - Username: {}, Roles: {}", username, roles);
//
//                if (username != null && !jwtUtil.isTokenExpired(token)) {
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                    log.info("JwtAuthenticationFilter: User details loaded - Username: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
//
//                    // Perform role-based authorization
//                    String path = request.getServletPath();
//                    if (path.startsWith("/buyer/") && !(roles.contains("ROLE_BUYER") || roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires BUYER, SELLER, or APP_ADMIN role");
//                        return;
//                    }
//
//                    if (path.startsWith("/seller/") && !(roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires SELLER or APP_ADMIN role");
//                        return;
//                    }
//
//                    if (path.startsWith("/admin/") && !roles.contains("ROLE_APP_ADMIN")) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires APP_ADMIN role");
//                        return;
//                    }
//
//                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                            userDetails, null, userDetails.getAuthorities());
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                    log.info("JwtAuthenticationFilter: Set JWT authentication for user: {}", username);
//                } else {
//                    log.warn("JwtAuthenticationFilter: Invalid or expired token for user: {}", username);
//                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
//                    return;
//                }
//            } catch (Exception e) {
//                log.error("JwtAuthenticationFilter: Error processing token: {}", e.getMessage());
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
//                return;
//            }
//        } else if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken oauthToken) {
//            String email = oauthToken.getPrincipal().getAttribute("email");
//            if (email == null) {
//                log.error("JwtAuthenticationFilter: No email found in OAuth2 token");
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No email found in OAuth2 token");
//                return;
//            }
//            try {
//                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
//                log.info("JwtAuthenticationFilter: OAuth2 user details loaded - Username: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                        userDetails, null, userDetails.getAuthorities());
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//                log.info("JwtAuthenticationFilter: Set OAuth2 authentication for user: {}", email);
//
/// /                String path = request.getServletPath();
/// /                if (path.startsWith("/buyer/") && !(roles.contains("ROLE_BUYER") || roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires BUYER, SELLER, or APP_ADMIN role");
/// /                    return;
/// /                }
/// /
/// /                if (path.startsWith("/seller/") && !(roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires SELLER or APP_ADMIN role");
/// /                    return;
/// /                }
/// /
/// /                if (path.startsWith("/admin/") && !roles.contains("ROLE_APP_ADMIN")) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires APP_ADMIN role");
/// /                    return;
/// /                }
//
//            } catch (Exception e) {
//                log.error("JwtAuthenticationFilter: Error loading user details for OAuth2 user: {}", email, e);
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Failed to load user details");
//                return;
//            }
//        } else {
//            log.warn("JwtAuthenticationFilter: No token or OAuth2 authentication found");
//            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
//            return;
//        }
//
//        chain.doFilter(request, response);
//    }

//    private final UserDetailsService userDetailsService;
//
//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
//            throws ServletException, IOException {
//        log.info("JwtAuthenticationFilter: Processing request for {}, Time: {}", request.getRequestURI(), System.currentTimeMillis());
//
//        // Skip authentication and authorization for public endpoints
//        if (isPublicEndpoint(request)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        //String token = request.getHeader("Authorization");
//
//        // Check for JWT token first
//        String token = request.getHeader("Authorization");
//        if (token != null && token.startsWith("Bearer ")) {
//            token = token.substring(7); // Remove "Bearer " prefix
//            try {
//                String username = JwtUtil.extractUsername(token);
//                List<String> roles = JwtUtil.extractRoles(token);
//
//                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//                    log.info("JwtAuthenticationFilter: User details: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
//
//                    // Perform role-based authorization
//                    String path = request.getServletPath();
//                    if (path.startsWith("/buyer/") && !(roles.contains("ROLE_BUYER") || roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires BUYER, SELLER, or APP_ADMIN role");
//                        return;
//                    }
//
//                    if (path.startsWith("/seller/") && !(roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires SELLER or APP_ADMIN role");
//                        return;
//                    }
//
//                    if (path.startsWith("/admin/") && !roles.contains("ROLE_APP_ADMIN")) {
//                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires APP_ADMIN role");
//                        return;
//                    }
//
//                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                            userDetails, null, userDetails.getAuthorities());
//                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                    SecurityContextHolder.getContext().setAuthentication(authentication);
//                    log.info("JwtAuthenticationFilter: Set JWT authentication for user: {}", username);
//                }
//            } catch (Exception e) {
//                log.error("JwtAuthenticationFilter: Error processing token", e);
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
//                return;
//            }
//        } else if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken oauthToken) {
//            String email = oauthToken.getPrincipal().getAttribute("email");
//            if (email == null) {
//                log.error("JwtAuthenticationFilter: No email found in OAuth2 token");
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No email found in OAuth2 token");
//                return;
//            }
//            try {
//                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
//                log.info("JwtAuthenticationFilter: OAuth2 user details: {}, Authorities: {}", userDetails.getUsername(), userDetails.getAuthorities());
//
//                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                        userDetails, null, userDetails.getAuthorities());
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//                log.info("JwtAuthenticationFilter: Set OAuth2 authentication for user: {}", email);
//            } catch (Exception e) {
//                log.error("JwtAuthenticationFilter: Error loading user details for OAuth2 user: {}", email, e);
//                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Failed to load user details");
//                return;
//            }
//        } else {
//            log.warn("JwtAuthenticationFilter: Missing or invalid token");
//            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
//            return;
//        }
//
//        chain.doFilter(request, response);
//
//

/// /        if (token == null || !token.startsWith("Bearer ")) {
/// /            log.warn("JwtAuthenticationFilter: Missing or invalid token");
/// /            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
/// /            return;
/// /        }
/// /
/// /        token = token.substring(7); // Remove "Bearer " prefix
/// /        try {
/// /            String username = JwtUtil.extractUsername(token);
/// /            List<String> roles = JwtUtil.extractRoles(token);
/// /
/// /            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
/// /                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
/// /                log.info("JwtAuthenticationFilter: User details: {}", userDetails.getUsername());
/// /
/// /                // Perform role-based authorization
/// /                String path = request.getServletPath();
/// /                if (path.startsWith("/buyer/") && !(roles.contains("ROLE_BUYER") || roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires BUYER, SELLER, or APP_ADMIN role");
/// /                    return;
/// /                }
/// /
/// /                if (path.startsWith("/seller/") && !(roles.contains("ROLE_SELLER") || roles.contains("ROLE_APP_ADMIN"))) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires SELLER or APP_ADMIN role");
/// /                    return;
/// /                }
/// /
/// /                if (path.startsWith("/admin/") && !roles.contains("ROLE_APP_ADMIN")) {
/// /                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "Requires APP_ADMIN role");
/// /                    return;
/// /                }
/// /
/// /                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
/// /                        userDetails, null, userDetails.getAuthorities());
/// /                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
/// /                SecurityContextHolder.getContext().setAuthentication(authentication);
/// /            }
/// /
/// /            chain.doFilter(request, response);
/// /        } catch (Exception e) {
/// /            log.error("JwtAuthenticationFilter: Error processing token", e);
/// /            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
/// /        }
//    }

//    @Override
//    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
//            throws ServletException, IOException {
//        log.info("JwtAuthenticationFilter: doFilterInternal at {}, Time : {}", request.getRequestURI(), System.currentTimeMillis());
//        // Skip JWT validation for login  and public endpoints
//
//        if (isPublicEndpoint(request)) {
//            chain.doFilter(request, response);
//            return;
//        }
//
//        String token = request.getHeader("Authorization");
//        if (token == null || !token.startsWith("Bearer ")) {
//            log.warn("JwtAuthenticationFilter: Missing or invalid token");
//            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//            response.setContentType("application/json");
//            response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Missing or invalid token\"}");
//            return;
//        }
//
//        token = token.substring(7); // Remove "Bearer " prefix
//        String username = JwtUtil.extractUsername(token);
//        log.info("JwtAuthenticationFilter: User name: {}", username);
//
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//            log.info("JwtAuthenticationFilter: User details: {}", userDetails);
//
//            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                    userDetails, null, userDetails.getAuthorities());
//
//            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//        }
//
//        chain.doFilter(request, response);
//    }
