# RL Recommendation System - Complete Deployment Guide

## 🎯 Overview

This guide provides step-by-step instructions for deploying the RL Recommendation System across all three components: Python RL Service, Java Backend, and Next.js Frontend.

## 📋 Prerequisites

### System Requirements
- **Operating System**: Linux, macOS, or Windows (with WSL)
- **Python**: 3.11+
- **Java**: 17+
- **Node.js**: 18+
- **MySQL**: 8.0.28+
- **RAM**: Minimum 8GB (16GB recommended)
- **Storage**: 10GB free space

### Development Tools
- Docker (optional, for containerized deployment)
- Git
- Maven 3.6+
- npm/yarn

## 🚀 Quick Start (Development)

### Step 1: Clone and Setup Repository

```bash
cd /Users/devzaheerai/CodeWork/EcommerceRL

# Verify structure
ls -la
# Should see: Backend/, Frontend/, Reinforce_recommend/
```

### Step 2: Database Setup

```bash
# Start MySQL (if not running)
mysql.server start  # macOS
# or: sudo systemctl start mysql  # Linux

# Login to MySQL
mysql -u dsazuser -p ecommercedb

# Run migration
source Backend/documentation/create_rl_tables.sql

# Verify tables
SHOW TABLES LIKE '%interactions%';
SHOW TABLES LIKE 'rl_%';
```

### Step 3: Python RL Service

```bash
cd Reinforce_recommend

# Create virtual environment
python3 -m venv venv
source venv/bin/activate  # macOS/Linux
# or: venv\Scripts\activate  # Windows

# Install dependencies
pip install -r requirements.txt

# Create models directory
mkdir -p models/checkpoints

# Start service
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# In another terminal, test health
curl http://localhost:8000/api/v1/health
```

**Expected output:**
```json
{
  "status": "healthy",
  "timestamp": "2024-11-20T...",
  "version": "0.1.0",
  "algorithm": "LINUCB",
  "database": "connected"
}
```

### Step 4: Java Backend (Optional for Local Testing)

```bash
cd Backend

# Build project
./mvnw clean install -DskipTests

# Run with RL profile (create application-rl-dev.yml first)
# Note: Since backend is in production, test RL service independently
```

### Step 5: Frontend

```bash
cd Frontend

# Install dependencies
npm install

# Start development server
npm run dev

# Open browser
open http://localhost:3000
```

## 🔧 Configuration

### Python RL Service (.env)

```env
# Database
DATABASE_URL=mysql+pymysql://dsazuser:DataROOT_4411630@localhost:3306/ecommercedb

# Algorithm
ALGORITHM_TYPE=LINUCB
UCB_ALPHA=1.0

# Service
RL_SERVICE_PORT=8000
LOG_LEVEL=INFO

# Rewards
REWARD_VIEW=0.1
REWARD_CLICK=0.5
REWARD_CART_ADD=2.0
REWARD_PURCHASE=10.0
```

### Backend (application-rl-dev.yml)

```yaml
rl:
  service:
    url: http://localhost:8000
    timeout: 5000
    retry:
      max-attempts: 3
  enabled: true
```

### Frontend (env variables)

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/ecommerce
NEXT_PUBLIC_RL_ENABLED=true
```

## 🧪 Testing the Integration

### 1. Test RL Service Health

```bash
curl http://localhost:8000/api/v1/health
curl http://localhost:8000/api/v1/health/ready
curl http://localhost:8000/api/v1/health/live
```

### 2. Test Recommendations (Mock Data)

```bash
curl -X POST http://localhost:8000/api/v1/recommendations/get \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": 1,
    "limit": 6
  }'
```

### 3. Test Training

```bash
curl -X POST http://localhost:8000/api/v1/training/train \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm_type": "LINUCB",
    "batch_size": 1000,
    "epochs": 10,
    "days_lookback": 30
  }'
```

### 4. Check Training Status

```bash
curl http://localhost:8000/api/v1/training/status
```

## 📊 Training the Model

### Prerequisites for Training
- Minimum 100 user interactions logged
- At least 30 days of data (recommended)
- Active users with multiple interactions

### Training Steps

1. **Verify Interaction Data**

```sql
SELECT COUNT(*) FROM user_product_interactions;
SELECT 
    interaction_type, 
    COUNT(*) as count 
FROM user_product_interactions 
GROUP BY interaction_type;
```

2. **Start Training**

```bash
curl -X POST http://localhost:8000/api/v1/training/train \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm_type": "LINUCB",
    "batch_size": 1000,
    "epochs": 100,
    "learning_rate": 0.001,
    "days_lookback": 30
  }'
```

3. **Monitor Training**

```bash
# Check logs
tail -f logs/rl-service.log

# Or check status endpoint
curl http://localhost:8000/api/v1/training/status
```

4. **Evaluate Model**

```bash
curl -X POST http://localhost:8000/api/v1/training/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "algorithm_type": "LINUCB",
    "test_size": 0.2
  }'
```

## 🐳 Docker Deployment

### Build Docker Image

```bash
cd Reinforce_recommend

# Build
docker build -t rl-recommendation-service:latest .

# Run
docker run -d \
  --name rl-service \
  -p 8000:8000 \
  -e DATABASE_URL=mysql+pymysql://user:pass@host:3306/db \
  -v $(pwd)/models:/app/models \
  rl-recommendation-service:latest

# Check logs
docker logs -f rl-service

# Stop
docker stop rl-service
```

### Docker Compose (All Services)

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: ecommercedb
      MYSQL_USER: dsazuser
      MYSQL_PASSWORD: DataROOT_4411630
      MYSQL_ROOT_PASSWORD: rootpass
    volumes:
      - mysql-data:/var/lib/mysql
    ports:
      - "3306:3306"

  rl-service:
    build: ./Reinforce_recommend
    ports:
      - "8000:8000"
    environment:
      DATABASE_URL: mysql+pymysql://dsazuser:DataROOT_4411630@mysql:3306/ecommercedb
      ALGORITHM_TYPE: LINUCB
    volumes:
      - ./Reinforce_recommend/models:/app/models
    depends_on:
      - mysql

  backend:
    build: ./Backend
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      RL_SERVICE_URL: http://rl-service:8000
    depends_on:
      - mysql
      - rl-service

  frontend:
    build: ./Frontend
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080/ecommerce
    depends_on:
      - backend

volumes:
  mysql-data:
```

## 🔍 Monitoring and Logging

### Log Locations

- **RL Service**: `Reinforce_recommend/logs/` or stdout
- **Backend**: `Backend/logs/`
- **Frontend**: Browser console and Next.js logs

### Key Metrics to Monitor

1. **RL Service Metrics**
   - Request latency
   - Model inference time
   - Recommendation quality (CTR, conversion)
   - Training job success rate

2. **Interaction Tracking**
   - Interactions logged per minute
   - Interaction type distribution
   - User coverage

3. **System Health**
   - CPU and memory usage
   - Database connection pool
   - API response times

### Monitoring Commands

```bash
# Check RL service health
watch -n 5 'curl -s http://localhost:8000/api/v1/health | jq'

# Monitor interactions in real-time
mysql -u dsazuser -p ecommercedb -e "
SELECT 
    interaction_type, 
    COUNT(*) as count,
    MAX(timestamp) as last_interaction
FROM user_product_interactions
WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY interaction_type;"

# Check model performance
curl -s http://localhost:8000/api/v1/recommendations/metrics | jq
```

## 🔒 Security Considerations

1. **Environment Variables**: Never commit .env files
2. **Database Credentials**: Use secrets management in production
3. **API Authentication**: Implement proper auth for RL service
4. **HTTPS**: Use HTTPS in production
5. **Rate Limiting**: Implement rate limiting on endpoints

## 🐛 Troubleshooting

### Issue: RL Service Won't Start

```bash
# Check Python version
python --version  # Should be 3.11+

# Check dependencies
pip list

# Check port availability
lsof -i :8000

# Check logs
tail -f logs/rl-service.log
```

### Issue: Database Connection Failed

```bash
# Test MySQL connection
mysql -u dsazuser -p ecommercedb

# Check if tables exist
mysql -u dsazuser -p ecommercedb -e "SHOW TABLES;"

# Verify credentials in .env
cat Reinforce_recommend/.env | grep DATABASE_URL
```

### Issue: No Recommendations Returned

```bash
# Check if model exists
ls -la Reinforce_recommend/models/

# Check interaction data
mysql -u dsazuser -p ecommercedb -e "SELECT COUNT(*) FROM user_product_interactions;"

# Train model if needed
curl -X POST http://localhost:8000/api/v1/training/train \
  -H "Content-Type: application/json" \
  -d '{"algorithm_type": "LINUCB", "epochs": 10}'
```

### Issue: Frontend Can't Connect to Backend

```bash
# Check backend is running
curl http://localhost:8080/ecommerce/health

# Check CORS settings
# Verify Next.js proxy configuration
cat Frontend/next.config.ts

# Check env variables
cat Frontend/.env.local
```

## 📈 Performance Optimization

### Python RL Service
- Use gunicorn with multiple workers
- Enable response caching
- Optimize feature extraction queries
- Use connection pooling for database

### Backend
- Enable JPA query caching
- Use async processing for interaction logging
- Batch interaction writes
- Optimize database queries

### Frontend
- Implement request debouncing
- Use SWR or React Query for caching
- Lazy load recommendation components
- Optimize image loading

## 🚀 Production Deployment Checklist

- [ ] Database migration executed
- [ ] RL service deployed and healthy
- [ ] Model trained with production data
- [ ] Backend integration tested
- [ ] Frontend interaction tracking verified
- [ ] Monitoring and alerting configured
- [ ] Backup strategy implemented
- [ ] Load testing completed
- [ ] Documentation updated
- [ ] Team trained on new system

## 📚 Additional Resources

- [Python RL Service README](Reinforce_recommend/README.md)
- [Database Migration Guide](Backend/documentation/RL_DATABASE_MIGRATION.md)
- [Backend Implementation Guide](Backend/documentation/RL_BACKEND_IMPLEMENTATION.md)
- [Integration Plan](rl-recommendation-integration.plan.md)

## 🆘 Support

For issues or questions:
1. Check documentation in `Backend/documentation/`
2. Review logs for error messages
3. Verify configuration files
4. Check GitHub issues (if applicable)

---

**Version**: 1.0.0  
**Last Updated**: 2024-11-20  
**Maintained by**: DataSaz E-commerce Team

