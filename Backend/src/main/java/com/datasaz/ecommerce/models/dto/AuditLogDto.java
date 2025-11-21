package com.datasaz.ecommerce.models.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogDto {
    private Long id;
    private String userEmail;
    private String roleName;
    private String action;
    private String details;
    private String performedBy;
    private LocalDateTime timestamp;
}
