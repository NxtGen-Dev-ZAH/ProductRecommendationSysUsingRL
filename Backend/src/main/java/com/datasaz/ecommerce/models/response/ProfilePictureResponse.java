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
@Schema(description = "Response object for profile picture upload")
public class ProfilePictureResponse {
    @Schema(description = "URL of the uploaded profile picture", example = "/uploads/profile-pictures/123.jpg")
    private String profilePictureUrl;

    @Schema(description = "Message indicating the result of the upload", example = "Profile picture uploaded successfully")
    private String message;
}
