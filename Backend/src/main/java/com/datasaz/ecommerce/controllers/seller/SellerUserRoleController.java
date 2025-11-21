package com.datasaz.ecommerce.controllers.seller;

import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.services.interfaces.ISellerUserRoleService;
import com.datasaz.ecommerce.utilities.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/seller/user/role")
public class SellerUserRoleController {

    private final ISellerUserRoleService sellerUserRoleService;
    private final CurrentUserService currentUserService;

//    @PostMapping(value = "/v1/become-company-admin-seller", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<String> becomeCompanyAdminSellerV1(
//            @RequestPart("companyRequest") @Valid CompanyRequest companyRequest,
//            @RequestPart(value = "file", required = false) MultipartFile image) throws MessagingException {
//        try {
//            sellerUserRoleService.requestCompanyAdminSeller(companyRequest, image);
//            return ResponseEntity.ok("Successfully became company admin seller");
//        } catch (IllegalStateException e) {
//            return ResponseEntity.status(409).body(e.getMessage());
//        }
//    }

    //@PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Request to become a company admin seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully became company admin seller"),
            @ApiResponse(responseCode = "409", description = "Company found, admin approval required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @PostMapping(value = "/v2/become-company-admin-seller", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> becomeCompanyAdminSeller(
            @RequestPart("companyRequest") @Valid CompanyRequest companyRequest,
            @RequestPart(value = "file", required = false) MultipartFile image) throws MessagingException {
        log.info("Request to become company admin seller for company: {}", companyRequest.getName());
        try {
            sellerUserRoleService.requestCompanyAdminSellerV2(companyRequest, image);
            return ResponseEntity.ok("Successfully became company admin seller");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

//    @PostMapping("/v2/become-company-admin-seller")
//    public ResponseEntity<String> becomeCompanyAdminSellerV2(@RequestBody CompanyRequest companyRequest) throws MessagingException {
//        try {
//            userRoleService.requestCompanyAdminSellerV2(companyRequest);
//            return ResponseEntity.ok("Successfully became company admin seller");
//        } catch (IllegalStateException e) {
//            return ResponseEntity.status(409).body(e.getMessage());
//        }
//    }

    // verify the below condition in service ¿
    // @PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    @Operation(summary = "Approve a company admin seller request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company admin request approved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company, user, or admin rights not found"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/approve-company-admin/company/{companyId}/user/{userId}")
    public ResponseEntity<String> approveCompanyAdmin(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestParam String token) throws MessagingException {
        log.info("Approving company admin request for companyId: {}, userId: {}", companyId, userId);
        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, true, token);
        return ResponseEntity.ok("Company admin request approved");
    }

    // @PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    //
    @Operation(summary = "Deny a company admin seller request")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company admin request denied"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company, user, or admin rights not found"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @PostMapping("/deny-company-admin/company/{companyId}/user/{userId}")
    public ResponseEntity<String> denyCompanyAdmin(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestParam String token) throws MessagingException {
        log.info("Denying company admin request for companyId: {}, userId: {}", companyId, userId);
        sellerUserRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, false, token);
        return ResponseEntity.ok("Company admin request denied");
    }

    //TODO: delete if not needed, wrt approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, true, token);
    @PostMapping("/approve-company-admin/{rightsId}")
    public ResponseEntity<String> approveCompanyAdmin(@PathVariable Long rightsId) throws MessagingException {
        sellerUserRoleService.approveOrDenyCompanyAdmin(rightsId, true);
        return ResponseEntity.ok("Company admin request approved");
    }

    //TODO: delete if not needed, wrt approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, true, token);
    @PostMapping("/deny-company-admin/{rightsId}")
    public ResponseEntity<String> denyCompanyAdmin(@PathVariable Long rightsId) throws MessagingException {
        sellerUserRoleService.approveOrDenyCompanyAdmin(rightsId, false);
        return ResponseEntity.ok("Company admin request denied");
    }

//    @PostMapping("/approve-company-admin/company/{companyId}/user/{userId}")
//    public ResponseEntity<String> approveCompanyAdmin(@PathVariable Long companyId, @PathVariable Long userId) throws MessagingException {
//        userRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, true);
//        return ResponseEntity.ok("Company admin request approved");
//    }
//
//    @PostMapping("/deny-company-admin/company/{companyId}/user/{userId}")
//    public ResponseEntity<String> denyCompanyAdmin(@PathVariable Long companyId, @PathVariable Long userId) throws MessagingException {
//        userRoleService.approveOrDenyCompanyAdminByCompanyAndUser(companyId, userId, false);
//        return ResponseEntity.ok("Company admin request denied");
//    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    //
    @Operation(summary = "Add a seller to a company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller added to company"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Admin, seller, or company not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/company/{companyId}/add-seller/{sellerEmail}")
    public ResponseEntity<String> addSellerToCompany(@PathVariable Long companyId, @PathVariable String sellerEmail) throws MessagingException {
        String adminEmail = currentUserService.getCurrentUser().getEmailAddress();
        log.info("Adding seller {} to company {}", sellerEmail, companyId);
        sellerUserRoleService.addSellerToCompany(companyId, sellerEmail, adminEmail);
        return ResponseEntity.ok("Seller added to company");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    @Operation(summary = "Remove a seller from a company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller removed from company"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Admin, seller, or company not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/company/{companyId}/remove-seller/{sellerEmail}")
    public ResponseEntity<String> removeSellerFromCompany(@PathVariable Long companyId, @PathVariable String sellerEmail) {
        String adminEmail = currentUserService.getCurrentUser().getEmailAddress();
        log.info("Removing seller {} from company {}", sellerEmail, companyId);
        sellerUserRoleService.removeSellerFromCompany(companyId, sellerEmail, adminEmail);
        return ResponseEntity.ok("Seller removed from company");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    @Operation(summary = "Promote a seller to company admin")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Seller promoted to company admin"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Admin, seller, or company not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/company/{companyId}/promote-admin/{sellerEmail}")
    public ResponseEntity<String> promoteToCompanyAdmin(
            @PathVariable Long companyId,
            @PathVariable String sellerEmail,
            @RequestParam boolean canAddRemoveSellers,
            @RequestParam boolean canPromoteDemoteAdmins,
            @RequestParam boolean canDelegateAdminRights) throws MessagingException {
        String adminEmail = currentUserService.getCurrentUser().getEmailAddress();
        log.info("Promoting seller {} to company admin for company {}", sellerEmail, companyId);
        sellerUserRoleService.promoteToCompanyAdmin(companyId, sellerEmail, adminEmail, canAddRemoveSellers, canPromoteDemoteAdmins, canDelegateAdminRights);
        return ResponseEntity.ok("Seller promoted to company admin");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    @Operation(summary = "Demote a company admin")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company admin demoted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Admin, seller, or company not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/company/{companyId}/demote-admin/{sellerEmail}")
    public ResponseEntity<String> demoteCompanyAdmin(@PathVariable Long companyId, @PathVariable String sellerEmail) throws MessagingException {
        String adminEmail = currentUserService.getCurrentUser().getEmailAddress();
        log.info("Demoting company admin {} for company {}", sellerEmail, companyId);
        sellerUserRoleService.demoteCompanyAdmin(companyId, sellerEmail, adminEmail);
        return ResponseEntity.ok("Company admin demoted");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    @Operation(summary = "Update company admin rights")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin rights updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Admin, seller, or company not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @PostMapping("/company/{companyId}/update-admin-rights/{sellerEmail}")
    public ResponseEntity<String> updateAdminRights(
            @PathVariable Long companyId,
            @PathVariable String sellerEmail,
            @RequestParam boolean canAddRemoveSellers,
            @RequestParam boolean canPromoteDemoteAdmins,
            @RequestParam boolean canDelegateAdminRights) {
        String adminEmail = currentUserService.getCurrentUser().getEmailAddress();
        log.info("Updating admin rights for {} in company {}", sellerEmail, companyId);
        sellerUserRoleService.updateAdminRights(companyId, sellerEmail, adminEmail, canAddRemoveSellers, canPromoteDemoteAdmins, canDelegateAdminRights);
        return ResponseEntity.ok("Admin rights updated");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    //
    @Operation(summary = "Delete a company (soft deletion)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "400", description = "Company already deleted")
    })
    @PostMapping("/company/{companyId}/delete")
    public ResponseEntity<String> deleteCompany(@PathVariable Long companyId) throws MessagingException {
        log.info("Request to delete company {}", companyId);
        sellerUserRoleService.deleteCompany(companyId);
        return ResponseEntity.ok("Company deleted successfully");
    }

    //@PreAuthorize("hasRole('COMPANY_ADMIN_SELLER')")
    //
    @Operation(summary = "Revoke (restore) a deleted company")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Company revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Company not found"),
            @ApiResponse(responseCode = "400", description = "Company not deleted or associated users deleted")
    })
    @PostMapping("/company/{companyId}/revoke")
    public ResponseEntity<String> revokeCompany(@PathVariable Long companyId) throws MessagingException {
        log.info("Request to revoke company {}", companyId);
        sellerUserRoleService.revokeCompany(companyId);
        return ResponseEntity.ok("Company revoked successfully");
    }


}
