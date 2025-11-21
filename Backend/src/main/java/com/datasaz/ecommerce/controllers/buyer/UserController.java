package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.exceptions.BadRequestException;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.LoginEmailRequest;
import com.datasaz.ecommerce.models.request.UpdatePasswordRequest;
import com.datasaz.ecommerce.services.interfaces.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/buyer/user")
public class UserController {

    private final IUserService userService;

    private String extractJwtToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            log.error("Invalid or missing Authorization header");
            throw BadRequestException.builder().message("Invalid or missing Authorization header").build();
        }
        return authorizationHeader.substring(7);
    }

    @PutMapping("/update-password")
    public ResponseEntity<UserDto> updateUserPassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("UserController.updatePassword: Updating password for email: {}", request.getEmail());
        String jwtToken = extractJwtToken(authorizationHeader);
        UserDto userDto = userService.updateUserPassword(
                request.getEmail(),
                request.getOldPassword(),
                request.getNewPassword(),
                jwtToken
        );
        return ResponseEntity.ok(userDto);
    }

//    @PutMapping("/update")
//    public ResponseEntity<UserDto> updateUser(
//            @RequestBody UserDto userDto,
//            @RequestHeader("Authorization") String authorizationHeader) {
//        log.info("UserController.updateUser: Updating profile for email: {}", userDto.getEmailAddress());
//        String jwtToken = extractJwtToken(authorizationHeader);
//        UserDto updatedUser = userService.updateUser(userDto, jwtToken);
//        return ResponseEntity.ok(updatedUser);
//    }

    @PutMapping("/change-email")
    public ResponseEntity<UserDto> changeEmail(
            @Valid @RequestBody LoginEmailRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("UserController.changeEmail: Changing email for user: {} to {}", userDetails.getUsername(), request.getEmailAddress());

        String jwtToken = extractJwtToken(authorizationHeader);
        UserDto updatedUser = userService.changeEmail(
                userDetails.getUsername(),
                request.getEmailAddress(),
                request.getPassword(),
                jwtToken
        );
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Void> deleteUserAccount(
            @RequestBody String password,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("UserController.deleteUserAccount: Initiating account deletion for authenticated user");
        String jwtToken = extractJwtToken(authorizationHeader);
        userService.deleteUser(password, jwtToken);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/confirm-delete-account")
    public ResponseEntity<UserDto> confirmDeleteUserAccount(
            @RequestParam String deletionToken,
            @RequestHeader("Authorization") String authorizationHeader) {
        log.info("UserController.confirmDeleteUserAccount: Confirming account deletion with token");
        String jwtToken = extractJwtToken(authorizationHeader);
        UserDto deletedUser = userService.confirmDeleteUser(deletionToken, jwtToken);
        return ResponseEntity.ok(deletedUser);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestParam String refreshToken,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authorizationHeader) {
        //String refreshToken = request.get("refreshToken");
        log.info("UserController.logout: Processing logout for user: {}, refreshToken: {}",
                userDetails.getUsername(), refreshToken);
        String jwtToken = extractJwtToken(authorizationHeader);
        String message = userService.logout(userDetails.getUsername(), refreshToken, jwtToken);

        HttpHeaders headers = new HttpHeaders();
        // Clear cookies by setting Max-Age=0
        ResponseCookie clearAccess = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        ResponseCookie clearRefresh = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        headers.add(HttpHeaders.SET_COOKIE, clearAccess.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearRefresh.toString());

        return ResponseEntity.ok().headers(headers).body(message);
    }
}
