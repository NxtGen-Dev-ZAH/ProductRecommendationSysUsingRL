package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
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
 * Controller for managing user addresses (Admin/Management access).
 * Allows administrators to manage addresses for any user.
 */
@Slf4j
@RestController
@RequestMapping("/admin/v1/users/{userId}/addresses")
@RequiredArgsConstructor
@Tag(name = "Admin User Address Management", description = "APIs for managing user addresses (Admin access)")
@PreAuthorize("hasRole('APP_ADMIN')")
public class AdminUserAddressController {

    private final IUserAddressService userAddressService;

    @Operation(summary = "Add a new address for a user", description = "Creates a new address for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> addAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @Valid @RequestBody AddressRequest request) {
        log.info("Admin adding address for user: {}", userId);
        AddressResponse response = userAddressService.addAddress(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an address", description = "Updates an existing address for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "User or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PutMapping(value = "/{addressId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address ID") Long addressId,
            @Valid @RequestBody AddressRequest request) {
        log.info("Admin updating address {} for user: {}", addressId, userId);
        AddressResponse response = userAddressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an address", description = "Deletes an address for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin deleting address {} for user: {}", addressId, userId);
        userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a specific address", description = "Retrieves a specific address by ID for the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/{addressId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin retrieving address {} for user: {}", addressId, userId);
        AddressResponse response = userAddressService.getAddress(userId, addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all addresses for a user", description = "Retrieves all addresses for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAllAddresses(
            @PathVariable @Parameter(description = "User ID") Long userId) {
        log.info("Admin retrieving all addresses for user: {}", userId);
        List<AddressResponse> addresses = userAddressService.getAllAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get paginated addresses", description = "Retrieves paginated addresses for the specified user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/paginated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AddressResponse>> getAddressesPaginated(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Parameter(description = "Page size") int size) {
        log.info("Admin retrieving paginated addresses for user: {}, page: {}, size: {}", userId, page, size);
        Page<AddressResponse> addresses = userAddressService.getAddressesPaginated(userId, page, size);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get addresses by type", description = "Retrieves addresses of a specific type for the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/by-type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAddressesByType(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        log.info("Admin retrieving {} addresses for user: {}", type, userId);
        List<AddressResponse> addresses = userAddressService.getAddressesByType(userId, type);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get default address by type", description = "Retrieves the default address of a specific type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found or no default address"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/default/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        log.info("Admin retrieving default {} address for user: {}", type, userId);
        AddressResponse response = userAddressService.getDefaultAddress(userId, type);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set address as default", description = "Sets a specific address as default for its type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address set as default successfully"),
            @ApiResponse(responseCode = "404", description = "User or address not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @PatchMapping(value = "/{addressId}/set-default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable @Parameter(description = "User ID") Long userId,
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        log.info("Admin setting address {} as default for user: {}", addressId, userId);
        AddressResponse response = userAddressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get shipping addresses", description = "Retrieves all shipping addresses for the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipping addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/shipping", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getShippingAddresses(
            @PathVariable @Parameter(description = "User ID") Long userId) {
        log.info("Admin retrieving shipping addresses for user: {}", userId);
        List<AddressResponse> addresses = userAddressService.getShippingAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get billing addresses", description = "Retrieves all billing addresses for the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Billing addresses retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    @GetMapping(value = "/billing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getBillingAddresses(
            @PathVariable @Parameter(description = "User ID") Long userId) {
        log.info("Admin retrieving billing addresses for user: {}", userId);
        List<AddressResponse> addresses = userAddressService.getBillingAddresses(userId);
        return ResponseEntity.ok(addresses);
    }
}