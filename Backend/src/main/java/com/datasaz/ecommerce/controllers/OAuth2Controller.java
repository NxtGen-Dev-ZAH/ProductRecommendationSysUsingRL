package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.models.dto.AuthResponse;
import com.datasaz.ecommerce.services.interfaces.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth2")
public class OAuth2Controller {

    private final IAuthService authService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

//    @GetMapping("/google")
//    public ResponseEntity<AuthResponse> oauthSuccess(OAuth2AuthenticationToken principal, HttpServletRequest httpServletRequest) {
//        return ResponseEntity.ok(authService.processOAuth2User(principal.getPrincipal(), "google", httpServletRequest));
//    }

    @GetMapping("/google")
    public void oauthSuccess(OAuth2AuthenticationToken principal, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException {
        AuthResponse authResponse = authService.processOAuth2User(principal.getPrincipal(), "google", httpServletRequest);

// Redirect to frontend callback page with token data
        String tokenParam = URLEncoder.encode(authResponse.getToken(), StandardCharsets.UTF_8);
        String refreshTokenParam = authResponse.getRefreshToken() != null ?
                URLEncoder.encode(authResponse.getRefreshToken(), StandardCharsets.UTF_8) : "";
        String emailParam = URLEncoder.encode(authResponse.getEmail(), StandardCharsets.UTF_8);

        String redirectUrl = frontendUrl + "/auth/oauth-callback?token=" + tokenParam +
                "&refreshToken=" + refreshTokenParam +
                "&email=" + emailParam;

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/facebook")
    public ResponseEntity<AuthResponse> facebookLogin(OAuth2AuthenticationToken principal, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.processOAuth2User(principal.getPrincipal(), "facebook", httpServletRequest));
    }

    @GetMapping("/apple")
    public ResponseEntity<AuthResponse> appleLogin(OAuth2AuthenticationToken principal, HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.processOAuth2User(principal.getPrincipal(), "apple", httpServletRequest));
    }


}
