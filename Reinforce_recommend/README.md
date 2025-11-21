# RL Recommendation Service

A reinforcement learning-based product recommendation system for e-commerce platforms.

## Overview

This service uses contextual bandit algorithms (LinUCB and Thompson Sampling) to provide personalized product recommendations based on user interactions and context.

## Features

- **Contextual Bandits**: LinUCB and Thompson Sampling algorithms
- **Real-time Recommendations**: Fast inference for product suggestions
- **Online Learning**: Continuous model updates from user feedback
- **Feature Engineering**: Automatic extraction of user and product features
- **Gymnasium Environment**: Standard RL environment for e-commerce
- **RESTful API**: FastAPI-based endpoints

## Architecture

```
rl-recommendation-service/
├── app/
│   ├── main.py                    # FastAPI application
│   ├── config.py                  # Configuration management
│   ├── models/                    # Pydantic data models
│   ├── api/                       # API endpoints
│   ├── algorithms/                # RL algorithms (LinUCB, Thompson Sampling)
│   ├── environment/               # Gymnasium environment
│   ├── services/                  # Business logic services
│   └── utils/                     # Utilities (database, features)
├── models/                        # Trained model storage
├── requirements.txt               # Python dependencies
└── .env                          # Environment variables
```

## Setup

### 1. Create Virtual Environment

```bash
python3 -m venv venv
source venv/bin/activate  # On macOS/Linux
# or: venv\Scripts\activate  # On Windows
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Configure Environment

Copy `.env.template` to `.env` and update with your settings:

```bash
cp .env.template .env
```

Edit `.env` with your database credentials and configuration.

### 4. Create Models Directory

```bash
mkdir -p models/checkpoints
```

### 5. Run the Service

```bash
# Development mode with auto-reload
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Production mode
uvicorn app.main:app --host 0.0.0.0 --port 8000 --workers 4
```

The service will be available at `http://localhost:8000`

## API Endpoints

### Health Check

```bash
GET /api/v1/health
GET /api/v1/health/ready
GET /api/v1/health/live
```

### Recommendations

```bash
# Get recommendations
POST /api/v1/recommendations/get
{
  "user_id": 1,
  "limit": 6,
  "category_id": 2,
  "price_range_min": 10.0,
  "price_range_max": 100.0
}

# Submit feedback
POST /api/v1/recommendations/feedback
{
  "user_id": 1,
  "recommended_products": [101, 102, 103],
  "selected_product": 102,
  "reward": 10.0
}

# Get model status
GET /api/v1/recommendations/model/status
```

### Training

```bash
# Start training
POST /api/v1/training/train
{
  "algorithm_type": "LINUCB",
  "batch_size": 1000,
  "epochs": 100,
  "days_lookback": 30
}

# Get training status
GET /api/v1/training/status?training_id=<id>

# Evaluate model
POST /api/v1/training/evaluate
{
  "algorithm_type": "LINUCB",
  "test_size": 0.2
}
```

## Algorithms

### LinUCB (Linear Upper Confidence Bound)

- Balances exploration and exploitation using confidence bounds
- Efficient for linear reward functions
- Good for cold-start scenarios

### Thompson Sampling

- Bayesian approach with posterior sampling
- Naturally handles exploration-exploitation tradeoff
- Often converges faster than UCB-based methods

## Configuration

Key configuration parameters in `.env`:

```env
# Algorithm selection
ALGORITHM_TYPE=LINUCB  # or THOMPSON_SAMPLING

# Model parameters
UCB_ALPHA=1.0
EXPLORATION_RATE=0.1

# Rewards
REWARD_VIEW=0.1
REWARD_CLICK=0.5
REWARD_CART_ADD=2.0
REWARD_PURCHASE=10.0

# Training
TRAINING_BATCH_SIZE=1000
MIN_INTERACTIONS_FOR_TRAINING=100
```

## Development

### Running Tests

```bash
pytest tests/ -v --cov=app
```

### Code Formatting

```bash
black app/
isort app/
```

### Type Checking

```bash
mypy app/
```

## Integration

### With Java Backend

The service integrates with the existing Java Spring Boot backend:

1. Backend logs user interactions to database
2. RL service reads interactions for training
3. Backend calls RL service for recommendations
4. RL service returns personalized products

See the main integration plan for detailed setup.

## Monitoring

- Health endpoints for Kubernetes liveness/readiness probes
- Structured logging with loguru
- Performance metrics tracking
- Model version management

## License

Copyright © 2024 DataSaz E-commerce Team

