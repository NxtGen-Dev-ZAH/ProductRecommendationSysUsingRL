package com.datasaz.ecommerce.models.response;

import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressResponse {

    private Long id;

    // Contact and Recipient Details
    private String name;
    private String email;
    private String phoneNumber;

    // Address Lines
    private String addressLine1;
    private String addressLine2;

    // Location Details
    private String city;
    private String state;
    private String postalCode;
    private String country;

    // Address Metadata
    private String reference;
    private AddressType addressType;
    private boolean isDefault;

    // Optional: Include parent information if needed
    private Long userId;
    private Long companyId;

    // Version for optimistic locking
    private Long version;
}