## RL recommendation system — setup and run guide

### Current state
- Backend: Spring Boot 3.3.5, Java 17, MySQL, connected to production
- Frontend: Next.js, can be developed independently
- Recommendations: Basic endpoint exists (`/api/v1/products/recommendations`) with placeholder logic
- Database: MySQL 8.0.28

### Development strategy

Since the backend is in the main repo and connected to production:

1. Backend changes: Document in Markdown, create PRs for review
2. Frontend: Develop independently in your local workspace
3. Python RL service: Separate microservice, develop locally

---

## Step-by-step setup

### Phase 1: Environment setup

#### 1.1 Database setup (local development)

Create the new tables locally first:

```sql
-- Run these in your local MySQL database (ecommercedb)
-- Based on the plan, create these tables:

CREATE TABLE user_product_interactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    session_id VARCHAR(255),
    timestamp DATETIME NOT NULL,
    context JSON,
    reward DECIMAL(10,2),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES product(id),
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_product_timestamp (product_id, timestamp)
);

CREATE TABLE rl_model_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    algorithm_type VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    training_date DATETIME,
    performance_metrics JSON,
    model_path VARCHAR(500),
    created_at DATETIME NOT NULL
);

CREATE TABLE user_context_features (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    feature_vector JSON NOT NULL,
    last_updated DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

Note: Document this SQL in a migration file for the backend PR.

#### 1.2 Python RL service setup

Create the Python service as a separate project:

```bash
# Navigate to your workspace root
cd /Users/devzaheerai/CodeWork/Ecommercework

# Create the RL service directory
mkdir rl-recommendation-service
cd rl-recommendation-service

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # On macOS/Linux
# or: venv\Scripts\activate  # On Windows

# Create project structure
mkdir -p app/{models,environment,algorithms,services,api,utils}
touch app/__init__.py
touch app/main.py
touch app/config.py
touch app/models/__init__.py
touch app/environment/__init__.py
touch app/algorithms/__init__.py
touch app/services/__init__.py
touch app/api/__init__.py
touch app/utils/__init__.py
touch requirements.txt
touch Dockerfile
touch .env
touch README.md
```

Create `requirements.txt`:

```txt
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

Create `.env`:

```env
# Database connection (use your local MySQL credentials)
DATABASE_URL=mysql+pymysql://dsazuser:DataROOT_4411630@localhost:3306/ecommercedb

# Model storage
MODEL_STORAGE_PATH=./models

# Training configuration
TRAINING_BATCH_SIZE=1000
ALGORITHM_TYPE=LINUCB

# Service configuration
RL_SERVICE_PORT=8000
RL_SERVICE_HOST=0.0.0.0

# Backend integration
BACKEND_URL=http://localhost:8080/ecommerce
```

#### 1.3 Backend configuration (local development)

Since the backend uses the `prod` profile by default, create a local profile for RL development:

Create `Backend/src/main/resources/application-rl-dev.yml`:

```yaml
# RL Service Configuration
rl:
  service:
    url: http://localhost:8000
    timeout: 5000
    retry:
      max-attempts: 3
  enabled: true

# Keep existing database config from dev profile
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommercedb
    username: dsazuser
    password: DataROOT_4411630
```

Update `Backend/src/main/resources/application.properties` for local development:

```properties
# For local RL development, use:
# spring.profiles.active=rl-dev

# For production (current):
spring.profiles.active=prod
```

Note: Do not commit the `rl-dev` profile changes directly. Document them for the PR.

---

### Phase 2: Running the system

#### 2.1 Start MySQL

```bash
# Ensure MySQL is running locally
# Check connection with:
mysql -u dsazuser -p ecommercedb
```

#### 2.2 Start Python RL service

```bash
cd rl-recommendation-service

# Activate virtual environment
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Create models directory
mkdir -p models

# Start the service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The service should be available at `http://localhost:8000`

#### 2.3 Start backend (if testing locally)

```bash
cd Backend

# For local RL development, temporarily change application.properties:
# spring.profiles.active=rl-dev

# Run Spring Boot
./mvnw spring-boot:run
# or: mvn spring-boot:run
```

Note: Since the backend is connected to production, you may want to test the RL service independently first.

#### 2.4 Start frontend

```bash
cd Ecommercefront

# Install dependencies (if not already done)
npm install

# Start development server
npm run dev
```

---

### Phase 3: Development workflow

#### 3.1 Backend development workflow

1. Create a feature branch:
   ```bash
   git checkout -b feature/rl-recommendation-integration
   ```

2. Implement changes locally:
   - Create entities, repositories, services
   - Add RL service integration
   - Document all changes

3. Create documentation file:
   Create `Documentation/rl-backend-implementation.md` with:
   - SQL migration scripts
   - New classes and their purposes
   - Configuration changes
   - API endpoint changes
   - Testing instructions

4. Create PR with:
   - Code changes
   - Documentation file
   - Migration SQL
   - Testing notes

#### 3.2 Python RL service development

Develop independently in `rl-recommendation-service/`:

1. Implement core components:
   - FastAPI app (`app/main.py`)
   - Environment (`app/environment/ecommerce_env.py`)
   - Algorithms (`app/algorithms/contextual_bandit.py`)
   - Services (`app/services/recommendation_service.py`)

2. Test independently:
   ```bash
   # Test health endpoint
   curl http://localhost:8000/health
   
   # Test recommendation endpoint (once implemented)
   curl -X POST http://localhost:8000/api/v1/recommendations/get \
     -H "Content-Type: application/json" \
     -d '{"user_id": 1, "limit": 6}'
   ```

3. Version control:
   - Can be a separate repo or a subdirectory
   - Add `.gitignore` for `venv/`, `models/`, `__pycache__/`

#### 3.3 Frontend development

Develop in `Ecommercefront/`:

1. Create interaction tracking:
   - `app/hooks/useInteractionTracking.ts`
   - `app/api/services/interaction.ts`

2. Integrate RL recommendations:
   - Update `app/api/services/product.ts`
   - Create `components/recommendations/RLRecommendationsSection.tsx`

3. Test locally:
   - Frontend connects to production backend
   - RL service runs locally
   - Test interaction tracking and recommendations

---

### Phase 4: Testing strategy

#### 4.1 Unit testing (Python service)

```bash
cd rl-recommendation-service

# Install test dependencies
pip install pytest pytest-asyncio

# Run tests
pytest tests/
```

#### 4.2 Integration testing

1. Test backend → RL service:
   ```bash
   # Start RL service
   # Test from backend using RestTemplate/WebClient
   ```

2. Test frontend → backend → RL service:
   - Use browser dev tools
   - Check network requests
   - Verify interaction logging

#### 4.3 End-to-end testing

1. User flow:
   - Browse products → track views
   - Click product → track clicks
   - Add to cart → track cart additions
   - Purchase → track purchases
   - Check recommendations update

---

### Phase 5: Deployment considerations

#### 5.1 Local development setup

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│  Frontend   │─────▶│   Backend    │─────▶│  Production │
│ (Local:3000)│      │ (Production) │      │   Database  │
└─────────────┘      └──────────────┘      └─────────────┘
                            │
                            │ (RL Service calls)
                            ▼
                     ┌──────────────┐
                     │  RL Service  │
                     │ (Local:8000) │
                     └──────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │ Local MySQL  │
                     │ (ecommercedb)│
                     └──────────────┘
```

#### 5.2 Production deployment (future)

1. Python RL service:
   - Deploy as Docker container
   - Use environment variables for config
   - Connect to production database (read-only for training data)

2. Backend integration:
   - Add RL service URL to production config
   - Enable RL recommendations gradually (feature flag)
   - Monitor performance

---

### Phase 6: Implementation order (recommended)

Week 1-2: Foundation
1. Set up Python RL service structure
2. Create database tables (document SQL)
3. Implement basic FastAPI endpoints
4. Test service independently

Week 3-4: Backend integration
1. Create backend entities and repositories (document for PR)
2. Implement interaction tracking service
3. Create RL service client
4. Test backend → RL service communication

Week 5-6: Algorithms
1. Implement LinUCB algorithm
2. Implement Thompson Sampling
3. Create training pipeline
4. Test with sample data

Week 7-8: Frontend integration
1. Add interaction tracking hooks
2. Create RL recommendations component
3. Integrate into homepage
4. Test end-to-end flow

---

### Quick start checklist

- [ ] Set up local MySQL database
- [ ] Create database tables (document SQL)
- [ ] Create Python RL service project structure
- [ ] Install Python dependencies
- [ ] Create `.env` file with database credentials
- [ ] Implement basic FastAPI app with health endpoint
- [ ] Test RL service runs on port 8000
- [ ] Document backend changes needed (for PR)
- [ ] Set up frontend interaction tracking
- [ ] Test end-to-end flow

---

### Important notes

1. Backend changes: Document everything for PR review. Do not commit directly to main.
2. Database: Use local MySQL for development. Production changes require migration scripts.
3. RL service: Can be developed independently. Test locally before integration.
4. Frontend: Can connect to production backend while developing RL features locally.
5. Testing: Test each component independently before full integration.

---

### Next steps

1. Start with the Python RL service foundation (Week 1-2)
2. Create the database schema and document it
3. Implement basic FastAPI endpoints
4. Then move to backend integration

Should I:
1. Create the initial Python RL service structure with basic FastAPI setup?
2. Generate the SQL migration scripts with documentation?
3. Create a detailed implementation guide for a specific phase?