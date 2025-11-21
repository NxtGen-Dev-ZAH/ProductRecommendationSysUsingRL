"""
Configuration management for the RL recommendation service.
"""
from typing import Optional
from pydantic_settings import BaseSettings
from pydantic import Field


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""
    
    # Database Configuration
    database_url: str = Field(
        default="mysql+pymysql://dsazuser:DataROOT_4411630@localhost:3306/ecommercedb",
        alias="DATABASE_URL"
    )
    database_pool_size: int = Field(default=5, alias="DATABASE_POOL_SIZE")
    database_max_overflow: int = Field(default=10, alias="DATABASE_MAX_OVERFLOW")
    
    # Model Storage
    model_storage_path: str = Field(default="./models", alias="MODEL_STORAGE_PATH")
    model_checkpoint_path: str = Field(
        default="./models/checkpoints",
        alias="MODEL_CHECKPOINT_PATH"
    )
    
    # Training Configuration
    training_batch_size: int = Field(default=1000, alias="TRAINING_BATCH_SIZE")
    training_epochs: int = Field(default=100, alias="TRAINING_EPOCHS")
    learning_rate: float = Field(default=0.001, alias="LEARNING_RATE")
    discount_factor: float = Field(default=0.99, alias="DISCOUNT_FACTOR")
    
    # Algorithm Configuration
    algorithm_type: str = Field(default="LINUCB", alias="ALGORITHM_TYPE")
    exploration_rate: float = Field(default=0.1, alias="EXPLORATION_RATE")
    ucb_alpha: float = Field(default=1.0, alias="UCB_ALPHA")
    
    # Service Configuration
    rl_service_port: int = Field(default=8000, alias="RL_SERVICE_PORT")
    rl_service_host: str = Field(default="0.0.0.0", alias="RL_SERVICE_HOST")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    
    # Backend Integration
    backend_url: str = Field(
        default="http://localhost:8080/ecommerce",
        alias="BACKEND_URL"
    )
    backend_timeout: int = Field(default=30, alias="BACKEND_TIMEOUT")
    
    # Recommendation Configuration
    default_recommendation_limit: int = Field(
        default=6,
        alias="DEFAULT_RECOMMENDATION_LIMIT"
    )
    max_recommendation_limit: int = Field(
        default=20,
        alias="MAX_RECOMMENDATION_LIMIT"
    )
    min_interactions_for_training: int = Field(
        default=100,
        alias="MIN_INTERACTIONS_FOR_TRAINING"
    )
    
    # Feature Engineering
    user_feature_dim: int = Field(default=50, alias="USER_FEATURE_DIM")
    product_feature_dim: int = Field(default=50, alias="PRODUCT_FEATURE_DIM")
    context_feature_dim: int = Field(default=20, alias="CONTEXT_FEATURE_DIM")
    
    # Reward Configuration
    reward_view: float = Field(default=0.1, alias="REWARD_VIEW")
    reward_click: float = Field(default=0.5, alias="REWARD_CLICK")
    reward_cart_add: float = Field(default=2.0, alias="REWARD_CART_ADD")
    reward_purchase: float = Field(default=10.0, alias="REWARD_PURCHASE")
    reward_wishlist: float = Field(default=1.0, alias="REWARD_WISHLIST")
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"
        case_sensitive = False


# Global settings instance
settings = Settings()

