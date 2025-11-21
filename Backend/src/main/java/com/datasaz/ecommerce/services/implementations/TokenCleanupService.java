package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.repositories.ApprovalTokenRepository;
import com.datasaz.ecommerce.repositories.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final ApprovalTokenRepository approvalTokenRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void cleanupExpiredOrRevokedRefreshTokens() {
        log.info("Starting cleanup of expired or revoked refresh tokens");
        try {
            refreshTokenRepository.deleteExpiredOrRevokedTokens(LocalDateTime.now());
            log.info("Successfully cleaned up expired or revoked refresh tokens");
        } catch (Exception e) {
            log.error("Error during refresh token cleanup: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void cleanupExpiredOrRevokedApprovalTokens() {
        log.info("Starting cleanup of expired or revoked Approval tokens");
        try {
            approvalTokenRepository.deleteExpiredOrRevokedTokens(LocalDateTime.now());
            log.info("Successfully cleaned up expired or revoked approval tokens");
        } catch (Exception e) {
            log.error("Error during approval token cleanup: {}", e.getMessage());
        }
    }
}
