package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "action", nullable = false)
    private String action; // e.g., "ASSIGN_ROLE", "REMOVE_ROLE"

    @Column(name = "details")
    private String details;

    @Column(name = "performed_by")
    private String performedBy; // Email of the admin performing the action

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
