package com.datasaz.ecommerce.mappers;

import com.datasaz.ecommerce.models.dto.AuditLogDto;
import com.datasaz.ecommerce.repositories.entities.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogDto toDto(AuditLog auditLog) {
        return AuditLogDto.builder()
                .id(auditLog.getId())
                .userEmail(auditLog.getUserEmail())
                .roleName(auditLog.getRoleName())
                .action(auditLog.getAction())
                .details(auditLog.getDetails())
                .performedBy(auditLog.getPerformedBy())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
