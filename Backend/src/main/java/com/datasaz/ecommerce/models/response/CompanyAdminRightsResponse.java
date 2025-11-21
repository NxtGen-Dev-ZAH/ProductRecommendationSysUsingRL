package com.datasaz.ecommerce.models.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompanyAdminRightsResponse {
    private Long userId;
    private String email;
    private String companyName;
    private Boolean canAddRemoveSellers;
    private Boolean canPromoteDemoteAdmins;
    private Boolean canDelegateAdminRights;

    private Boolean approved;
    // private boolean canManageAdmins;
}
