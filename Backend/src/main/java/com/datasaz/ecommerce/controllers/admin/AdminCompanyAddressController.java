package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.services.interfaces.ICompanyAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing company addresses (Admin/Management access).
 * Allows administrators to manage addresses for any company.
 */
@Slf4j
@RestController
@RequestMapping("/admin/v1/company/{companyId}/addresses")
@RequiredArgsConstructor
@Tag(name = "Admin Company Address Management", description = "APIs for managing company addresses (Admin access)")
//@PreAuthorize("hasRole('APP_ADMIN') or hasRole('COMPANY_ADMIN_SELLER')")
@PreAuthorize("hasRole('APP_ADMIN')")
public class AdminCompanyAddressController {

    private final ICompanyAddressService companyAddressService;

    @Operation(summary = "Add a new address for a company", description = "Creates a new address for the specified company")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @Valid @RequestBody AddressRequest request) {
        log.info("Admin adding address for company: {}", companyId);
        AddressResponse response = companyAddressService.addAddress(companyId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an address", description = "Updates an existing address for the specified company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Company or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PutMapping(value = "/{addressId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address ID") Long addressId,
            @Valid @RequestBody AddressRequest request) {
        log.info("Admin updating address {} for company: {}", addressId, companyId);
        AddressResponse response = companyAddressService.updateAddress(companyId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an address", description = "Deletes an address for the specified company")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Company or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin deleting address {} for company: {}", addressId, companyId);
        companyAddressService.deleteAddress(companyId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a specific address", description = "Retrieves a specific address by ID for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/{addressId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin retrieving address {} for company: {}", addressId, companyId);
        AddressResponse response = companyAddressService.getAddress(companyId, addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all addresses for a company", description = "Retrieves all addresses for the specified company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAllAddresses(
            @PathVariable @Parameter(description = "Company ID") Long companyId) {
        log.info("Admin retrieving all addresses for company: {}", companyId);
        List<AddressResponse> addresses = companyAddressService.getAllAddresses(companyId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get paginated addresses", description = "Retrieves paginated addresses for the specified company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/paginated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AddressResponse>> getAddressesPaginated(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Parameter(description = "Page size") int size) {
        log.info("Admin retrieving paginated addresses for company: {}, page: {}, size: {}", companyId, page, size);
        Page<AddressResponse> addresses = companyAddressService.getAddressesPaginated(companyId, page, size);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get addresses by type", description = "Retrieves addresses of a specific type for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/by-type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAddressesByType(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        log.info("Admin retrieving {} addresses for company: {}", type, companyId);
        List<AddressResponse> addresses = companyAddressService.getAddressesByType(companyId, type);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get default address by type", description = "Retrieves the default address of a specific type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found or no default address"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/default/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        log.info("Admin retrieving default {} address for company: {}", type, companyId);
        AddressResponse response = companyAddressService.getDefaultAddress(companyId, type);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set address as default", description = "Sets a specific address as default for its type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address set as default successfully"),
            @ApiResponse(responseCode = "404", description = "Company or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PatchMapping(value = "/{addressId}/set-default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin setting address {} as default for company: {}", addressId, companyId);
        AddressResponse response = companyAddressService.setDefaultAddress(companyId, addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get expedition addresses", description = "Retrieves all expedition/shipping addresses for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Expedition addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/expedition", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getExpeditionAddresses(
            @PathVariable @Parameter(description = "Company ID") Long companyId) {
        log.info("Admin retrieving expedition addresses for company: {}", companyId);
        List<AddressResponse> addresses = companyAddressService.getExpeditionAddresses(companyId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get billing addresses", description = "Retrieves all billing addresses for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Billing addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/billing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getBillingAddresses(
            @PathVariable @Parameter(description = "Company ID") Long companyId) {
        log.info("Admin retrieving billing addresses for company: {}", companyId);
        List<AddressResponse> addresses = companyAddressService.getBillingAddresses(companyId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get contact addresses", description = "Retrieves all contact addresses for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/contact", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getContactAddresses(
            @PathVariable @Parameter(description = "Company ID") Long companyId) {
        log.info("Admin retrieving contact addresses for company: {}", companyId);
        List<AddressResponse> addresses = companyAddressService.getContactAddresses(companyId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get primary business address", description = "Retrieves the primary business address for the company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Primary address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/primary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getPrimaryBusinessAddress(
            @PathVariable @Parameter(description = "Company ID") Long companyId) {
        log.info("Admin retrieving primary business address for company: {}", companyId);
        AddressResponse response = companyAddressService.getPrimaryBusinessAddress(companyId);
        return ResponseEntity.ok(response);
    }
}
