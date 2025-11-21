package com.datasaz.ecommerce.models.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Lightweight response object for user summary in followers/following lists")
public class UserSummaryResponse {
    @Schema(description = "Email address of the user", example = "user@example.com")
    private String emailAddress;

    @Schema(description = "Display name of the user", example = "John Doe")
    private String displayName;

    @Schema(description = "URL of the user's profile picture", example = "http://example.com/profile.jpg")
    private String profilePictureUrl;

    @Schema(description = "Base64-encoded content of the user's profile image", example = "data:image/jpeg;base64,/9j/4AAQSkZJRg==")
    private String imageContent;

    @Schema(description = "MIME type of the user's profile image", example = "image/jpeg")
    private String imageContentType;
}