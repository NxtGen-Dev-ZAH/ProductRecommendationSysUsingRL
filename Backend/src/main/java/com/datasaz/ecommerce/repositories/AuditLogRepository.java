package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:userEmail IS NULL OR a.userEmail = :userEmail) AND " +
            "(:roleName IS NULL OR a.roleName = :roleName) AND " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:details IS NULL OR a.details = :details) AND " +
            "(:performedBy IS NULL OR a.performedBy = :performedBy) AND " +
            "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
            "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> findByFilters(
            @Param("userEmail") String userEmail,
            @Param("roleName") String roleName,
            @Param("action") String action,
            @Param("details") String details,
            @Param("performedBy") String performedBy,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}
