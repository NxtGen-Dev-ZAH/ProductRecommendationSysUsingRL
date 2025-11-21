package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.models.dto.AuthResponse;
import com.datasaz.ecommerce.models.dto.ErrorResponse;
import com.datasaz.ecommerce.models.request.ForgotPasswordRequest;
import com.datasaz.ecommerce.models.request.LoginEmailRequest;
import com.datasaz.ecommerce.models.request.RegisterRequest;
import com.datasaz.ecommerce.models.request.ResetPasswordRequest;
import com.datasaz.ecommerce.services.interfaces.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final IAuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AuthResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest, HttpServletRequest httpServletRequest) {
        log.info("AuthController.register: Registering user with email: {}", registerRequest.getEmailAddress());
        return ResponseEntity.ok(authService.registerUser(registerRequest, httpServletRequest));
    }

    @GetMapping("/activate-account")
    public ResponseEntity<String> activateAccount(@RequestParam String token) {
        log.info("LoginController.activateAccount: Processing account activation with token");
        String response = authService.activateAccount(token);
        return ResponseEntity.ok(response);
    }

    // QA UNIT: Tested Login via provider Google and it works fine
    @GetMapping("/login/form-consumes-json")
    public String loginPage2() {
        return "<form action=\"/ecommerce/auth/login/submit\" method=\"POST\">\n" +
                "    <h2>Login</h2>\n" +
                "    <input type=\"text\" name=\"email\" placeholder=\"Email Address\" required>\n" +
                "    <input type=\"password\" name=\"password\" placeholder=\"Password\" required>\n" +
                "    <button type=\"submit\">Login</button>\n" +
                "</form>\n" +
                "\n" +
                "<a href=\"/ecommerce/auth/forgot-password\">Forgot Password</a>\n" +
                "<a href=\"/ecommerce/auth/register\">Register an account</a>\n" +
                "<h3>Or login with:</h3>\n" +
                "<a href=\"/ecommerce/oauth2/authorization/google\">Login with Google</a>\n" +
                "<a href=\"/ecommerce/oauth2/authorization/facebook\">Login with Facebook</a>\n";
    }

    @GetMapping("/login/form")
    public String loginPage() {
        return "<form action=\"/ecommerce/auth/login/submit-form\" method=\"POST\">\n" +
                "    <h2>Login</h2>\n" +
                "    <input type=\"text\" name=\"email\" placeholder=\"Email Address\" required>\n" +
                "    <input type=\"password\" name=\"password\" placeholder=\"Password\" required>\n" +
                "    <button type=\"submit\">Login</button>\n" +
                "</form>\n" +
                "\n" +
                "<a href=\"/ecommerce/auth/forgot-password\">Forgot Password</a>\n" +
                "<a href=\"/ecommerce/auth/register\">Register an account</a>\n" +
                "<h3>Or login with:</h3>\n" +
                "<a href=\"/ecommerce/oauth2/authorization/google\">Login with Google</a>\n" +
                "<a href=\"/ecommerce/oauth2/authorization/facebook\">Login with Facebook</a>\n";
    }

    @PostMapping("/login/submit")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginEmailRequest request) {
        log.info("LoginController.loginUser: " + request.toString());
        AuthResponse authResponse = authService.loginUser(request);

        HttpHeaders headers = new HttpHeaders();
        ResponseCookie accessCookie = ResponseCookie.from("token", authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .build();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authResponse.getRefreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Lax")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        return ResponseEntity.ok().headers(headers).body(authResponse);
    }

    @PostMapping(value = "/login/submit-form", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<AuthResponse> loginUserForm(@RequestParam String email, @RequestParam String password) {
        log.info("LoginController.loginUserForm: email={}", email);
        LoginEmailRequest request = new LoginEmailRequest();
        request.setEmailAddress(email);
        request.setPassword(password);
        AuthResponse authResponse = authService.loginUser(request);

        HttpHeaders headers = new HttpHeaders();
        ResponseCookie accessCookie = ResponseCookie.from("token", authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .build();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authResponse.getRefreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Lax")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        return ResponseEntity.ok().headers(headers).body(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody String refreshToken) {
        AuthResponse authResponse = authService.refreshToken(refreshToken);

        HttpHeaders headers = new HttpHeaders();
        ResponseCookie accessCookie = ResponseCookie.from("token", authResponse.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .build();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (authResponse.getRefreshToken() != null) {
            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", authResponse.getRefreshToken())
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Lax")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        }

        return ResponseEntity.ok().headers(headers).body(authResponse);
    }

    @GetMapping("/error")
    public ResponseEntity<ErrorResponse> loginError(@RequestParam String message) {
        log.error("LoginController.loginError: " + message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                        .errorCode(HttpStatus.UNAUTHORIZED.value())
                        .message(message)
                        .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest httpServletRequest) {
        log.info("LoginController.resetPassword: Processing password reset with token");
        String response = authService.resetPassword(request.getToken(), request.getNewPassword(), httpServletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("LoginController.forgotPassword: Processing password reset request for email: {}", request.getEmail());
        String response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }
}
