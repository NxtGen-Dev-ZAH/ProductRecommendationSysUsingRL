# Critical Missing Code for Integration

## 🚨 URGENT: These files MUST be created for the system to work

### File 1: RlRecommendationController.java
**Location**: `src/main/java/com/datasaz/ecommerce/controllers/RlRecommendationController.java`

**Purpose**: Bridge between frontend and Python RL service

```java
package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.dtos.request.RlRecommendationRequest;
import com.datasaz.ecommerce.dtos.response.RlRecommendationResponse;
import com.datasaz.ecommerce.services.interfaces.IRlRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for RL-powered recommendations
 * Bridges frontend requests to Python RL service
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RL Recommendations", description = "AI-powered product recommendations")
public class RlRecommendationController {

    private final IRlRecommendationService rlRecommendationService;

    /**
     * Get personalized recommendations using RL
     * 
     * This endpoint calls the Python RL service and returns
     * personalized product recommendations based on user history
     * and context.
     */
    @Operation(
        summary = "Get RL-powered recommendations",
        description = "Returns personalized product recommendations using reinforcement learning"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Recommendations retrieved"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "500", description = "RL service unavailable")
    })
    @PostMapping("/rl")
    public ResponseEntity<RlRecommendationResponse> getRlRecommendations(
            @Valid @RequestBody RlRecommendationRequest request,
            Authentication auth
    ) {
        log.info("RL recommendation request for user: {}", request.getUserId());

        try {
            // Get recommendations from RL service
            RlRecommendationResponse response = rlRecommendationService.getRecommendations(request);

            log.info("Returning {} RL recommendations for user {}",
                    response.getRecommendations().size(),
                    request.getUserId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting RL recommendations: {}", e.getMessage(), e);

            // Return empty recommendations on error (graceful degradation)
            return ResponseEntity.ok(RlRecommendationResponse.builder()
                    .userId(request.getUserId())
                    .recommendations(java.util.Collections.emptyList())
                    .totalCount(0)
                    .algorithmUsed("FALLBACK")
                    .metadata(Map.of("error", e.getMessage()))
                    .build());
        }
    }

    /**
     * Get RL model status
     */
    @Operation(
        summary = "Get model status",
        description = "Returns current RL model status and metrics"
    )
    @GetMapping("/rl/status")
    public ResponseEntity<Map<String, Object>> getModelStatus() {
        try {
            Map<String, Object> status = rlRecommendationService.getModelStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Error getting model status: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Submit feedback for online learning
     */
    @Operation(
        summary = "Submit feedback",
        description = "Submit user feedback for online model updates"
    )
    @PostMapping("/rl/feedback")
    public ResponseEntity<Map<String, String>> submitFeedback(
            @RequestParam Long userId,
            @RequestParam Long productId,
            @RequestParam Double reward
    ) {
        try {
            rlRecommendationService.submitFeedback(userId, productId, reward);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Feedback submitted"
            ));
        } catch (Exception e) {
            log.error("Error submitting feedback: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        }
    }
}
```

### File 2: UserInteractionController.java
**Location**: `src/main/java/com/datasaz/ecommerce/controllers/UserInteractionController.java`

```java
package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.dtos.response.InteractionStatsResponse;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import com.datasaz.ecommerce.services.interfaces.IUserInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for tracking user-product interactions
 * Logs all user actions for RL training
 */
@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interactions", description = "User interaction tracking for RL")
public class UserInteractionController {

    private final IUserInteractionService interactionService;

    /**
     * Log a user-product interaction
     * This is called by frontend when user interacts with products
     */
    @Operation(
        summary = "Log interaction",
        description = "Log user-product interaction (view, click, cart add, etc.)"
    )
    @PostMapping("/log")
    public ResponseEntity<Map<String, String>> logInteraction(
            @Valid @RequestBody InteractionLogRequest request
    ) {
        log.debug("Logging interaction: userId={}, productId={}, type={}",
                request.getUserId(), request.getProductId(), request.getInteractionType());

        try {
            interactionService.logInteraction(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "Interaction logged successfully"
            ));

        } catch (Exception e) {
            log.error("Error logging interaction: {}", e.getMessage(), e);

            // Return success anyway to not disrupt user experience
            return ResponseEntity.ok(Map.of(
                "status", "partial_success",
                "message", "Request received but may not be persisted"
            ));
        }
    }

    /**
     * Get user interaction history
     */
    @Operation(
        summary = "Get interaction history",
        description = "Get paginated interaction history for a user"
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UserProductInteraction>> getUserInteractionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Fetching interaction history for user: {}", userId);

        Page<UserProductInteraction> history = interactionService.getUserInteractionHistory(
                userId,
                PageRequest.of(page, size)
        );

        return ResponseEntity.ok(history);
    }

    /**
     * Get user interaction statistics
     */
    @Operation(
        summary = "Get interaction stats",
        description = "Get interaction statistics for a user"
    )
    @GetMapping("/stats")
    public ResponseEntity<InteractionStatsResponse> getUserInteractionStats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") Integer days
    ) {
        log.debug("Fetching interaction stats for user: {} (last {} days)", userId, days);

        InteractionStatsResponse stats = interactionService.getUserInteractionStats(userId, days);

        return ResponseEntity.ok(stats);
    }
}
```

### File 3: RlRecommendationRequest.java (DTO)
**Location**: `src/main/java/com/datasaz/ecommerce/dtos/request/RlRecommendationRequest.java`

```java
package com.datasaz.ecommerce.dtos.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RlRecommendationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 20, message = "Limit cannot exceed 20")
    @Builder.Default
    private Integer limit = 6;

    private Long categoryId;

    private Double priceRangeMin;

    private Double priceRangeMax;

    @Builder.Default
    private List<Long> excludeProducts = List.of();

    @Builder.Default
    private Map<String, Object> context = Map.of();

    private String algorithm; // Optional override
}
```

### File 4: RlRecommendationResponse.java (DTO)
**Location**: `src/main/java/com/datasaz/ecommerce/dtos/response/RlRecommendationResponse.java`

```java
package com.datasaz.ecommerce.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RlRecommendationResponse {

    private Long userId;

    private List<RlRecommendationItem> recommendations;

    private Integer totalCount;

    private String algorithmUsed;

    private String modelVersion;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private Map<String, Object> metadata = Map.of();
}
```

### File 5: RlRecommendationItem.java (DTO)
**Location**: `src/main/java/com/datasaz/ecommerce/dtos/response/RlRecommendationItem.java`

```java
package com.datasaz.ecommerce.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RlRecommendationItem {

    private Long productId;
    private String productName;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private Double price;
    private Double discountPrice;
    private Double discountPercentage;
    private String imageUrl;
    private Double rating;
    private Integer reviewCount;
    private Double confidenceScore;
    private Integer rank;
    private String reason;
}
```

## 🎯 Integration Priority

### Critical (Must Have)
1. ✅ Create all 5 files above
2. ✅ Implement RlRecommendationService (see RL_SERVICE_EXAMPLES.md)
3. ✅ Implement UserInteractionService (see RL_SERVICE_EXAMPLES.md)
4. ✅ Add configuration to application.yml

### Important (Should Have)
5. Create JPA entities
6. Create repositories
7. Add unit tests

### Nice to Have
8. Add caching
9. Add metrics collection
10. Add request logging

## 🚀 Quick Implementation Steps

```bash
# 1. Create DTOs (copy from above)
cd Backend/src/main/java/com/datasaz/ecommerce/dtos

# 2. Create Controllers (copy from above)
cd ../controllers

# 3. Create Services (use examples from RL_SERVICE_EXAMPLES.md)
cd ../services

# 4. Add configuration
# Edit: src/main/resources/application.yml

# 5. Rebuild
cd Backend
./mvnw clean install

# 6. Test
./mvnw spring-boot:run
```

## 🧪 Testing the Integration

### Test 1: Health Check
```bash
curl http://localhost:8080/ecommerce/api/v1/recommendations/rl/status
```

### Test 2: Log Interaction
```bash
curl -X POST http://localhost:8080/ecommerce/api/v1/interactions/log \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": 1,
    "productId": 100,
    "interactionType": "VIEW",
    "sessionId": "test-session"
  }'
```

### Test 3: Get RL Recommendations
```bash
curl -X POST http://localhost:8080/ecommerce/api/v1/recommendations/rl \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "userId": 1,
    "limit": 6
  }'
```

---

**Status**: READY TO IMPLEMENT  
**Estimated Time**: 2-3 hours  
**Priority**: CRITICAL for integration

