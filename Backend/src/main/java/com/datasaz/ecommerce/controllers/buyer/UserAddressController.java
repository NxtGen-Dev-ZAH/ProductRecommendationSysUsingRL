package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import com.datasaz.ecommerce.repositories.entities.User;
import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
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
 * Controller for managing addresses of the currently authenticated user.
 * Allows users to manage their own addresses.
 */
@Slf4j
@RestController
@RequestMapping("/buyer/user/v1/addresses")
@RequiredArgsConstructor
@Tag(name = "User Address Management", description = "APIs for managing current user's addresses")
//@PreAuthorize("isAuthenticated()")
@PreAuthorize("hasRole('BUYER')")
public class UserAddressController {

    private final IUserAddressService userAddressService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Add a new address", description = "Creates a new address for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address created successfully",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} adding new address", currentUser.getEmailAddress());
        AddressResponse response = userAddressService.addAddress(currentUser.getId(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Update an address", description = "Updates an existing address for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @PutMapping(value = "/{addressId}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable @Parameter(description = "Address ID") Long addressId,
            @Valid @RequestBody AddressRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} updating address {}", currentUser.getEmailAddress(), addressId);
        AddressResponse response = userAddressService.updateAddress(currentUser.getId(), addressId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an address", description = "Deletes an address for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} deleting address {}", currentUser.getEmailAddress(), addressId);
        userAddressService.deleteAddress(currentUser.getId(), addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get a specific address", description = "Retrieves a specific address by ID for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/{addressId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving address {}", currentUser.getEmailAddress(), addressId);
        AddressResponse response = userAddressService.getAddress(currentUser.getId(), addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all my addresses", description = "Retrieves all addresses for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving all addresses", currentUser.getEmailAddress());
        List<AddressResponse> addresses = userAddressService.getAllAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get paginated addresses", description = "Retrieves paginated addresses for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/paginated", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<AddressResponse>> getAddressesPaginated(
            @RequestParam(defaultValue = "0") @Min(0) @Parameter(description = "Page number (0-based)") int page,
            @RequestParam(defaultValue = "10") @Min(1) @Parameter(description = "Page size") int size) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving paginated addresses, page: {}, size: {}",
                currentUser.getEmailAddress(), page, size);
        Page<AddressResponse> addresses = userAddressService.getAddressesPaginated(currentUser.getId(), page, size);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get addresses by type", description = "Retrieves addresses of a specific type for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/by-type/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getAddressesByType(
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving {} addresses", currentUser.getEmailAddress(), type);
        List<AddressResponse> addresses = userAddressService.getAddressesByType(currentUser.getId(), type);
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get default address by type", description = "Retrieves the default address of a specific type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No default address found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/default/{type}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getDefaultAddress(
            @PathVariable @Parameter(description = "Address type") AddressType type) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving default {} address", currentUser.getEmailAddress(), type);
        AddressResponse response = userAddressService.getDefaultAddress(currentUser.getId(), type);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Set address as default", description = "Sets a specific address as default for its type")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Address set as default successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @PatchMapping(value = "/{addressId}/set-default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> setDefaultAddress(
            @PathVariable @Parameter(description = "Address ID") Long addressId) {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} setting address {} as default", currentUser.getEmailAddress(), addressId);
        AddressResponse response = userAddressService.setDefaultAddress(currentUser.getId(), addressId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get my shipping addresses", description = "Retrieves all shipping addresses for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Shipping addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/shipping", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getShippingAddresses() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving shipping addresses", currentUser.getEmailAddress());
        List<AddressResponse> addresses = userAddressService.getShippingAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get my billing addresses", description = "Retrieves all billing addresses for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Billing addresses retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/billing", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AddressResponse>> getBillingAddresses() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving billing addresses", currentUser.getEmailAddress());
        List<AddressResponse> addresses = userAddressService.getBillingAddresses(currentUser.getId());
        return ResponseEntity.ok(addresses);
    }

    @Operation(summary = "Get default shipping address", description = "Retrieves the default shipping address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default shipping address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No default shipping address found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/shipping/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getDefaultShippingAddress() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving default shipping address", currentUser.getEmailAddress());
        AddressResponse response = userAddressService.getDefaultShippingAddress(currentUser.getId());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get default billing address", description = "Retrieves the default billing address")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Default billing address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No default billing address found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/billing/default", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AddressResponse> getDefaultBillingAddress() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} retrieving default billing address", currentUser.getEmailAddress());
        AddressResponse response = userAddressService.getDefaultBillingAddress(currentUser.getId());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Check if I have addresses", description = "Checks if the current user has any addresses")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check completed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/exists", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> hasAddresses() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} checking if they have addresses", currentUser.getEmailAddress());
        boolean hasAddresses = userAddressService.userHasAddresses(currentUser.getId());
        return ResponseEntity.ok(hasAddresses);
    }

    @Operation(summary = "Count my addresses", description = "Returns the total number of addresses for the current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User not authenticated")
    })
    @GetMapping(value = "/count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> countAddresses() {
        User currentUser = currentUserService.getCurrentUser();
        log.info("User {} counting addresses", currentUser.getEmailAddress());
        long count = userAddressService.countAddressesByUserId(currentUser.getId());
        return ResponseEntity.ok(count);
    }
}

//
//import com.datasaz.ecommerce.models.request.AddressRequest;
//import com.datasaz.ecommerce.models.response.AddressResponse;
//import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
//import com.datasaz.ecommerce.utilities.CurrentUserService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@Slf4j
//@RestController
//@RequestMapping("/buyer/user/addresses")
//@RequiredArgsConstructor
//@PreAuthorize("hasRole('BUYER')")
//public class UserAddressController {
//
//    private final IUserAddressService userAddressService;
//    private final CurrentUserService currentUserService;
//
//    @PostMapping
//    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
//        Long userId = currentUserService.getCurrentUser().getId();
//        log.info("POST /buyer/user/addresses for user ID: {}", userId);
//        AddressResponse response = userAddressService.addAddress(userId, request);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }
//
//    @PutMapping("/{addressId}")
//    public ResponseEntity<AddressResponse> updateAddress(@PathVariable Long addressId,
//                                                         @Valid @RequestBody AddressRequest request) {
//        Long userId = currentUserService.getCurrentUser().getId();
//        log.info("PUT /buyer/user/addresses/{} for user ID: {}", addressId, userId);
//        AddressResponse response = userAddressService.updateAddress(userId, addressId, request);
//        return ResponseEntity.ok(response);
//    }
//
//    @DeleteMapping("/{addressId}")
//    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId) {
//        Long userId = currentUserService.getCurrentUser().getId();
//        log.info("DELETE /buyer/user/addresses/{} for user ID: {}", addressId, userId);
//        userAddressService.deleteAddress(userId, addressId);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping("/{addressId}")
//    public ResponseEntity<AddressResponse> getAddress(@PathVariable Long addressId) {
//        Long userId = currentUserService.getCurrentUser().getId();
//        log.info("GET /buyer/user/addresses/{} for user ID: {}", addressId, userId);
//        AddressResponse response = userAddressService.getAddress(userId, addressId);
//        return ResponseEntity.ok(response);
//    }
//
//    @GetMapping
//    public ResponseEntity<List<AddressResponse>> getAllAddresses() {
//        Long userId = currentUserService.getCurrentUser().getId();
//        log.info("GET /buyer/user/addresses for user ID: {}", userId);
//        List<AddressResponse> responses = userAddressService.getAllAddresses(userId);
//        return ResponseEntity.ok(responses);
//    }
//}
