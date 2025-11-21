package com.datasaz.ecommerce.controllers.buyer;

import com.datasaz.ecommerce.services.interfaces.IBuyerUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/buyer/user/role")
public class BuyerUserRoleController {

    private final IBuyerUserRoleService buyerUserRoleService;

    @Operation(summary = "Become an individual seller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully became an individual seller"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @PostMapping("/become-seller")
    public ResponseEntity<String> becomeIndividualSeller() {
        log.info("Request to become individual seller");
        buyerUserRoleService.becomeIndividualSeller();
        return ResponseEntity.ok("Successfully became an individual seller");
    }

}
