package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.request.CompanyRequest;
import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.CompanyAdminRights;
import com.datasaz.ecommerce.repositories.entities.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ICompanyService {
    //Company registerCompany(CompanyRequest companyRequest, User user);
    Company registerCompany(CompanyRequest companyRequest, MultipartFile image, User user);

    List<CompanyAdminRights> getCompanyAdmins(Company company);

    CompanyAdminRights assignAdminRights(User user, Company company, boolean canManageAdmins, boolean approved);

    void updateAdminRights(Long rightsId, boolean approved, User approvingUser);

    String uploadLogoPicture(MultipartFile image, String companyName);
}
