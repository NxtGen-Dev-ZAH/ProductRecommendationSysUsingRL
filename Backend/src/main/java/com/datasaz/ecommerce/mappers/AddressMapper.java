package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.Address;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AddressMapper {

    public Address toEntity(AddressRequest request) {
        if (request == null) {
            log.warn("AddressRequest is null");
            return null;
        }

        log.debug("Converting AddressRequest to Address entity");
        return Address.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .reference(request.getReference())
                .addressType(request.getAddressType())
                .isDefault(request.isDefault())
                .build();
    }

    public AddressResponse toResponse(Address address) {
        if (address == null) {
            log.warn("Address entity is null");
            return null;
        }

        log.debug("Converting Address entity to AddressResponse");
        return AddressResponse.builder()
                .id(address.getId())
                .name(address.getName())
                .email(address.getEmail())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .reference(address.getReference())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .userId(address.getUser() != null ? address.getUser().getId() : null)
                .companyId(address.getCompany() != null ? address.getCompany().getId() : null)
                .version(address.getVersion())
                .build();
    }

    public void updateEntityFromRequest(AddressRequest request, Address address) {
        if (request == null || address == null) {
            log.warn("AddressRequest or Address entity is null");
            return;
        }

        log.debug("Updating Address entity from AddressRequest");

        if (request.getName() != null) {
            address.setName(request.getName());
        }

        address.setEmail(request.getEmail());
        address.setPhoneNumber(request.getPhoneNumber());

        if (request.getAddressLine1() != null) {
            address.setAddressLine1(request.getAddressLine1());
        }

        address.setAddressLine2(request.getAddressLine2());

        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        // Allow setting state to null
        address.setState(request.getState());

        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        // Allow setting reference to null
        address.setReference(request.getReference());

        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }

        // Always update isDefault
        address.setDefault(request.isDefault());
    }
}