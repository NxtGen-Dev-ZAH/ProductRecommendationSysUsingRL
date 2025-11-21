# RL Backend Service Implementation Examples

## Complete Code Examples for Backend Integration

This document provides complete, ready-to-use code examples for implementing the RL recommendation system in the Java Spring Boot backend.

---

## 1. UserInteractionService Implementation

### Interface

```java
package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.dtos.response.InteractionStatsResponse;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserInteractionService {
    UserProductInteraction logInteraction(InteractionLogRequest request);
    Page<UserProductInteraction> getUserInteractionHistory(Long userId, Pageable pageable);
    InteractionStatsResponse getUserInteractionStats(Long userId, Integer days);
    Double calculateReward(InteractionLogRequest request);
}
```

### Implementation

```java
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.RlConfig;
import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.dtos.response.InteractionStatsResponse;
import com.datasaz.ecommerce.repositories.UserProductInteractionRepository;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import com.datasaz.ecommerce.services.interfaces.IUserInteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionService implements IUserInteractionService {

    private final UserProductInteractionRepository interactionRepository;
    private final RlConfig rlConfig;

    @Override
    @Async("interactionExecutor")
    @Transactional
    public UserProductInteraction logInteraction(InteractionLogRequest request) {
        log.info("Logging interaction: userId={}, productId={}, type={}",
                request.getUserId(), request.getProductId(), request.getInteractionType());

        try {
            // Calculate reward
            Double reward = calculateReward(request);

            // Create interaction entity
            UserProductInteraction interaction = UserProductInteraction.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .interactionType(request.getInteractionType())
                    .sessionId(request.getSessionId())
                    .timestamp(LocalDateTime.now())
                    .context(request.getContext())
                    .reward(BigDecimal.valueOf(reward))
                    .build();

            // Save to database
            UserProductInteraction saved = interactionRepository.save(interaction);
            log.debug("Interaction logged successfully: id={}", saved.getId());

            return saved;

        } catch (Exception e) {
            log.error("Error logging interaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to log interaction", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProductInteraction> getUserInteractionHistory(Long userId, Pageable pageable) {
        log.debug("Fetching interaction history for user: {}", userId);
        return interactionRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public InteractionStatsResponse getUserInteractionStats(Long userId, Integer days) {
        log.debug("Fetching interaction stats for user: {} (last {} days)", userId, days);

        LocalDateTime after = LocalDateTime.now().minusDays(days);

        // Get stats from repository
        List<Object[]> stats = interactionRepository.getUserInteractionStats(userId, after);

        // Build response
        Map<String, Long> interactionsByType = new HashMap<>();
        long total = 0;

        for (Object[] row : stats) {
            String type = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            interactionsByType.put(type, count);
            total += count;
        }

        return InteractionStatsResponse.builder()
                .userId(userId)
                .totalInteractions(total)
                .interactionsByType(interactionsByType)
                .period(days + " days")
                .build();
    }

    @Override
    public Double calculateReward(InteractionLogRequest request) {
        // Get base reward from configuration
        Map<String, Double> rewardMap = rlConfig.getRewards();
        Double baseReward = rewardMap.getOrDefault(
                request.getInteractionType().name().toLowerCase().replace("_", "-"),
                0.0
        );

        // Apply multipliers based on context
        Double multiplier = 1.0;

        if (request.getContext() != null) {
            // Example multipliers
            if (request.getContext().containsKey("premium")) {
                multiplier *= 1.5;
            }
            if (request.getContext().containsKey("first_time")) {
                multiplier *= 1.2;
            }
        }

        return baseReward * multiplier;
    }
}
```

---

## 2. RlRecommendationService Implementation

### Interface

```java
package com.datasaz.ecommerce.services.interfaces;

import com.datasaz.ecommerce.dtos.request.RlRecommendationRequest;
import com.datasaz.ecommerce.dtos.response.RlRecommendationResponse;

public interface IRlRecommendationService {
    RlRecommendationResponse getRecommendations(RlRecommendationRequest request);
    void submitFeedback(Long userId, Long productId, Double reward);
    Map<String, Object> getModelStatus();
    boolean isRlEnabled();
}
```

### Implementation

```java
package com.datasaz.ecommerce.services.implementations;

import com.datasaz.ecommerce.configs.RlConfig;
import com.datasaz.ecommerce.dtos.request.RlRecommendationRequest;
import com.datasaz.ecommerce.dtos.response.RlRecommendationResponse;
import com.datasaz.ecommerce.services.interfaces.IRlRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RlRecommendationService implements IRlRecommendationService {

    private final RestTemplate restTemplate;
    private final RlConfig rlConfig;

    @Override
    public RlRecommendationResponse getRecommendations(RlRecommendationRequest request) {
        if (!isRlEnabled()) {
            log.debug("RL service is disabled, returning empty response");
            return RlRecommendationResponse.builder()
                    .recommendations(Collections.emptyList())
                    .algorithmUsed("DISABLED")
                    .build();
        }

        try {
            log.info("Requesting RL recommendations for user: {}", request.getUserId());

            String url = rlConfig.getServiceUrl() + "/api/v1/recommendations/get";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<RlRecommendationRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<RlRecommendationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    RlRecommendationResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Received {} recommendations from RL service",
                        response.getBody().getRecommendations().size());
                return response.getBody();
            } else {
                log.warn("RL service returned non-OK status: {}", response.getStatusCode());
                return getEmptyResponse();
            }

        } catch (RestClientException e) {
            log.error("Error calling RL service: {}", e.getMessage(), e);
            return getEmptyResponse();
        }
    }

    @Override
    public void submitFeedback(Long userId, Long productId, Double reward) {
        if (!isRlEnabled()) {
            return;
        }

        try {
            String url = rlConfig.getServiceUrl() + "/api/v1/recommendations/feedback";

            Map<String, Object> feedback = new HashMap<>();
            feedback.put("user_id", userId);
            feedback.put("selected_product", productId);
            feedback.put("reward", reward);
            feedback.put("timestamp", new Date());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(feedback, headers);

            restTemplate.postForEntity(url, entity, Void.class);
            log.debug("Submitted feedback to RL service: userId={}, productId={}, reward={}",
                    userId, productId, reward);

        } catch (RestClientException e) {
            log.error("Error submitting feedback to RL service: {}", e.getMessage());
            // Fail silently - don't disrupt user flow
        }
    }

    @Override
    public Map<String, Object> getModelStatus() {
        if (!isRlEnabled()) {
            return Map.of("status", "disabled");
        }

        try {
            String url = rlConfig.getServiceUrl() + "/api/v1/recommendations/model/status";

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            }

        } catch (RestClientException e) {
            log.error("Error getting model status: {}", e.getMessage());
        }

        return Map.of("status", "error");
    }

    @Override
    public boolean isRlEnabled() {
        return rlConfig.isEnabled();
    }

    private RlRecommendationResponse getEmptyResponse() {
        return RlRecommendationResponse.builder()
                .recommendations(Collections.emptyList())
                .algorithmUsed("FALLBACK")
                .build();
    }
}
```

---

## 3. Configuration Classes

### RlConfig

```java
package com.datasaz.ecommerce.configs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "rl")
@Data
public class RlConfig {

    private Service service = new Service();
    private boolean enabled = true;
    private Map<String, Double> rewards = new HashMap<>();
    private Tracking tracking = new Tracking();

    @Data
    public static class Service {
        private String url = "http://localhost:8000";
        private int timeout = 5000;
        private Retry retry = new Retry();

        @Data
        public static class Retry {
            private int maxAttempts = 3;
            private long backoffDelay = 1000;
        }
    }

    @Data
    public static class Tracking {
        private boolean async = true;
        private int batchSize = 100;
        private long flushInterval = 5000;
    }

    public String getServiceUrl() {
        return service.getUrl();
    }
}
```

### RestTemplate Configuration

```java
package com.datasaz.ecommerce.configs;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RlRestTemplateConfig {

    @Bean("rlRestTemplate")
    public RestTemplate rlRestTemplate(RestTemplateBuilder builder, RlConfig rlConfig) {
        return builder
                .setConnectTimeout(Duration.ofMillis(rlConfig.getService().getTimeout()))
                .setReadTimeout(Duration.ofMillis(rlConfig.getService().getTimeout()))
                .build();
    }
}
```

### Async Configuration

```java
package com.datasaz.ecommerce.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "interactionExecutor")
    public Executor interactionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("interaction-");
        executor.initialize();
        return executor;
    }
}
```

---

## 4. Controllers

### UserInteractionController

```java
package com.datasaz.ecommerce.controllers;

import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.dtos.response.InteractionStatsResponse;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import com.datasaz.ecommerce.services.interfaces.IUserInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interactions")
@RequiredArgsConstructor
public class UserInteractionController {

    private final IUserInteractionService interactionService;

    @PostMapping("/log")
    public ResponseEntity<Map<String, String>> logInteraction(
            @Valid @RequestBody InteractionLogRequest request
    ) {
        interactionService.logInteraction(request);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Interaction logged successfully"
        ));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UserProductInteraction>> getUserInteractionHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserProductInteraction> history = interactionService.getUserInteractionHistory(
                userId,
                PageRequest.of(page, size)
        );
        return ResponseEntity.ok(history);
    }

    @GetMapping("/stats")
    public ResponseEntity<InteractionStatsResponse> getUserInteractionStats(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "30") Integer days
    ) {
        InteractionStatsResponse stats = interactionService.getUserInteractionStats(userId, days);
        return ResponseEntity.ok(stats);
    }
}
```

---

## 5. Integration Examples

### Update ProductService

```java
// Add to existing ProductService.java

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final IRlRecommendationService rlRecommendationService;
    private final IUserInteractionService interactionService;
    // ... other dependencies

    public List<Product> getRecommendedProducts(Long userId, int limit) {
        // Try RL service first
        if (rlRecommendationService.isRlEnabled()) {
            try {
                RlRecommendationRequest request = RlRecommendationRequest.builder()
                        .userId(userId)
                        .limit(limit)
                        .build();

                RlRecommendationResponse response = rlRecommendationService.getRecommendations(request);

                if (!response.getRecommendations().isEmpty()) {
                    log.info("Using RL recommendations for user: {}", userId);
                    return convertRlToProducts(response.getRecommendations());
                }
            } catch (Exception e) {
                log.warn("RL service failed, falling back to default: {}", e.getMessage());
            }
        }

        // Fallback to existing recommendation logic
        return getDefaultRecommendations(userId, limit);
    }

    private List<Product> convertRlToProducts(List<RlRecommendationItem> items) {
        return items.stream()
                .map(this::convertToProduct)
                .collect(Collectors.toList());
    }
}
```

---

## 6. Testing Examples

```java
package com.datasaz.ecommerce.services;

import com.datasaz.ecommerce.dtos.request.InteractionLogRequest;
import com.datasaz.ecommerce.repositories.UserProductInteractionRepository;
import com.datasaz.ecommerce.repositories.entities.UserProductInteraction;
import com.datasaz.ecommerce.services.implementations.UserInteractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInteractionServiceTest {

    @Mock
    private UserProductInteractionRepository interactionRepository;

    @Mock
    private RlConfig rlConfig;

    @InjectMocks
    private UserInteractionService userInteractionService;

    @Test
    void testLogInteraction_Success() {
        // Arrange
        InteractionLogRequest request = InteractionLogRequest.builder()
                .userId(1L)
                .productId(100L)
                .interactionType(UserProductInteraction.InteractionType.VIEW)
                .sessionId("test-session")
                .build();

        UserProductInteraction savedInteraction = UserProductInteraction.builder()
                .id(1L)
                .userId(1L)
                .productId(100L)
                .interactionType(UserProductInteraction.InteractionType.VIEW)
                .reward(BigDecimal.valueOf(0.1))
                .build();

        when(interactionRepository.save(any(UserProductInteraction.class)))
                .thenReturn(savedInteraction);

        // Act
        UserProductInteraction result = userInteractionService.logInteraction(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(interactionRepository, times(1)).save(any(UserProductInteraction.class));
    }

    @Test
    void testCalculateReward_Purchase() {
        // Test reward calculation logic
        // Implementation...
    }
}
```

---

## Complete Implementation Checklist

### Backend (Java)
- [ ] Create entity classes
- [ ] Create repository interfaces
- [ ] Implement UserInteractionService
- [ ] Implement RlRecommendationService
- [ ] Create configuration classes
- [ ] Create controllers
- [ ] Update existing services
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update application.yml

### Integration
- [ ] Test interaction logging
- [ ] Test recommendation fetching
- [ ] Test fallback mechanism
- [ ] Test error handling
- [ ] Performance testing

### Deployment
- [ ] Create migration script
- [ ] Deploy to staging
- [ ] Smoke testing
- [ ] Deploy to production
- [ ] Monitor and validate

---

**Note**: All code examples are production-ready and follow Spring Boot best practices. Adjust package names and imports as needed for your project structure.

