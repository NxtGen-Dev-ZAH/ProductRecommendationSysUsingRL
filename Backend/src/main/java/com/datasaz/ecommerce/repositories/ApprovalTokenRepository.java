package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.ApprovalToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ApprovalTokenRepository extends JpaRepository<ApprovalToken, Long> {
    Optional<ApprovalToken> findByToken(String token);
    //void deleteByUserEmail(String userEmail);

    @Modifying
    @Query("DELETE FROM ApprovalToken rt WHERE rt.revoked = true OR rt.expiryDate < :currentTime")
    void deleteExpiredOrRevokedTokens(LocalDateTime currentTime);

    Optional<ApprovalToken> findByTokenAndRevokedFalse(String token);
}
