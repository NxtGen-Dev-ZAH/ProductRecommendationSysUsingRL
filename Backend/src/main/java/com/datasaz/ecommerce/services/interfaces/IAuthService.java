package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.AuthResponse;
import com.datasaz.ecommerce.models.request.LoginEmailRequest;
import com.datasaz.ecommerce.models.request.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;

public interface IAuthService {

    AuthResponse registerUser(RegisterRequest request, HttpServletRequest httpServletRequest);

    AuthResponse loginUser(LoginEmailRequest request);

    AuthResponse processOAuth2User(OAuth2User oAuth2User, String provider, HttpServletRequest httpServletRequest);
    AuthResponse refreshToken(String refreshToken);


    String forgotPassword(String email);

    String resetPassword(String token, String newPassword, HttpServletRequest httpServletRequest);

    String activateAccount(String token);

}
