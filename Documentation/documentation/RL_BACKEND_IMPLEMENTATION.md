# RL Recommendation System - Backend Implementation Guide

## Overview

This document provides detailed implementation guidance for integrating the RL recommendation system into the Java Spring Boot backend.

**⚠️ Note**: This is documentation for PR review. Do not commit directly to main branch.

## Implementation Checklist

- [ ] Database migration executed
- [ ] JPA entities created
- [ ] Repository interfaces implemented
- [ ] Interaction tracking service created
- [ ] RL service client implemented
- [ ] Controllers updated
- [ ] Configuration added
- [ ] Unit tests written
- [ ] Integration tests written
- [ ] Documentation updated

## 1. JPA Entities

### 1.1 UserProductInteraction Entity

**File**: `src/main/java/com/datasaz/ecommerce/repositories/entities/UserProductInteraction.java`

```java
package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_product_interactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProductInteraction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "product_id", nullable = false)
    private Long productId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 50)
    private InteractionType interactionType;
    
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "json")
    private Map<String, Object> context;
    
    @Column(name = "reward", precision = 10, scale = 2)
    private BigDecimal reward;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
    
    public enum InteractionType {
        VIEW,
        CLICK,
        CART_ADD,
        PURCHASE,
        WISHLIST,
        REMOVE_CART,
        REMOVE_WISHLIST
    }
}
```

### 1.2 RlModelMetadata Entity

**File**: `src/main/java/com/datasaz/ecommerce/repositories/entities/RlModelMetadata.java`

```java
package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "rl_model_metadata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RlModelMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false, length = 50)
    private AlgorithmType algorithmType;
    
    @Column(name = "version", nullable = false, length = 20)
    private String version;
    
    @Column(name = "is_active")
    private Boolean isActive = false;
    
    @Column(name = "training_date")
    private LocalDateTime trainingDate;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "performance_metrics", columnDefinition = "json")
    private Map<String, Object> performanceMetrics;
    
    @Column(name = "model_path", length = 500)
    private String modelPath;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    
    public enum AlgorithmType {
        LINUCB,
        THOMPSON_SAMPLING,
        DQN
    }
}
```

### 1.3 UserContextFeatures Entity

**File**: `src/main/java/com/datasaz/ecommerce/repositories/entities/UserContextFeatures.java`

```java
package com.datasaz.ecommerce.repositories.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_context_features")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContextFeatures {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_vector", nullable = false, columnDefinition = "json")
    private Map<String, Object> featureVector;
    
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }
}
```

## 2. Repository Interfaces

### 2.1 UserProductInteractionRepository

**File**: `src/main/java/com/datasaz/ecommerce/repositories/UserProductInteractionRepository.java`

```java
package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction.InteractionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserProductInteractionRepository extends JpaRepository<UserProductInteraction, Long> {
    
    // Find interactions by user
    Page<UserProductInteraction> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    // Find recent interactions for a user
    List<UserProductInteraction> findByUserIdAndTimestampAfterOrderByTimestampDesc(
            Long userId, 
            LocalDateTime after
    );
    
    // Find interactions by product
    List<UserProductInteraction> findByProductIdAndTimestampAfterOrderByTimestampDesc(
            Long productId, 
            LocalDateTime after
    );
    
    // Count interactions by type for a user
    @Query("SELECT COUNT(upi) FROM UserProductInteraction upi " +
           "WHERE upi.userId = :userId AND upi.interactionType = :type " +
           "AND upi.timestamp >= :after")
    Long countByUserIdAndTypeAndTimeAfter(
            @Param("userId") Long userId,
            @Param("type") InteractionType type,
            @Param("after") LocalDateTime after
    );
    
    // Get user interaction statistics
    @Query("SELECT upi.interactionType as type, COUNT(upi) as count " +
           "FROM UserProductInteraction upi " +
           "WHERE upi.userId = :userId AND upi.timestamp >= :after " +
           "GROUP BY upi.interactionType")
    List<Object[]> getUserInteractionStats(
            @Param("userId") Long userId,
            @Param("after") LocalDateTime after
    );
    
    // Find interactions for training (with minimum interaction threshold)
    @Query("SELECT upi FROM UserProductInteraction upi " +
           "WHERE upi.timestamp >= :after " +
           "AND upi.userId IN (" +
           "  SELECT DISTINCT upi2.userId FROM UserProductInteraction upi2 " +
           "  WHERE upi2.timestamp >= :after " +
           "  GROUP BY upi2.userId " +
           "  HAVING COUNT(upi2.id) >= :minInteractions" +
           ") " +
           "ORDER BY upi.timestamp DESC")
    List<UserProductInteraction> findInteractionsForTraining(
            @Param("after") LocalDateTime after,
            @Param("minInteractions") Long minInteractions
    );
}
```

### 2.2 Other Repositories

**File**: `src/main/java/com/datasaz/ecommerce/repositories/RlModelMetadataRepository.java`

```java
package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.RlModelMetadata;
import com.datasaz.ecommerce.repositories.entities.RlModelMetadata.AlgorithmType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RlModelMetadataRepository extends JpaRepository<RlModelMetadata, Long> {
    
    Optional<RlModelMetadata> findByAlgorithmTypeAndIsActiveTrue(AlgorithmType algorithmType);
    
    List<RlModelMetadata> findByAlgorithmTypeOrderByTrainingDateDesc(AlgorithmType algorithmType);
    
    Optional<RlModelMetadata> findByModelNameAndVersion(String modelName, String version);
}
```

**File**: `src/main/java/com/datasaz/ecommerce/repositories/UserContextFeaturesRepository.java`

```java
package com.datasaz.ecommerce.repositories;

import com.datasaz.ecommerce.repositories.entities.UserContextFeatures;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserContextFeaturesRepository extends JpaRepository<UserContextFeatures, Long> {
    
    Optional<UserContextFeatures> findByUserId(Long userId);
    
    void deleteByUserId(Long userId);
}
```

## 3. DTOs (Data Transfer Objects)

**File**: `src/main/java/com/datasaz/ecommerce/dtos/request/InteractionLogRequest.java`

```java
package com.datasaz.ecommerce.dtos.request;

import com.datasaz.ecommerce.repositories.entities.UserProductInteraction.InteractionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionLogRequest {
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @NotNull(message = "Interaction type is required")
    private InteractionType interactionType;
    
    private String sessionId;
    
    private Map<String, Object> context;
}
```

**File**: `src/main/java/com/datasaz/ecommerce/dtos/response/InteractionStatsResponse.java`

```java
package com.datasaz/ecommerce.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InteractionStatsResponse {
    private Long userId;
    private Long totalInteractions;
    private Map<String, Long> interactionsByType;
    private String period;
}
```

## 4. Service Layer

### 4.1 UserInteractionService Interface

**File**: `src/main/java/com/datasaz/ecommerce/services/interfaces/IUserInteractionService.java`

```java
package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.dtos.response.InteractionStatsResponse;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserInteractionService {
    
    /**
     * Log a user-product interaction
     */
    UserProductInteraction logInteraction(InteractionLogRequest request);
    
    /**
     * Get user interaction history
     */
    Page<UserProductInteraction> getUserInteractionHistory(Long userId, Pageable pageable);
    
    /**
     * Get user interaction statistics
     */
    InteractionStatsResponse getUserInteractionStats(Long userId, Integer days);
    
    /**
     * Calculate reward for an interaction
     */
    Double calculateReward(InteractionLogRequest request);
}
```

### 4.2 Implementation (Continued in next file)

See separate file `RL_SERVICE_IMPLEMENTATION.md` for complete service implementation code.

## 5. Configuration

**File**: `src/main/resources/application-rl-dev.yml`

```yaml
# RL Service Configuration (Local Development)
rl:
  service:
    url: http://localhost:8000
    timeout: 5000
    retry:
      max-attempts: 3
      backoff-delay: 1000
  enabled: true
  
  # Reward configuration
  rewards:
    view: 0.1
    click: 0.5
    cart-add: 2.0
    purchase: 10.0
    wishlist: 1.0
    
  # Interaction tracking
  tracking:
    async: true
    batch-size: 100
    flush-interval: 5000

# Keep existing database config
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommercedb
    username: dsazuser
    password: DataROOT_4411630
```

## 6. Testing

### 6.1 Unit Test Example

**File**: `src/test/java/com/datasaz/ecommerce/services/UserInteractionServiceTest.java`

```java
package com.datasaz.ecommerce.services;

import com.datasaz.ecommerce.repositories.UserProductInteractionRepository;
import com.datasaz.ecommerce.services.implementations.UserInteractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInteractionServiceTest {
    
    @Mock
    private UserProductInteractionRepository interactionRepository;
    
    @InjectMocks
    private UserInteractionService userInteractionService;
    
    @Test
    void testCalculateReward_Purchase() {
        // Test reward calculation for purchase
        // Implementation details...
    }
    
    // More tests...
}
```

## 7. Deployment Steps

1. **Review and merge PR**: Code review by team
2. **Run migration**: Execute database migration scripts
3. **Deploy backend**: Deploy updated backend with RL integration
4. **Start RL service**: Deploy Python RL service
5. **Monitor**: Watch logs and metrics
6. **Gradual rollout**: Use feature flag for gradual enable

## 8. Monitoring and Metrics

Monitor these key metrics:

- Interaction logging rate
- RL service response time
- Recommendation quality (CTR, conversion)
- Model training frequency
- Error rates

## Next Steps

1. Complete service implementation
2. Add controller endpoints
3. Write comprehensive tests
4. Create integration tests with RL service
5. Document API endpoints
6. Prepare deployment guide

## References

- Database Migration: `RL_DATABASE_MIGRATION.md`
- RL Service: `../Reinforce_recommend/README.md`
- Main Plan: `../rl-recommendation-integration.plan.md`

