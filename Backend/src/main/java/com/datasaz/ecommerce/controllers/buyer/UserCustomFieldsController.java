package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.models.dto.UserDto;
import com.datasaz.ecommerce.services.interfaces.IUserCustomFieldsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/buyer/user/custom-fields")
public class UserCustomFieldsController {

    private final IUserCustomFieldsService userCustomFieldsService;

    @PostMapping
    public ResponseEntity<UserDto> addCustomField(
            @AuthenticationPrincipal Principal principal,
            @Valid @RequestBody UserDto.CustomFieldDto customFieldDto
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto updatedUser = userCustomFieldsService.addCustomField(principal.getName(), customFieldDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
    }

    @PutMapping("/{fieldId}")
    public ResponseEntity<UserDto> updateCustomField(
            @AuthenticationPrincipal Principal principal,
            @PathVariable Long fieldId,
            @Valid @RequestBody UserDto.CustomFieldDto customFieldDto
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto updatedUser = userCustomFieldsService.updateCustomField(principal.getName(), fieldId, customFieldDto);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{fieldId}")
    public ResponseEntity<UserDto> deleteCustomField(
            @AuthenticationPrincipal Principal principal,
            @PathVariable Long fieldId
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto updatedUser = userCustomFieldsService.deleteCustomField(principal.getName(), fieldId);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping
    public ResponseEntity<List<UserDto.CustomFieldDto>> getAllCustomFields(
            @AuthenticationPrincipal Principal principal
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        List<UserDto.CustomFieldDto> customFields = userCustomFieldsService.getAllCustomFields(principal.getName());
        return ResponseEntity.ok(customFields);
    }

    @GetMapping("/{fieldKey}")
    public ResponseEntity<UserDto.CustomFieldDto> getCustomField(
            @AuthenticationPrincipal Principal principal,
            @PathVariable String fieldKey
    ) {
        if (principal == null) {
            throw new SecurityException("Authentication required");
        }
        UserDto.CustomFieldDto customField = userCustomFieldsService.getCustomField(principal.getName(), fieldKey);
        return ResponseEntity.ok(customField);
    }

}
