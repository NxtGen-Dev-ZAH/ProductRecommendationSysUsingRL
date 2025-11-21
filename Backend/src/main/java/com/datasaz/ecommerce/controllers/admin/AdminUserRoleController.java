package com.datasaz.ecommerce.controllers.admin;

import com.datasaz.ecommerce.exceptions.IllegalParameterException;
import com.datasaz.ecommerce.exceptions.InvalidRoleException;
import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.models.request.ManageRoleRequest;
import com.datasaz.ecommerce.repositories.entities.Roles;
import com.datasaz.ecommerce.services.interfaces.IAdminUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/user/role")
public class AdminUserRoleController {

    private final IAdminUserRoleService adminUserRoleService;

    //@PreAuthorize("hasAnyRole('APP_ADMIN')")
    @PostMapping("/{userId}/add")
    public ResponseEntity<UserDto> addUserRole(
            @PathVariable Long userId,
            @RequestParam("role") String role
    ) {
        UserDto updatedUser = adminUserRoleService.addUserRole(userId, role);
        return ResponseEntity.ok(updatedUser);
    }

    //@PreAuthorize("hasAnyRole('APP_ADMIN')")
    @DeleteMapping("/{userId}/remove")
    public ResponseEntity<UserDto> removeUserRole(
            @PathVariable Long userId,
            @RequestParam("role") String role
    ) {
        UserDto updatedUser = adminUserRoleService.removeUserRole(userId, role);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
            summary = "Get roles of a user",
            description = "Returns the set of roles assigned to the user with the given ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user roles"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{userId}/roles")
    public ResponseEntity<Set<Roles>> getUserRoles(
            @PathVariable Long userId) {
        Set<Roles> roles = adminUserRoleService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

//    @PostMapping("/assign-seller-role")
//    public ResponseEntity<UserDto> assignSellerRole(@RequestBody String email) {
//        log.info("AdminUserRoleController.assignSellerRole: " + email);
//        return ResponseEntity.ok(userRoleService.assignSellerRole(email));
//    }

    @PostMapping("/assign-seller-role")
    public ResponseEntity<UserDto> assignSellerRole(@RequestBody String email) {
        log.info("AdminUserRoleController.assignSellerRole: " + email);
        UserDto userDto = adminUserRoleService.assignSellerRole(email);
        log.info("Returning UserDto: {}", userDto); // Add logging
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userDto);
    }

//    @PostMapping("/remove-seller-role")
//    public ResponseEntity<UserDto> removeSellerRole(@RequestBody String email) {
//        log.info("AdminUserRoleController.removeSellerRole: " + email);
//        return ResponseEntity.ok(userRoleService.removeSellerRole(email));
//    }

    @PostMapping("/remove-seller-role")
    public ResponseEntity<UserDto> removeSellerRole(@RequestBody String email) {
        log.info("AdminUserRoleController.removeSellerRole: {}", email);
        UserDto userDto = adminUserRoleService.removeSellerRole(email);
        log.info("Returning UserDto: {}", userDto); // Add logging for debugging
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(userDto);
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/admin/assign-role")
    public ResponseEntity<UserDto> assignRole(@Valid @RequestBody ManageRoleRequest request) {
        try {
            log.info("AdminUserRoleController.assignRole: " + request);

            return ResponseEntity.ok(adminUserRoleService.assignRole(request.getEmail(), request.getRole()));
        } catch (IllegalParameterException e) {
            throw InvalidRoleException.builder().message("Invalid role: " + request.getRole()).build();
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @PostMapping("/admin/remove-role")
    public ResponseEntity<UserDto> removeRole(@Valid @RequestBody ManageRoleRequest request) {
        try {
            log.info("AdminUserRoleController.removeRole: " + request);
            return ResponseEntity.ok(adminUserRoleService.removeRole(request.getEmail(), request.getRole()));
        } catch (IllegalParameterException e) {
            throw InvalidRoleException.builder().message("Invalid role: " + request.getRole()).build();
        }
    }

}
