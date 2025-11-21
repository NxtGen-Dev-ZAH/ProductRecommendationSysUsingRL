package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.Cart;
import com.datasaz.ecommerce.repositories.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.user.id = :userId")
    Optional<Cart> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.user = :user")
    Optional<Cart> findByUserWithItems(@Param("user") User user);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.id = :id")
    Optional<Cart> findByIdWithItemsAndCoupon(@Param("id") Long id);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionId(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product p " +
            "LEFT JOIN FETCH p.author LEFT JOIN FETCH p.company LEFT JOIN FETCH c.coupon " +
            "WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItemsAndCoupon(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.sessionId = :sessionId")
    Optional<Cart> findBySessionIdWithItemsAndCoupons(@Param("sessionId") String sessionId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.sessionId IN :sessionIds")
    List<Cart> findBySessionIds(@Param("sessionIds") List<String> sessionIds);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items LEFT JOIN FETCH c.coupon WHERE c.user = :user")
    Optional<Cart> findByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM Cart c WHERE c.sessionId IS NOT NULL AND c.user.id IS NULL AND c.lastModified < :threshold")
    void deleteBySessionIdNotNullAndUserIdNullAndLastModifiedBefore(@Param("threshold") LocalDateTime threshold);

//    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product p " +
//            "LEFT JOIN FETCH p.author LEFT JOIN FETCH p.company LEFT JOIN FETCH c.coupon " +
//            "WHERE c.user = :user")
//    Optional<Cart> findByUserWithItems(@Param("user") User user);

//    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.sessionId = :sessionId")
//    Optional<Cart> findBySessionIdWithItems(@Param("sessionId") String sessionId);

}
