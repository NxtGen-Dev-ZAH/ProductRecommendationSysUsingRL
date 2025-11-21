package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.CompanyRequest;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

public interface ISellerUserRoleService {

    //void requestCompanyAdminSeller(CompanyRequest companyRequest, MultipartFile image) throws MessagingException;

    void requestCompanyAdminSellerV2(CompanyRequest companyRequest, MultipartFile image) throws MessagingException;

    void approveOrDenyCompanyAdmin(Long rightsId, boolean approve) throws MessagingException;

    void approveOrDenyCompanyAdminByCompanyAndUser(Long companyId, Long userId, boolean approve, String token) throws MessagingException;


    void addSellerToCompany(Long companyId, String sellerEmail, String adminEmail) throws MessagingException;

    void removeSellerFromCompany(Long companyId, String sellerEmail, String adminEmail);

    void promoteToCompanyAdmin(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights) throws MessagingException;

    void demoteCompanyAdmin(Long companyId, String sellerEmail, String adminEmail) throws MessagingException;

    void updateAdminRights(Long companyId, String sellerEmail, String adminEmail, boolean canAddRemoveSellers, boolean canPromoteDemoteAdmins, boolean canDelegateAdminRights);

    void deleteCompany(Long companyId) throws MessagingException;

    void revokeCompany(Long companyId) throws MessagingException;

}
