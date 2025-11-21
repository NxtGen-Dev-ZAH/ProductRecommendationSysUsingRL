package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Address;
import com.datasaz.ecommerce.repositories.entities.AddressType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    // User-specific queries
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.user.deleted = false")
    Page<Address> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.user.deleted = false")
    List<Address> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.id = :addressId AND a.user.deleted = false")
    Optional<Address> findByIdAndUserId(@Param("addressId") Long addressId, @Param("userId") Long userId);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.addressType = :type AND a.isDefault = true AND a.user.deleted = false")
    Optional<Address> findDefaultByUserIdAndType(@Param("userId") Long userId, @Param("type") AddressType type);

    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.addressType = :type AND a.user.deleted = false")
    List<Address> findByUserIdAndType(@Param("userId") Long userId, @Param("type") AddressType type);

    // Company-specific queries
    @Query("SELECT a FROM Address a WHERE a.company.id = :companyId AND a.company.deleted = false")
    Page<Address> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT a FROM Address a WHERE a.company.id = :companyId AND a.company.deleted = false")
    List<Address> findAllByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT a FROM Address a WHERE a.company.id = :companyId AND a.id = :addressId AND a.company.deleted = false")
    Optional<Address> findByIdAndCompanyId(@Param("addressId") Long addressId, @Param("companyId") Long companyId);

    @Query("SELECT a FROM Address a WHERE a.company.id = :companyId AND a.addressType = :type AND a.isDefault = true AND a.company.deleted = false")
    Optional<Address> findDefaultByCompanyIdAndType(@Param("companyId") Long companyId, @Param("type") AddressType type);

    @Query("SELECT a FROM Address a WHERE a.company.id = :companyId AND a.addressType = :type AND a.company.deleted = false")
    List<Address> findByCompanyIdAndType(@Param("companyId") Long companyId, @Param("type") AddressType type);

    // Count queries
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId AND a.user.deleted = false")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(a) FROM Address a WHERE a.company.id = :companyId AND a.company.deleted = false")
    long countByCompanyId(@Param("companyId") Long companyId);

    // Delete queries
    @Query("DELETE FROM Address a WHERE a.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Query("DELETE FROM Address a WHERE a.company.id = :companyId")
    void deleteAllByCompanyId(@Param("companyId") Long companyId);
}
