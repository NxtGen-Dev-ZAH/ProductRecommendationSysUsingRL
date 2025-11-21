package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.models.request.AddressRequest;
import com.datasaz.ecommerce.models.response.AddressResponse;
import com.datasaz.ecommerce.services.interfaces.IUserAddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/user/{userId}/addresses")
@RequiredArgsConstructor
public class AdminAddressController {

    private final IUserAddressService userAddressService;

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressRequest request) {
        log.info("POST /admin/user/{}/addresses", userId);
        AddressResponse response = userAddressService.addAddress(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(@PathVariable Long userId, @PathVariable Long addressId,
                                                         @Valid @RequestBody AddressRequest request) {
        log.info("PUT /admin/user/{}/addresses/{}", userId, addressId);
        AddressResponse response = userAddressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        log.info("DELETE /admin/user/{}/addresses/{}", userId, addressId);
        userAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        log.info("GET /admin/user/{}/addresses/{}", userId, addressId);
        AddressResponse response = userAddressService.getAddress(userId, addressId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAllAddresses(@PathVariable Long userId) {
        log.info("GET /admin/user/{}/addresses", userId);
        List<AddressResponse> responses = userAddressService.getAllAddresses(userId);
        return ResponseEntity.ok(responses);
    }

}
