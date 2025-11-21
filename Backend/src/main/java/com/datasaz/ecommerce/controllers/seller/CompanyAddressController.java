package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.exceptions.UnauthorizedException;
import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.RoleTypes;
import com.datasaz.ecommerce.services.interfaces.ICompanyAddressService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/seller/company/addresses")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
public class CompanyAddressController {

    private final ICompanyAddressService companyAddressService;
    private final CurrentUserService currentUserService;

    private Long getAuthenticatedCompanyId() {
        var user = currentUserService.getCurrentUser();
        if (user.getCompany() == null) {
            throw UnauthorizedException.builder()
                    .message("User is not associated with any company")
                    .build();
        }
        if (user.getUserRoles().stream().noneMatch(role -> role.getRole() == RoleTypes.COMPANY_ADMIN_SELLER)) {
            throw UnauthorizedException.builder()
                    .message("User does not have COMPANY_ADMIN_SELLER role")
                    .build();
        }
        return user.getCompany().getId();
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        Long companyId = getAuthenticatedCompanyId();
        log.info("POST /seller/company/addresses for company ID: {}", companyId);
        AddressResponse response = companyAddressService.addAddress(companyId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable Long addressId,
                                                         @Valid @RequestBody AddressRequest request) {
        Long companyId = getAuthenticatedCompanyId();
        log.info("PUT /seller/company/addresses/{} for company ID: {}", addressId, companyId);
        AddressResponse response = companyAddressService.updateAddress(companyId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
        Long companyId = getAuthenticatedCompanyId();
        log.info("DELETE /seller/company/addresses/{} for company ID: {}", addressId, companyId);
        companyAddressService.deleteAddress(companyId, addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddress(@PathVariable Long addressId) {
        Long companyId = getAuthenticatedCompanyId();
        log.info("GET /seller/company/addresses/{} for company ID: {}", addressId, companyId);
        AddressResponse response = companyAddressService.getAddress(companyId, addressId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
        Long companyId = getAuthenticatedCompanyId();
        log.info("GET /seller/company/addresses for company ID: {}", companyId);
        List<AddressResponse> responses = companyAddressService.getAllAddresses(companyId);
        return ResponseEntity.ok(responses);
    }
}
