package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.models.dto.AuditLogDto;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;

public interface IAuditLogService {

    void logAction(String userEmail, String action, String details);
    void logAction(String userEmail, String action, String performedBy, String details);
    void logAction(String userEmail, String action, String performedBy, String roleType, String details);

    //Page<AuditLogDto> getAuditLogs(int page, int size);
    Page<AuditLogDto> getAuditLogs(int page, int size, String userEmail, String roleName, String action,
                                   String details, String performedBy, LocalDateTime startDate, LocalDateTime endDate);

}
