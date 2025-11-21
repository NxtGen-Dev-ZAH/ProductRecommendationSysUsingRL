package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.mappers.AuditLogMapper;
import com.datasaz.ecommerce.models.dto.AuditLogDto;
import com.datasaz.ecommerce.repositories.AuditLogRepository;
import com.datasaz.ecommerce.repositories.entities.AuditLog;
import com.datasaz.ecommerce.services.interfaces.IAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService implements IAuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void logAction(String userEmail, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUserEmail(userEmail);
        log.setAction(action);
        log.setDetails(details);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public void logAction(String userEmail, String action, String performedBy, String details) {
        AuditLog log = new AuditLog();
        log.setUserEmail(userEmail);
        log.setAction(action);
        log.setDetails(details);
        log.setPerformedBy(performedBy);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Override
    @Transactional
    public void logAction(String userEmail, String action, String performedBy, String roleType, String details) {
        AuditLog log = new AuditLog();
        log.setUserEmail(userEmail);
        log.setAction(action);
        log.setDetails(details);
        log.setPerformedBy(performedBy);
        log.setRoleName(roleType);
        log.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(int page, int size, String userEmail, String roleName, String action,
                                          String details, String performedBy, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("getAuditLogs: Retrieving audit logs for page {}, size {}, userEmail: {}, roleName: {}, action: {}, details: {}, performedBy: {}, startDate: {}, endDate: {}",
                page, size, userEmail, roleName, action, details, performedBy, startDate, endDate);
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByFilters(userEmail, roleName, action, details, performedBy, startDate, endDate, pageable)
                .map(auditLogMapper::toDto);
    }

}

