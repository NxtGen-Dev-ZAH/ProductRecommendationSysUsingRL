package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Company;
import com.datasaz.ecommerce.repositories.entities.CompanyAdminRights;
import com.datasaz.ecommerce.repositories.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyAdminRightsRepository extends JpaRepository<CompanyAdminRights, Long> {
    Optional<CompanyAdminRights> findByCompanyIdAndUserId(Long companyId, Long userId);

    List<CompanyAdminRights> findByCompanyAndApprovedTrue(Company company);

    Optional<CompanyAdminRights> findByUserAndCompany(User user, Company company);

    @Modifying
    @Query("DELETE FROM CompanyAdminRights car WHERE car.company.id = :companyId")
    void deleteByCompanyId(@Param("companyId") Long companyId);

    //void deleteByCompanyIdAndUserId(Long companyId, Long userId);
    @Modifying
    @Query("DELETE FROM CompanyAdminRights car WHERE car.company.id = :companyId AND car.user.id = :userId")
    void deleteByCompanyIdAndUserId(@Param("companyId") Long companyId, @Param("userId") Long userId);

}
