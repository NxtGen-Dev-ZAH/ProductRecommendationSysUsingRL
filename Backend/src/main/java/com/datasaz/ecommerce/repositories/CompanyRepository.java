package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByName(String companyName);

    Optional<Company> findByNameAndDeletedFalse(String companyName);

    Optional<Company> findById(Long companyId);

    Company findByIdAndDeletedFalse(Long companyId);

    // Add this method to check if company exists
    boolean existsByIdAndDeletedFalse(Long id);


//    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.addresses WHERE c.id = :companyId AND c.deleted = false")
//    Optional<Company> findByIdAndDeletedFalseWithAddresses(@Param("companyId") Long companyId);

    // If you still need the old method for backward compatibility
//    @Query("SELECT c FROM Company c LEFT JOIN FETCH c.addresses WHERE c.id = :companyId AND c.deleted = false")
//    Optional<Company> findByIdAndDeletedFalseWithAddresses(@Param("companyId") Long companyId);
}