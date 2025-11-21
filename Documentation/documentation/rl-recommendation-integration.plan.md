<!-- 74989533-aee1-4253-853a-b8888044f824 c2e2ac4b-b967-4a00-bd83-6a938fadb85a -->
# Reinforcement Learning Product Recommendation System Integration Plan

## Project Overview

This plan outlines the integration of a dynamic product recommendation system using reinforcement learning (RL) into the existing e-commerce platform. The system will learn optimal recommendation strategies through user interaction feedback (clicks, purchases, cart additions) and continuously adapt to changing user preferences.

## Architecture Overview

### System Components

1. **Java Spring Boot Backend** (Existing)

   - User interaction tracking
   - API endpoints for RL service communication
   - Integration with existing recommendation endpoint

2. **Python FastAPI RL Service** (New)

   - Gymnasium-based e-commerce environment
   - RL algorithms (LinUCB/Thompson Sampling for Phase 1, DQN for Phase 2)
   - Model training and inference endpoints
   - Real-time recommendation generation

3. **Frontend** (Next.js)

   - Display RL recommendations
   - Track user interactions (clicks, views, cart additions)

4. **Database**

   - New tables for user interaction logs
   - Model metadata storage

## Phase 1: Foundation & Interaction Tracking

### 1.1 Database Schema Extensions

**New Tables:**

```sql
-- User interaction tracking
CREATE TABLE user_product_interactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL, -- CLICK, VIEW, CART_ADD, PURCHASE, WISHLIST
    session_id VARCHAR(255),
    timestamp DATETIME NOT NULL,
    context JSON, -- Additional context (category, price range, etc.)
    reward DECIMAL(10,2), -- Calculated reward for RL
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_product_timestamp (product_id, timestamp)
);

-- RL model metadata
CREATE TABLE rl_model_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    algorithm_type VARCHAR(50) NOT NULL, -- LINUCB, THOMPSON_SAMPLING, DQN
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    training_date DATETIME,
    performance_metrics JSON,
    model_path VARCHAR(500),
    created_at DATETIME NOT NULL
);

-- User context features (for contextual bandits)
CREATE TABLE user_context_features (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    feature_vector JSON NOT NULL, -- Age, preferences, purchase history, etc.
    last_updated DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

**Files to Create:**

- `Backend/src/main/java/com/datasaz/ecommerce/repositories/entities/UserProductInteraction.java`
- `Backend/src/main/java/com/datasaz/ecommerce/repositories/entities/RlModelMetadata.java`
- `Backend/src/main/java/com/datasaz/ecommerce/repositories/entities/UserContextFeatures.java`
- `Backend/src/main/java/com/datasaz/ecommerce/repositories/UserProductInteractionRepository.java`
- `Backend/src/main/java/com/datasaz/ecommerce/repositories/RlModelMetadataRepository.java`
- `Backend/src/main/java/com/datasaz/ecommerce/repositories/UserContextFeaturesRepository.java`

### 1.2 Backend: Interaction Tracking Service

**Files to Create/Modify:**

1. **Interaction Service**

   - `Backend/src/main/java/com/datasaz/ecommerce/services/interfaces/IUserInteractionService.java`
   - `Backend/src/main/java/com/datasaz/ecommerce/services/implementations/UserInteractionService.java`
   - Methods:
     - `logInteraction(userId, productId, interactionType, context)`
     - `getUserInteractionHistory(userId, limit)`
     - `calculateReward(interactionType, product, context)`
     - `getUserContextFeatures(userId)`

2. **Interaction Controller**

   - `Backend/src/main/java/com/datasaz/ecommerce/controllers/UserInteractionController.java`
   - Endpoints:
     - `POST /api/v1/interactions/log` - Log user interaction
     - `GET /api/v1/interactions/user/{userId}` - Get user interaction history
     - `GET /api/v1/interactions/stats` - Get interaction statistics

3. **Modify Existing Controllers**

   - `Backend/src/main/java/com/datasaz/ecommerce/controllers/ProductController.java`
     - Add interaction logging on product view
   - `Backend/src/main/java/com/datasaz/ecommerce/controllers/buyer/BuyerCartController.java`
     - Add interaction logging on cart add
   - `Backend/src/main/java/com/datasaz/ecommerce/controllers/buyer/BuyerOrderController.java`
     - Add interaction logging on purchase

### 1.3 Backend: RL Service Integration

**Files to Create:**

1. **RL Service Client**

   - `Backend/src/main/java/com/datasaz/ecommerce/services/interfaces/IRlRecommendationService.java`
   - `Backend/src/main/java/com/datasaz/ecommerce/services/implementations/RlRecommendationService.java`
   - Methods:
     - `getRecommendations(userId, limit, context)` - Get RL-based recommendations
     - `trainModel(algorithmType)` - Trigger model training
     - `getModelStatus()` - Get current model status
   - Uses RestTemplate or WebClient to communicate with Python service

2. **RL Service Configuration**

   - `Backend/src/main/java/com/datasaz/ecommerce/configs/RlServiceConfig.java`
   - Configuration for Python service URL, timeouts, retry logic

3. **Modify Product Service**

   - `Backend/src/main/java/com/datasaz/ecommerce/services/implementations/ProductService.java`
   - Update `getRecommendedProducts()` to use RL service when available, fallback to existing logic

## Phase 2: Python RL Service Implementation

### 2.1 Project Structure

```
rl-recommendation-service/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI application
│   ├── config.py               # Configuration
│   ├── models/
│   │   ├── __init__.py
│   │   ├── user.py            # User context models
│   │   ├── product.py         # Product models
│   │   └── interaction.py     # Interaction models
│   ├── environment/
│   │   ├── __init__.py
│   │   └── ecommerce_env.py    # Gymnasium environment
│   ├── algorithms/
│   │   ├── __init__.py
│   │   ├── contextual_bandit.py  # LinUCB, Thompson Sampling
│   │   └── dqn.py             # Deep Q-Learning
│   ├── services/
│   │   ├── __init__.py
│   │   ├── recommendation_service.py
│   │   ├── training_service.py
│   │   └── data_service.py
│   ├── api/
│   │   ├── __init__.py
│   │   ├── recommendations.py  # Recommendation endpoints
│   │   ├── training.py         # Training endpoints
│   │   └── health.py           # Health check
│   └── utils/
│       ├── __init__.py
│       ├── database.py         # Database connection
│       └── feature_extraction.py
├── requirements.txt
├── Dockerfile
└── README.md
```

### 2.2 Core Components

**Files to Create:**

1. **FastAPI Application** (`app/main.py`)

   - Main FastAPI app with CORS, middleware
   - Route registration

2. **E-commerce Environment** (`app/environment/ecommerce_env.py`)

   - Gymnasium environment implementation
   - State space: user features, product features, interaction history
   - Action space: product recommendations
   - Reward function: based on clicks, purchases, engagement

3. **Contextual Bandit Algorithms** (`app/algorithms/contextual_bandit.py`)

   - LinUCB implementation
   - Thompson Sampling implementation
   - Model persistence

4. **DQN Implementation** (`app/algorithms/dqn.py`)

   - Deep Q-Network with PyTorch
   - Experience replay buffer
   - Target network updates

5. **Recommendation Service** (`app/services/recommendation_service.py`)

   - Load trained models
   - Generate recommendations based on user context
   - Handle cold-start users

6. **Training Service** (`app/services/training_service.py`)

   - Batch training from interaction logs
   - Online learning updates
   - Model evaluation and selection

7. **API Endpoints** (`app/api/recommendations.py`)

   - `POST /api/v1/recommendations/get` - Get recommendations
   - `POST /api/v1/recommendations/feedback` - Submit feedback
   - `GET /api/v1/recommendations/model/status` - Model status

8. **Training Endpoints** (`app/api/training.py`)

   - `POST /api/v1/training/train` - Trigger training
   - `GET /api/v1/training/status` - Training status
   - `POST /api/v1/training/evaluate` - Evaluate model

### 2.3 Dependencies (`requirements.txt`)

```
fastapi==0.104.1
uvicorn==0.24.0
pydantic==2.5.0
gymnasium==0.29.1
numpy==1.24.3
pandas==2.1.3
scikit-learn==1.3.2
torch==2.1.0
stable-baselines3==2.2.1
sqlalchemy==2.0.23
pymysql==1.1.0
python-dotenv==1.0.0
matplotlib==3.8.2
seaborn==0.13.0
```

## Phase 3: Frontend Integration

### 3.1 Interaction Tracking

**Files to Create/Modify:**

1. **Interaction Tracking Hook**

   - `Ecommercefront/app/hooks/useInteractionTracking.ts`
   - Functions:
     - `trackProductView(productId)`
     - `trackProductClick(productId)`
     - `trackCartAdd(productId)`
     - `trackPurchase(orderId)`

2. **Interaction Service**

   - `Ecommercefront/app/api/services/interaction.ts`
   - API calls to log interactions

3. **Modify Existing Components**

   - `Ecommercefront/components/ProductCard.tsx` - Add click tracking
   - `Ecommercefront/app/product/[id]/page.tsx` - Add view tracking
   - `Ecommercefront/app/cart/page.tsx` - Add cart interaction tracking

### 3.2 RL Recommendations Display

**Files to Create/Modify:**

1. **RL Recommendations Component**

   - `Ecommercefront/components/recommendations/RLRecommendationsSection.tsx`
   - Display RL-based recommendations
   - Handle loading and error states

2. **Update Recommendations Service**

   - `Ecommercefront/app/api/services/product.ts`
   - Enhance `getRecommendedProducts()` to use RL endpoint

3. **Update Homepage**

   - `Ecommercefront/app/page.tsx`
   - Integrate RL recommendations section

## Phase 4: Data Pipeline & Training

### 4.1 Data Synchronization

**Backend Service:**

- `Backend/src/main/java/com/datasaz/ecommerce/services/implementations/RlDataSyncService.java`
- Scheduled job to sync interactions to RL service
- Batch processing for efficiency

**Python Service:**

- `app/services/data_service.py`
- Endpoint to receive batch interactions
- Data preprocessing and feature extraction

### 4.2 Model Training Pipeline

**Training Workflow:**

1. Collect interactions for training period (e.g., last 30 days)
2. Extract features (user context, product features)
3. Train model (LinUCB/Thompson Sampling or DQN)
4. Evaluate on validation set
5. Deploy if performance improved
6. A/B testing with existing recommendations

**Files:**

- `app/services/training_service.py` - Training orchestration
- `app/utils/feature_extraction.py` - Feature engineering

## Phase 5: Evaluation & Monitoring

### 5.1 Metrics Tracking

**Metrics to Track:**

- Click-through rate (CTR)
- Conversion rate
- Revenue per recommendation
- Recommendation diversity
- User engagement

**Files to Create:**

- `Backend/src/main/java/com/datasaz/ecommerce/services/implementations/RlMetricsService.java`
- `app/services/metrics_service.py`

### 5.2 A/B Testing

**Implementation:**

- Split users into control (existing recommendations) and treatment (RL recommendations)
- Compare performance metrics
- Gradual rollout based on results

## Implementation Order

1. **Week 1-2: Database & Interaction Tracking**

   - Create database tables
   - Implement interaction tracking in backend
   - Add interaction logging to existing endpoints

2. **Week 3-4: Python RL Service Foundation**

   - Set up FastAPI project
   - Implement Gymnasium environment
   - Create basic API endpoints

3. **Week 5-6: RL Algorithms (Phase 1)**

   - Implement LinUCB
   - Implement Thompson Sampling
   - Integration with backend

4. **Week 7-8: Frontend Integration**

   - Add interaction tracking
   - Display RL recommendations
   - Testing

5. **Week 9-10: DQN Implementation (Phase 2)**

   - Implement DQN algorithm
   - Training pipeline
   - Model deployment

6. **Week 11-12: Evaluation & Optimization**

   - Metrics collection
   - A/B testing
   - Performance optimization

## Configuration & Deployment

### Environment Variables

**Backend:**

```properties
rl.service.url=http://localhost:8000
rl.service.timeout=5000
rl.service.retry.max-attempts=3
rl.enabled=true
```

**Python Service:**

```env
DATABASE_URL=mysql+pymysql://user:pass@host:3306/db
MODEL_STORAGE_PATH=/models
TRAINING_BATCH_SIZE=1000
ALGORITHM_TYPE=LINUCB  # or THOMPSON_SAMPLING, DQN
```

### Docker Setup

**Python Service Dockerfile:**

```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

## Testing Strategy

1. **Unit Tests**

   - RL algorithms
   - Environment logic
   - Interaction tracking

2. **Integration Tests**

   - Backend-RL service communication
   - End-to-end recommendation flow

3. **Performance Tests**

   - Recommendation latency
   - Training time
   - Concurrent request handling

## Documentation Requirements

1. API documentation (OpenAPI/Swagger)
2. RL algorithm documentation
3. Deployment guide
4. Training guide
5. Monitoring and metrics guide

## Notes

- The backend is currently connected to production, so all backend changes should be documented for PR review
- Frontend can be developed independently
- Python service can be developed as a separate microservice
- Consider using message queue (RabbitMQ/Kafka) for async interaction logging if volume is high
- Model versioning and rollback strategy needed for production

### To-dos

- [ ] Create database schema for user interactions, RL model metadata, and user context features
- [ ] Create JPA entities and repositories for UserProductInteraction, RlModelMetadata, and UserContextFeatures
- [ ] Implement UserInteractionService for logging interactions and calculating rewards
- [ ] Create UserInteractionController with endpoints for logging and retrieving interactions
- [ ] Add interaction logging to ProductController, BuyerCartController, and BuyerOrderController
- [ ] Create RlServiceConfig and RlRecommendationService for communicating with Python service
- [ ] Modify ProductService.getRecommendedProducts() to use RL service with fallback
- [ ] Set up Python FastAPI project structure with dependencies and configuration
- [ ] Implement Gymnasium e-commerce environment with state/action spaces and reward function
- [ ] Implement LinUCB and Thompson Sampling algorithms for Phase 1
- [ ] Implement Deep Q-Network (DQN) algorithm for Phase 2
- [ ] Create FastAPI endpoints for getting recommendations and submitting feedback
- [ ] Implement training service for batch training and online learning updates
- [ ] Create data synchronization service to send interactions from Java backend to Python service
- [ ] Create useInteractionTracking hook and interaction service for frontend
- [ ] Add interaction tracking to ProductCard, product detail page, and cart components
- [ ] Create RLRecommendationsSection component and integrate into homepage
- [ ] Implement metrics tracking service for CTR, conversion rate, and engagement metrics
- [ ] Implement A/B testing framework to compare RL vs existing recommendations