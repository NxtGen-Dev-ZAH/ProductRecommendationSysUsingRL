package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.UserFollowersCountResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.models.response.UserSummaryResponse;
import com.datasaz.ecommerce.repositories.entities.UserPrivacySettings;
import com.datasaz.ecommerce.services.interfaces.IUserProfileVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Profile Management", description = "APIs for managing user profiles, followers, favorites, and privacy settings")
@RequiredArgsConstructor
@RestController
@RequestMapping("/profile/visit")
public class UserProfileVisitController {

    private final IUserProfileVisitService userProfileService;

    @Operation(summary = "Get user followers", description = "Retrieves a paginated list of followers for a specified user, respecting privacy settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Followers retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{email}/followers")
    public ResponseEntity<Page<UserSummaryResponse>> getFollowers(
            @Parameter(description = "Email of the user whose followers are retrieved") @PathVariable String email,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userProfileService.getFollowers(email, page, size, viewerEmail));
    }

    @Operation(summary = "Get user followings", description = "Retrieves a paginated list of users followed by a specified user, respecting privacy settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Followings retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{email}/following")
    public ResponseEntity<Page<UserSummaryResponse>> getFollowings(
            @Parameter(description = "Email of the user whose followings are retrieved") @PathVariable String email,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        String viewerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userProfileService.getFollowings(email, page, size, viewerEmail));
    }

    @Operation(summary = "Get follower count", description = "Retrieves the number of followers for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved follower count"),
            @ApiResponse(responseCode = "400", description = "Invalid email format"),
            @ApiResponse(responseCode = "403", description = "Access denied due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{email}/follower-count")
    public ResponseEntity<UserFollowersCountResponse> getFollowerCount(@Parameter(description = "Email address of the user") @PathVariable("email") String emailAddress) {
        return ResponseEntity.ok(userProfileService.getFollowerCount(emailAddress));
    }

    @Operation(summary = "Get following count", description = "Retrieves the number of users the specified user is following")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved following count"),
            @ApiResponse(responseCode = "400", description = "Invalid email format"),
            @ApiResponse(responseCode = "403", description = "Access denied due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{email}/following-count")
    public ResponseEntity<UserFollowersCountResponse> getFollowingCount(@Parameter(description = "Email address of the user") @PathVariable("email") String emailAddress) {
        return ResponseEntity.ok(userProfileService.getFollowingCount(emailAddress));
    }

    @Operation(summary = "Get user profile", description = "Retrieves the profile of a specified user, respecting privacy settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "500", description = "Could not execute statement")

    })
    @GetMapping("/{email}")
    public ResponseEntity<UserProfileResponse> getProfile(
            @Parameter(description = "Email of the user to retrieve") @PathVariable String email) {
        return ResponseEntity.ok(userProfileService.getProfile(email));
    }

    @Operation(summary = "Get user favorite products", description = "Retrieves a paginated list of favorite products for a specified user, respecting privacy settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite products retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden due to privacy settings"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{email}/favorites")
    public ResponseEntity<Page<ProductResponse>> getFavoriteProducts(
            @Parameter(description = "Email of the user whose favorite products are retrieved") @PathVariable String email,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userProfileService.getFavoriteProducts(email, page, size));
    }

    @Operation(summary = "Get privacy settings", description = "Retrieves the authenticated user's privacy settings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully retrieved privacy settings"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me/privacy-settings")
    public ResponseEntity<UserPrivacySettings> getPrivacySettings() {
        return ResponseEntity.ok(userProfileService.getPrivacySettings());
    }

//    @GetMapping("/me/privacy-settings")
//    public ResponseEntity<UserProfileResponse> getPrivacySettings() {
//        String email = SecurityContextHolder.getContext().getAuthentication().getName();
//        return ResponseEntity.ok(userProfileService.getPrivacySettings(email));
//    }

}
