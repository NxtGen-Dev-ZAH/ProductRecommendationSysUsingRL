package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.request.UserPrivacySettingsRequest;
import com.datasaz.ecommerce.models.request.UserProfileRequest;
import com.datasaz.ecommerce.models.response.ProductResponse;
import com.datasaz.ecommerce.models.response.ProfilePictureResponse;
import com.datasaz.ecommerce.models.response.UserProfileResponse;
import com.datasaz.ecommerce.services.interfaces.IUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User Profile Management", description = "APIs for managing user profiles, followers, favorites, and privacy settings")
@RequiredArgsConstructor
@RestController
@RequestMapping("/buyer/profile")
public class UserProfileController {

    private final IUserProfileService userProfileService;

    @Operation(summary = "Toggle follow/unfollow user", description = "Toggles the follow status of the authenticated user for a specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Follow status toggled successfully", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Cannot follow self or invalid request"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/{targetEmail}/toggle-follow")
    public ResponseEntity<String> toggleFollow(
            @Parameter(description = "Email of the user to follow/unfollow") @PathVariable String targetEmail) {
        String followerEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userProfileService.toggleFollow(targetEmail, followerEmail));
    }

    @Operation(summary = "Update authenticated user's profile", description = "Updates profile details for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me/update-profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Parameter(description = "Profile update details") @RequestBody UserProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userProfileService.updateProfile(request));
    }

    @Operation(summary = "Update privacy settings", description = "Updates privacy settings for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Privacy settings updated successfully", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid visibility settings"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/me/privacy-settings")
    public ResponseEntity<UserProfileResponse> updatePrivacySettings(
            @Parameter(description = "Privacy settings update details") @RequestBody UserPrivacySettingsRequest request) {
        return ResponseEntity.ok(userProfileService.updatePrivacySettings(request));
    }

    @Operation(summary = "Get authenticated user's favorite products", description = "Retrieves a paginated list of favorite products for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite products retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me/favorites")
    public ResponseEntity<Page<ProductResponse>> getMyFavoriteProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(userProfileService.getFavoriteProducts(email, page, size));
    }

    @Operation(summary = "Toggle favorite product", description = "Toggles the favorite status of a product for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Favorite status toggled successfully", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "404", description = "User or product not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping("/toggle-favorite/{productId}")
    public ResponseEntity<String> toggleFavoriteProduct(
            @Parameter(description = "ID of the product to favorite/unfavorite") @PathVariable Long productId) {
        return ResponseEntity.ok(userProfileService.toggleFavoriteProduct(productId));
    }

    @Operation(summary = "Upload profile picture", description = "Uploads a profile picture for the authenticated user, supporting both file upload and Base64-encoded image")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile picture uploaded successfully", content = @Content(schema = @Schema(implementation = ProfilePictureResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid image or size exceeded"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
            @ApiResponse(responseCode = "500", description = "Internal server error during file upload")
    })
    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfilePictureResponse> uploadProfilePicture(
            @Parameter(description = "Image file (PNG, JPEG, etc.)", required = false) @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(description = "Profile request with Base64-encoded image", required = false) @RequestPart(value = "request", required = false) UserProfileRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if ((image == null || image.isEmpty()) && (request == null || request.getProfilePictureBase64() == null || request.getProfilePictureBase64().isEmpty())) {
            return ResponseEntity.badRequest().body(ProfilePictureResponse.builder()
                    .message("Image file or Base64 image content is required")
                    .build());
        }
        String profilePictureUrl;
        if (image != null && !image.isEmpty()) {
            profilePictureUrl = userProfileService.uploadProfilePicture(image, email);
        } else {
            profilePictureUrl = userProfileService.uploadProfilePicture(request, email);
        }
        return ResponseEntity.ok(ProfilePictureResponse.builder()
                .profilePictureUrl(profilePictureUrl != null ? profilePictureUrl : "")
                .message("Profile picture uploaded successfully")
                .build());
    }
}



//    @PostMapping(value = "/me/profile-picture", consumes = "multipart/form-data")
//    public ResponseEntity<UserProfileResponse> uploadProfilePicture(
//            @Parameter(description = "Profile picture file (jpg, png, gif)", required = true) @RequestPart("file") MultipartFile file,
//            @AuthenticationPrincipal Principal principal) {
//        if (principal == null || principal.getName() == null || principal.getName().isEmpty()) {
//            throw new SecurityException("Authentication required");
//        }
//        return ResponseEntity.ok(userProfileService.uploadProfilePicture(file, principal.getName()));
//    }

//    @Operation(summary = "Upload profile picture", description = "Uploads a profile picture for the authenticated user")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "Successfully uploaded profile picture"),
//            @ApiResponse(responseCode = "400", description = "Invalid or empty file"),
//            @ApiResponse(responseCode = "401", description = "Unauthorized")
//    })
//    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<UserDto> uploadProfilePicture(
//            @Parameter(description = "Profile picture file") @RequestParam("file") MultipartFile file,
//            @AuthenticationPrincipal Principal principal) {
//        if (principal == null) {
//            throw new SecurityException("Authentication required");
//        }
//        return ResponseEntity.ok(userProfileService.uploadProfilePicture(principal.getName(), file));
//    }

    /*

    @PostMapping("/{email}/follow")
    public ResponseEntity<UserDto> followUser(@PathVariable("email") String emailAddress) {
        return ResponseEntity.ok(userProfileService.followUser(emailAddress));
    }

    @PostMapping("/{email}/unfollow")
    public ResponseEntity<UserDto> unfollowUser(@PathVariable("email") String emailAddress) {
        return ResponseEntity.ok(userProfileService.unfollowUser(emailAddress));
    }

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserDto> uploadProfilePicture(
            @AuthenticationPrincipal Principal principal,
            @RequestParam("file") MultipartFile file
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        UserDto updatedUser = userProfileService.uploadProfilePicture(principal.getName(), file);
        // for test user ->;
        // UserDto updatedUser = userProfileService.uploadProfilePicture("datasaz.contact@gmail.com", file);
        return ResponseEntity.ok(updatedUser);
    }

     */