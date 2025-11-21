"""
Gymnasium environment for e-commerce product recommendations.
"""
import gymnasium as gym
from gymnasium import spaces
import numpy as np
from typing import List, Dict, Any, Optional, Tuple
from loguru import logger

from app.config import settings
from app.models.product import Product
from app.models.user import UserFeatures, UserContext
from app.utils.feature_extraction import FeatureExtractor


class EcommerceRecommendationEnv(gym.Env):
    
    metadata = {"render_modes": ["human"]}
    
    def __init__(
        self,
        products: List[Product],
        max_recommendations: int = 6,
        feature_extractor: Optional[FeatureExtractor] = None
    ):
        """
        Initialize the environment.
        
        Args:
            products: Available products
            max_recommendations: Maximum products to recommend
            feature_extractor: Feature extraction utility
        """
        super().__init__()
        
        self.products = products
        self.product_ids = [p.id for p in products]
        self.max_recommendations = max_recommendations
        self.feature_extractor = feature_extractor or FeatureExtractor()
        
        # Calculate feature dimensions
        self.user_dim = settings.user_feature_dim
        self.context_dim = settings.context_feature_dim
        self.product_dim = settings.product_feature_dim
        self.state_dim = self.user_dim + self.context_dim
        
        # Define observation space (continuous features)
        self.observation_space = spaces.Box(
            low=-np.inf,
            high=np.inf,
            shape=(self.state_dim,),
            dtype=np.float32
        )
        
        # Define action space (discrete product selection)
        self.action_space = spaces.Discrete(len(self.products))
        
        # Environment state
        self.current_user: Optional[UserFeatures] = None
        self.current_context: Optional[UserContext] = None
        self.episode_rewards: List[float] = []
        self.episode_actions: List[int] = []
        
        logger.info(
            f"Initialized EcommerceRecommendationEnv with "
            f"{len(self.products)} products, "
            f"state_dim={self.state_dim}, "
            f"action_space={len(self.products)}"
        )
    
    def reset(
        self,
        seed: Optional[int] = None,
        options: Optional[Dict[str, Any]] = None
    ) -> Tuple[np.ndarray, Dict[str, Any]]:
        """
        Reset the environment for a new episode.
        
        Args:
            seed: Random seed
            options: Additional options (user_features, context)
            
        Returns:
            Initial observation and info dict
        """
        super().reset(seed=seed)
        
        # Reset episode tracking
        self.episode_rewards = []
        self.episode_actions = []
        
        # Set user and context from options
        if options:
            self.current_user = options.get("user_features")
            self.current_context = options.get("context")
        
        # Build initial state
        state = self._build_state()
        
        info = {
            "user_id": self.current_user.user_id if self.current_user else None,
            "episode_length": 0
        }
        
        return state, info
    
    def step(
        self,
        action: int
    ) -> Tuple[np.ndarray, float, bool, bool, Dict[str, Any]]:
        """
        Execute one step in the environment.
        
        Args:
            action: Product index to recommend
            
        Returns:
            observation, reward, terminated, truncated, info
        """
        # Validate action
        if action < 0 or action >= len(self.products):
            raise ValueError(f"Invalid action: {action}")
        
        # Get recommended product
        recommended_product = self.products[action]
        
        # Calculate reward (would come from user interaction in real scenario)
        # For training, this would be based on logged data
        reward = self._calculate_reward(recommended_product)
        
        # Track episode
        self.episode_rewards.append(reward)
        self.episode_actions.append(action)
        
        # Get next state (state doesn't change within same user session)
        next_state = self._build_state()
        
        # Episode ends after one recommendation (can be extended)
        terminated = True
        truncated = False
        
        info = {
            "product_id": recommended_product.id,
            "product_name": recommended_product.name,
            "reward": reward,
            "episode_length": len(self.episode_actions),
            "total_reward": sum(self.episode_rewards)
        }
        
        return next_state, reward, terminated, truncated, info
    
    def _build_state(self) -> np.ndarray:
        """
        Build state representation from user features and context.
        
        Returns:
            State vector
        """
        # User features
        if self.current_user:
            user_features = self.current_user.feature_vector
        else:
            user_features = [0.0] * self.user_dim
        
        # Context features
        if self.current_context:
            context_features = self.feature_extractor.build_context_features(
                self.current_context
            )
        else:
            context_features = [0.0] * self.context_dim
        
        # Combine features
        state = np.array(user_features + context_features, dtype=np.float32)
        
        return state
    
    def _calculate_reward(self, product: Product) -> float:
        """
        Calculate reward for recommending a product.
        
        In training, this would be based on actual user interaction.
        For simulation, we can use heuristics based on product features.
        
        Args:
            product: Recommended product
            
        Returns:
            Reward value
        """
        # Base reward (would come from interaction logging)
        base_reward = 0.0
        
        # Heuristic rewards for simulation
        # Higher reward for products matching user preferences
        if self.current_user and product.category_id in self.current_user.favorite_categories:
            base_reward += 1.0
        
        # Reward for good products (high rating)
        if product.rating >= 4.0:
            base_reward += 0.5
        
        # Reward for products in stock
        if product.stock_quantity > 0:
            base_reward += 0.2
        
        # Reward for products in user's price range
        if self.current_context:
            if (self.current_context.price_range_min and 
                self.current_context.price_range_max):
                if (self.current_context.price_range_min <= product.price <= 
                    self.current_context.price_range_max):
                    base_reward += 0.5
        
        return base_reward
    
    def render(self):
        """Render the environment (optional)."""
        if self.current_user:
            print(f"User ID: {self.current_user.user_id}")
            print(f"Episode Length: {len(self.episode_actions)}")
            print(f"Total Reward: {sum(self.episode_rewards):.2f}")
            if self.episode_actions:
                print(f"Last Action: {self.episode_actions[-1]}")
                print(f"Last Reward: {self.episode_rewards[-1]:.2f}")
    
    def get_product_features(self, product_id: int) -> Optional[np.ndarray]:
        """
        Get feature vector for a product.
        
        Args:
            product_id: Product ID
            
        Returns:
            Product feature vector or None
        """
        for product in self.products:
            if product.id == product_id:
                # Would use feature_extractor in production
                return np.random.randn(self.product_dim).astype(np.float32)
        return None
    
    def get_valid_actions(self, exclude_products: Optional[List[int]] = None) -> List[int]:
        """
        Get list of valid action indices.
        
        Args:
            exclude_products: Product IDs to exclude
            
        Returns:
            List of valid action indices
        """
        exclude_products = exclude_products or []
        valid_actions = []
        
        for idx, product in enumerate(self.products):
            if product.id not in exclude_products and product.stock_quantity > 0:
                valid_actions.append(idx)
        
        return valid_actions

