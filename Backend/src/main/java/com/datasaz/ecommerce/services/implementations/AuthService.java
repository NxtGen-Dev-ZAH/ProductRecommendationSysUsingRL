package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.UserAlreadyExistsException;
import com.datasaz.ecommerce.exceptions.UserNotFoundException;
import com.datasaz.ecommerce.exceptions.response.ExceptionMessages;
import com.datasaz.ecommerce.mappers.UserMapper;
import com.datasaz.ecommerce.models.dto.AuthResponse;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.LoginEmailRequest;
import com.datasaz.ecommerce.models.request.RegisterRequest;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import com.datasaz.ecommerce.repositories.RolesRepository;
import com.datasaz.ecommerce.repositories.UserRepository;
import com.datasaz.ecommerce.repositories.entities.RefreshToken;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IAuthService;
import com.datasaz.ecommerce.services.interfaces.IEmailService;
import com.datasaz.ecommerce.services.interfaces.IUserPrivacySettingsService;
import com.datasaz.ecommerce.utilities.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final RolesRepository rolesRepository;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    private final IEmailService emailService;
    private final IUserPrivacySettingsService userPrivacySettingsService;

    private static final String UPLOAD_DIR = "uploads/profile-pictures/";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    //private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    //simpler format
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{8,15}$|^\\+?[1-9]\\d{1,14}$");

    private AuthResponse createAuthResponse(User user, UserDetails userDetails) {
        String token = jwtUtil.generateToken(userDetails.getUsername(), userDetails.getAuthorities());
        String refreshToken = jwtUtil.generateRefreshToken(userDetails.getUsername());
        refreshTokenRepository.save(RefreshToken.builder()
                .userEmail(user.getEmailAddress())
                .token(refreshToken)
                .expiryDate(jwtUtil.getRefreshTokenExpiry(refreshToken))
                .revoked(false)
                .build());
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .email(userDetails.getUsername())
                .userRoles(user.getUserRoles())
                .provider(user.getProvider())
                .build();
    }

    /*
    private User initializeUser(UserDto userDto, String provider, String registrationIp) {
        User user = userMapper.toEntity(userDto);
        Roles buyerRole = rolesRepository.findByRole(RoleTypes.BUYER)
                .orElseGet(() -> rolesRepository.save(Roles.builder().role(RoleTypes.BUYER).build()));
        user.setUserRoles(Set.of(buyerRole));
        user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));
        user.setRegistrationIp(registrationIp);

        // verify if Company is correctly mapped for newly registered users, there is no company as yet, only login information
        user.setCompany(null);
        //user.setRegistrationDate(LocalDateTime.now());
        //user.setIsActivated(userDto.getIsActivated());
        //user.setIsBlocked(userDto.getIsBlocked());
        //user.setDeleted(userDto.getDeleted());
        user.setProvider(provider);
        return user;
    }
*/
    private User initializeUser(UserDto userDto, String provider, String registrationIp) {
        User user = userMapper.toEntity(userDto);
        Roles buyerRole = rolesRepository.findByRole(RoleTypes.BUYER)
                .orElseGet(() -> {
                    Roles newRole = Roles.builder().role(RoleTypes.BUYER).build();
                    return rolesRepository.saveAndFlush(newRole); // Use saveAndFlush to ensure immediate persistence
                });
        user.setUserRoles(Set.of(buyerRole));
        user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));
        user.setRegistrationIp(registrationIp);
        user.setProvider(provider);
        return user;
    }

    private String getClientIp(HttpServletRequest request, String clientIp) {
        log.info("getClientIp: request ip: {} and client IP {}", request.getRemoteAddr(), clientIp);
        //TODO: add clientIP in request; need further verification: client ip recuperation and/or ip format etc...
        if (clientIp == null || clientIp.isEmpty()) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            log.info("getClientIp: Retrieved IP address: {}", ip);
            return ip;
        } else
            return clientIp;
    }

    private void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth != null) {
            if (dateOfBirth.isAfter(LocalDate.now())) {
                log.error("Date of birth cannot be in the future: {}", dateOfBirth);
                throw IllegalParameterException.builder().message("Date of birth cannot be in the future").build();
            }
            if (Period.between(dateOfBirth, LocalDate.now()).getYears() < 18) {
                log.error("User must be at least 18 years old: {}", dateOfBirth);
                throw IllegalParameterException.builder().message("User must be at least 18 years old").build();
            }
        }
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest request, HttpServletRequest httpServletRequest) {
        log.info("registerUser: Attempting to register user with email: {}", request.getEmailAddress());

        if (request.getEmailAddress() == null || request.getEmailAddress().isEmpty() || !EMAIL_PATTERN.matcher(request.getEmailAddress()).matches()) {
            log.error("Invalid email format: {}", request.getEmailAddress());
            throw IllegalParameterException.builder().message("Invalid email format").build();
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            log.error("Error saving new user: password cannot be null or empty");
            throw BadRequestException.builder().message("Error saving new client: password cannot be null or empty.").build();
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.error("Passwords do not match");
            throw BadRequestException.builder().message("Passwords do not match").build();
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty() && !PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
            log.error("Invalid phone number format: {}", request.getPhoneNumber());
            throw IllegalParameterException.builder().message("Invalid phone number format").build();
        }

        validateDateOfBirth(request.getDateOfBirth());

        String registrationIp = getClientIp(httpServletRequest, request.getRegistrationIp());
        //String registrationIp = getClientIp(httpServletRequest);

        //Optional<User> existingUser = userRepository.findByEmailAddress(request.getEmailAddress());

        Optional<User> existingUser;
        try {
            existingUser = userRepository.findByEmailAddress(request.getEmailAddress());
        } catch (DataAccessException e) {
            log.error("Database error while checking existing user: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while checking existing user: " + e.getMessage(), e);
        }


        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.getDeleted()) {
                log.error("User already exists with email: {}", user.getEmailAddress());
                throw UserAlreadyExistsException.builder()
                        .message(ExceptionMessages.USER_ALREADY_EXISTS + " " + user.getEmailAddress())
                        .build();
            }

            log.info("registerUser: Restoring deleted user with email: {}", user.getEmailAddress());
            user.setDeleted(false);
            user.setIsActivated(false);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName() != null && !request.getFirstName().isEmpty() ? request.getFirstName() : user.getFirstName());
            user.setLastName(request.getLastName() != null && !request.getLastName().isEmpty() ? request.getLastName() : user.getLastName());
            user.setDateOfBirth(request.getDateOfBirth() != null ? request.getDateOfBirth() : user.getDateOfBirth());
            user.setPhoneNumber(request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty() ? request.getPhoneNumber() : user.getPhoneNumber());
            user.setRegistrationIp(registrationIp);
            user.setRegistrationDate(LocalDateTime.now());
            user.setProvider(null); // Reset provider for password-based registration
            user.setDeletionToken(null);
            user.setActivationCode(UUID.randomUUID().toString());

            try {
            if (user.getPrivacySettings() == null) {
                user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));
            }
                userRepository.save(user);
                auditLogService.logAction(request.getEmailAddress(), "RESTORE_USER", "User restored at: " + LocalDateTime.now() + ", IP: " + registrationIp);

                emailService.sendActivationEmail(request.getEmailAddress(), user.getActivationCode());
            } catch (DataAccessException e) {
                log.error("Database error while restoring user: {}", e.getMessage(), e);
                throw new RuntimeException("Database error while restoring user: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Failed to restore user: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to restore user: " + e.getMessage(), e);
            }
//            } catch (Exception e) {
//                log.error("Failed to send activation email to {}: {}", request.getEmailAddress(), e.getMessage());
//            }

            return AuthResponse.builder()
                    .email(user.getEmailAddress())
                    .message("Account restored. Please check your email to re-activate your account.")
                    .build();

//            try {
//                emailService.sendUserRestorationNotification(request.getEmailAddress(), LocalDateTime.now());
//            } catch (Exception e) {
//                log.error("Failed to send restoration notification to {}: {}", request.getEmailAddress(), e.getMessage());
//            }
//
//            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmailAddress());
//            return createAuthResponse(user, userDetails);
        }

        try {
            UserDto userDto = UserDto.builder()
                    .emailAddress(request.getEmailAddress())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .dateOfBirth(request.getDateOfBirth())
                    .phoneNumber(request.getPhoneNumber())
                    .isActivated(false)
                    .isBlocked(false)
                    .registrationDate(LocalDateTime.now())
                    .deleted(false)
                    .provider(null)
                    .build();

            User user = initializeUser(userDto, null, registrationIp);
            user.setActivationCode(UUID.randomUUID().toString());

//            User user = userMapper.toEntity(userDto);
//            Roles buyerRole = rolesRepository.findByRole(RoleTypes.BUYER)
//                    .orElseGet(() -> rolesRepository.save(Roles.builder().role(RoleTypes.BUYER).build()));
//            user.setUserRoles(Set.of(buyerRole));
//
//            user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));

            //userRepository.save(user);
            userRepository.saveAndFlush(user);
            auditLogService.logAction(request.getEmailAddress(), "REGISTER_USER", "New user registered at: " + user.getRegistrationDate() + ", IP: " + registrationIp);

            try {
                emailService.sendActivationEmail(request.getEmailAddress(), user.getActivationCode());
            } catch (Exception e) {
                log.error("Failed to send activation email to {}: {}", request.getEmailAddress(), e.getMessage());
            }

            return AuthResponse.builder()
                    .email(user.getEmailAddress())
                    .message("Registration successful. Please check your email to activate your account.")
                    .build();
//            try {
//                emailService.sendWelcomeNotification(request.getEmailAddress(), LocalDateTime.now());
//            } catch (Exception e) {
//                log.error("Failed to send welcome notification to {}: {}", request.getEmailAddress(), e.getMessage());
//            }
//
//            UserDetails userDetails = userDetailsService.loadUserByUsername(userDto.getEmailAddress());
//            return createAuthResponse(user, userDetails);
        } catch (Exception e) {
            log.error("Error while registering user: {}", e.getMessage());
            throw new RuntimeException("Error while registering user: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public String activateAccount(String token) {
        log.info("activateAccount: Processing account activation with token");

        Optional<User> userOpt = userRepository.findByActivationCode(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsActivated()) {
                log.info("Account for {} is already activated", user.getEmailAddress());
                return "Account is already activated.";
            }
            if (user.getIsBlocked()) {
                log.error("User {} is blocked", user.getEmailAddress());
                throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
            }

            user.setIsActivated(true);
            user.setActivationCode(null);
            userRepository.save(user);

            auditLogService.logAction(user.getEmailAddress(), "ACTIVATE_ACCOUNT", "Account activated at: " + LocalDateTime.now());

            try {
                emailService.sendWelcomeNotification(user.getEmailAddress(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send welcome notification to {}: {}", user.getEmailAddress(), e.getMessage());
            }

            return "Account activated successfully.";
        } else {
            log.error("Invalid activation token: {}", token);
            throw BadRequestException.builder().message("Invalid activation token. Please try again with a valid token.").build();
        }
    }

    @Override
    @Transactional
    public AuthResponse loginUser(LoginEmailRequest request) {
        log.info("loginUser: Attempting to login with email: {}", request.getEmailAddress());

        if (request.getEmailAddress() == null || request.getEmailAddress().isEmpty() || !EMAIL_PATTERN.matcher(request.getEmailAddress()).matches()) {
            log.error("Invalid email format: {}", request.getEmailAddress());
            throw new IllegalArgumentException("Invalid email format");
        }

        User user = userRepository.findByEmailAddressAndDeletedFalse(request.getEmailAddress())
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + request.getEmailAddress()).build());

        if (user.getProvider() != null) {
            log.error("User {} is registered with provider: {}", request.getEmailAddress(), user.getProvider());
            throw BadRequestException.builder().message("User already exists; login via : " + user.getProvider()).build();
        }

        if (!user.getIsActivated()) {
            log.error("User {} is not activated", request.getEmailAddress());
            throw BadRequestException.builder().message("Account not activated. Please check your email for the activation link.").build();
        }

        if (user.getIsBlocked()) {
            log.error("User {} is blocked", request.getEmailAddress());
            throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmailAddress(), request.getPassword()));
        } catch (DisabledException e) {
            log.error("Authentication failed for user {}: Account is disabled or blocked", request.getEmailAddress());
            throw BadRequestException.builder().message("Account is disabled or blocked").build();
        } catch (AccountExpiredException e) {
            log.error("Authentication failed for user {}: Account has expired", request.getEmailAddress());
            throw BadRequestException.builder().message("Account has expired").build();
        } catch (CredentialsExpiredException e) {
            log.error("Authentication failed for user {}: Credentials have expired", request.getEmailAddress());
            throw BadRequestException.builder().message("Credentials have expired").build();
        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", request.getEmailAddress(), e.getMessage());
            throw BadRequestException.builder().message("Invalid credentials").build();
        }

        //user.getIsActivated() && !user.getIsBlocked(),
        //                true, // accountNonExpired
        //                true, // credentialsNonExpired
        //                true, // accountNonLocked

        /*

        password: Starts with $2a$ (indicating BCrypt encoding).
        provider: NULL (for non-OAuth2 users).
        is_activated: true.
        is_blocked: false.
        deleted: false.

         */

        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmailAddress());
        return createAuthResponse(user, userDetails);
    }

    @Override
    @Transactional
    public AuthResponse processOAuth2User(OAuth2User oAuth2User, String provider, HttpServletRequest httpServletRequest) {
        String email = oAuth2User.getAttribute("email") == null ? null : oAuth2User.getAttribute("email").toString();
        String name = oAuth2User.getAttribute("name") != null ? oAuth2User.getAttribute("name") : "";
        String firstName = oAuth2User.getAttribute("given_name") != null ? oAuth2User.getAttribute("given_name") : "";
        String lastName = oAuth2User.getAttribute("family_name") != null ? oAuth2User.getAttribute("family_name") : "";
        String pictureUrl = oAuth2User.getAttribute("picture") != null ? oAuth2User.getAttribute("picture") : "";

        log.info("processOAuth2User: Processing OAuth2 login for email: {}, provider: {}", email, provider);

        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid or missing email in OAuth2 data: {}", email);
            throw new IllegalArgumentException("Invalid or missing email in OAuth2 data");
        }

        //TODO: verify registration IP
        String registrationIp = getClientIp(httpServletRequest, "");
        Optional<User> existingUser = userRepository.findByEmailAddressAndDeletedFalse(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (user.getIsBlocked()) {
                log.error("User {} is blocked", email);
                throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
            }
            user.setFirstName(!firstName.isEmpty() ? firstName : user.getFirstName());
            user.setLastName(!lastName.isEmpty() ? lastName : user.getLastName());
            user.setProfilePictureUrl(!pictureUrl.isEmpty() ? pictureUrl : user.getProfilePictureUrl());
            user.setLastLoginDate(LocalDateTime.now());
            user.setRegistrationIp(registrationIp);
            userRepository.save(user);
            auditLogService.logAction(email, "OAUTH2_LOGIN", "User " + name + " logged in via OAuth2 at: " + LocalDateTime.now() + ", provider: " + provider + ", IP: " + registrationIp);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            return createAuthResponse(user, userDetails);
        }

        Optional<User> deletedUser = userRepository.findByEmailAddress(email);
        if (deletedUser.isPresent()) {
            log.info("processOAuth2User: Restoring deleted user with email: {}", email);
            User user = deletedUser.get();
            if (user.getIsBlocked()) {
                log.error("User {} is blocked", email);
                throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
            }
            user.setFirstName(!firstName.isEmpty() ? firstName : user.getFirstName());
            user.setLastName(!lastName.isEmpty() ? lastName : user.getLastName());
            user.setProfilePictureUrl(!pictureUrl.isEmpty() ? pictureUrl : user.getProfilePictureUrl());
            user.setDeleted(false);
            user.setIsActivated(true);
            user.setProvider(provider);
            user.setRegistrationIp(registrationIp);
            user.setLastLoginDate(LocalDateTime.now());

            if (user.getPrivacySettings() == null) {
                user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));
            }

            userRepository.save(user);
            auditLogService.logAction(email, "OAUTH2_RESTORE_USER", "User restored via OAuth2 at: " + LocalDateTime.now() + ", provider: " + provider + ", IP: " + registrationIp);

            try {
                emailService.sendUserRestorationNotification(email, LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send restoration notification to {}: {}", email, e.getMessage());
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            return createAuthResponse(user, userDetails);
        }

        log.info("processOAuth2User: Creating new user with email: {}", email);
        //String first_Name = name != null ? name.split(" ")[0] : "Unknown";
        //String last_Name = name != null && name.contains(" ") ? name.substring(name.indexOf(" ") + 1) : "Unknown";

        UserDto userDto = UserDto.builder()
                .emailAddress(email)
                .password("nopassword")
                .firstName(firstName != null && !firstName.isEmpty() ? firstName : "Unknown")
                .lastName(lastName != null && !lastName.isEmpty() ? lastName : "Unknown")
                .phoneNumber("+0000000000")
                .provider(provider)
                .profilePictureUrl(pictureUrl)
                .isActivated(true)
                .isBlocked(false)
                .registrationDate(LocalDateTime.now())
                .deleted(false)
                .build();

        User user = initializeUser(userDto, provider, registrationIp);
        userRepository.save(user);
        auditLogService.logAction(email, "OAUTH2_REGISTER_USER", "New user registered via OAuth2 at: " + LocalDateTime.now() + ", provider: " + provider + ", IP: " + registrationIp);

        try {
            emailService.sendWelcomeNotification(email, LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send welcome notification to {}: {}", email, e.getMessage());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return createAuthResponse(user, userDetails);

//        if (!userRepository.findByEmailAddress(user.getEmailAddress()).isPresent()) {
//            log.info("processOAuth2User: user does not exist, creating new user");
//            Roles buyerRole = rolesRepository.findByRole(RoleTypes.BUYER)
//                    .orElseGet(() -> rolesRepository.save(Roles.builder().role(RoleTypes.BUYER).build()));
//            user.setUserRoles(Set.of(buyerRole));
//
//            user.setPrivacySettings(userPrivacySettingsService.createDefaultPrivacySettings(user));
//            userRepository.save(user);
//            auditLogService.logAction(user.getEmailAddress(), "OAUTH2_REGISTER_USER", "New user registered via OAuth2 at: " + user.getRegistrationDate() + " via provider: " + provider);
//
//        } else if (!userRepository.findByEmailAddressAndDeletedFalse(user.getEmailAddress()).isPresent()) {
//            log.info("processOAuth2User: user was deleted, restoring user");
//            user.setDeleted(false);
//            userRepository.save(user);
//            auditLogService.logAction(user.getEmailAddress(), "OAUTH2_RESTORE_USER", "User restored via OAuth2 at: " + user.getRegistrationDate() + " at login via provider: " + provider);
//        }
//
//        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
//        return createAuthResponse(user, userDetails);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        log.info("refreshToken: Attempting to refresh token");
        Optional<RefreshToken> refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken);
        if (refreshTokenEntity.isEmpty()) {
            log.error("refreshToken: Invalid refresh token");
            throw new IllegalArgumentException("Invalid refresh token");
        }
        RefreshToken token = refreshTokenEntity.get();
        if (token.isRevoked()) {
            log.error("refreshToken: Refresh token is revoked");
            throw new IllegalArgumentException("Refresh token is revoked");
        }
        if (jwtUtil.isTokenExpired(refreshToken) || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.error("refreshToken: Refresh token expired");
            refreshTokenRepository.delete(token);
            throw new IllegalArgumentException("Refresh token expired");
        }
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userRepository.findByEmailAddressAndDeletedFalse(username)
                .orElseThrow(() -> UserNotFoundException.builder().message("User not found with email: " + username).build());

        if (user.getIsBlocked()) {
            log.error("User {} is blocked", username);
            throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
        }

        String newToken = jwtUtil.generateToken(username, userDetailsService.loadUserByUsername(username).getAuthorities());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        refreshTokenRepository.save(RefreshToken.builder()
                .userEmail(username)
                .token(newRefreshToken)
                .expiryDate(jwtUtil.getRefreshTokenExpiry(newRefreshToken))
                .revoked(false)
                .build());

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .email(username)
                .userRoles(user.getUserRoles())
                .build();
    }

    @Override
    @Transactional
    public String forgotPassword(String email) {
        log.info("forgotPassword: Processing password reset request for email: {}", email);

        // Validate email
        if (email == null || email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            log.error("Invalid email format: {}", email);
            throw IllegalParameterException.builder().message("Invalid email format").build();
        }

        Optional<User> userOpt = userRepository.findByEmailAddress(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsBlocked()) {
                log.error("User {} is blocked", email);
                throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
            }
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            userRepository.save(user);

            auditLogService.logAction(email, "FORGOT_PASSWORD", "Password reset requested at: " + LocalDateTime.now());

            try {
                emailService.sendResetEmail(email, resetToken);
            } catch (Exception e) {
                log.error("Failed to send reset email to {}: {}", email, e.getMessage());
            }

            return "An email to reset the password has been sent to " + email + ". Please check the inbox or spam folder for the reset link.";
        } else {
            log.warn("No user found with email: {}", email);
            throw UserNotFoundException.builder().message("No user found with email " + email + ". Please check your email and try again.").build();
        }
    }

    @Override
    @Transactional
    public String resetPassword(String token, String newPassword, HttpServletRequest httpServletRequest) {
        log.info("resetPassword: Processing password reset with token");

        // Validate password
        if (newPassword == null || newPassword.isEmpty()) {
            log.error("New password cannot be null or empty");
            throw BadRequestException.builder().message("Missing token. Please try again with a valid token.").build();
        }

        if (token == null || token.isEmpty()) {
            log.error("resetPassword: Error missing token");
            throw BadRequestException.builder().message("Error saving reset Password status.").build();
        }

        Optional<User> userOpt = userRepository.findByResetToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getIsBlocked()) {
                log.error("User {} is blocked", user.getEmailAddress());
                throw BadRequestException.builder().message("Account is blocked. Please contact support.").build();
            }
            String email = user.getEmailAddress();
            //TODO: add clientIP in request
            String registrationIp = getClientIp(httpServletRequest, "");

            // Update password and clear reset token
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            user.setIsResetPassword(true);
            user.setLastPasswordResetDate(LocalDateTime.now());
            user.setRegistrationIp(registrationIp);

            userRepository.save(user);
            auditLogService.logAction(email, "RESET_PASSWORD", "Password reset at: " + LocalDateTime.now() + ", IP: " + registrationIp);

            try {
                emailService.sendPasswordChangeNotification(email, LocalDateTime.now());
            } catch (Exception e) {
                log.error("Failed to send password change notification to {}: {}", email, e.getMessage());
            }

            // Revoke all refresh tokens
            refreshTokenRepository.deleteByUserEmail(email);
            log.info("resetPassword: Revoked all refresh tokens for user: {}", email);

            return "Password reset successfully";
        } else {
            log.error("Invalid reset token: {}", token);
            throw BadRequestException.builder().message("Invalid token. Please try again with a valid token.").build();
        }
    }

}