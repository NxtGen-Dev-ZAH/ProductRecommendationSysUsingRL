package com.datasaz.ecommerce.models.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String logoUrl;
    private String logoContent;
    private String logoContentType;

    private String registrationNumber;
    private String vatNumber;
    //private String address;

    @Email(message = "Invalid email format")
    private String contactEmail;

    private boolean deleted;
    private Long primaryAdminId;
}
