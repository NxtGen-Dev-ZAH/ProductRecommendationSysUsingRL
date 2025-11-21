package com.datasaz.ecommerce.repositories;


import com.datasaz.ecommerce.repositories.entities.Coupon;
import com.datasaz.ecommerce.repositories.entities.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    // Optimistic locking (default with @Version)
    Optional<Coupon> findByCode(String code);

    // Pessimistic locking
    @Query(value = "SELECT c FROM Coupon c LEFT JOIN FETCH c.couponTrackings WHERE c.code = :code")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Coupon> findByCodeWithLock(@Param("code") String code);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.couponTrackings WHERE c.code = :code")
    Optional<Coupon> findByCodeWithTrackings(@Param("code") String code);

    @Query("SELECT c FROM Coupon c WHERE c.author = :author AND c.state != 'DELETED'")
    List<Coupon> findByAuthor(@Param("author") User author);

    @Query("SELECT c FROM Coupon c LEFT JOIN FETCH c.couponTrackings WHERE c.id = :id")
    Optional<Coupon> findByIdWithTrackings(@Param("id") Long id);

    boolean existsByCode(String code);
}
