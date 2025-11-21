"""
Recommendation service using RL algorithms.
"""
import os
from typing import List, Optional, Dict, Any
from datetime import datetime
from loguru import logger
import numpy as np

from app.config import settings
from app.models.recommendation import (
    RecommendationRequest,
    RecommendationResponse,
    RecommendationItem,
    RecommendationMetrics
)
from app.models.interaction import FeedbackData
from app.models.user import UserContext
from app.algorithms.contextual_bandit import LinUCB, ThompsonSampling
from app.services.data_service import DataService
from app.utils.feature_extraction import FeatureExtractor


class RecommendationService:
    """Service for generating recommendations using RL."""
    
    def __init__(self):
        self.data_service = DataService()
        self.feature_extractor = FeatureExtractor()
        self.model: Optional[Any] = None
        self.model_type: Optional[str] = None
        self.model_version: Optional[str] = None
        
        # Try to load existing model
        self._load_model()
    
    def _load_model(self):
        """Load trained model from disk."""
        try:
            model_path = os.path.join(
                settings.model_storage_path,
                f"{settings.algorithm_type.lower()}_model.pkl"
            )
            
            if os.path.exists(model_path):
                if settings.algorithm_type == "LINUCB":
                    self.model = LinUCB.load(model_path)
                elif settings.algorithm_type == "THOMPSON_SAMPLING":
                    self.model = ThompsonSampling.load(model_path)
                
                self.model_type = settings.algorithm_type
                self.model_version = "0.1.0"
                logger.info(f"Loaded {self.model_type} model from {model_path}")
            else:
                logger.warning(f"No trained model found at {model_path}")
                
        except Exception as e:
            logger.error(f"Error loading model: {e}")
    
    async def get_recommendations(
        self,
        request: RecommendationRequest
    ) -> RecommendationResponse:
        """
        Generate product recommendations for a user.
        
        Args:
            request: Recommendation request
            
        Returns:
            Recommendation response with products
        """
        try:
            # Get available products
            products = await self.data_service.get_active_products(
                category_id=request.category_id
            )
            
            if not products:
                logger.warning("No active products available")
                return RecommendationResponse(
                    user_id=request.user_id,
                    recommendations=[],
                    total_count=0,
                    algorithm_used="FALLBACK",
                    model_version=None
                )
            
            # Extract user features
            user_features = await self.feature_extractor.extract_user_features(
                request.user_id
            )
            
            # Build context
            user_context = UserContext(
                user_id=request.user_id,
                current_category=request.category_id,
                price_range_min=request.price_range_min,
                price_range_max=request.price_range_max,
                **request.context
            )
            
            # Get context features
            context_features = self.feature_extractor.build_context_features(
                user_context
            )
            
            # Combine features for state representation
            state_features = user_features.feature_vector + context_features
            state = np.array(state_features, dtype=np.float32)
            
            # Get recommendations
            if self.model and self.model_type in ["LINUCB", "THOMPSON_SAMPLING"]:
                recommendations = await self._get_rl_recommendations(
                    state=state,
                    products=products,
                    request=request
                )
                algorithm_used = self.model_type
            else:
                # Fallback to simple recommendations
                recommendations = await self._get_fallback_recommendations(
                    products=products,
                    request=request,
                    user_features=user_features
                )
                algorithm_used = "FALLBACK"
            
            return RecommendationResponse(
                user_id=request.user_id,
                recommendations=recommendations,
                total_count=len(recommendations),
                algorithm_used=algorithm_used,
                model_version=self.model_version,
                metadata={
                    "category_id": request.category_id,
                    "price_filter": {
                        "min": request.price_range_min,
                        "max": request.price_range_max
                    }
                }
            )
            
        except Exception as e:
            logger.error(f"Error generating recommendations: {e}", exc_info=True)
            raise
    
    async def _get_rl_recommendations(
        self,
        state: np.ndarray,
        products: List,
        request: RecommendationRequest
    ) -> List[RecommendationItem]:
        """Get recommendations using RL model."""
        recommendations = []
        excluded_ids = set(request.exclude_products)
        
        # Create product index mapping
        product_map = {i: p for i, p in enumerate(products) if p.id not in excluded_ids}
        valid_actions = list(product_map.keys())
        
        if not valid_actions:
            return []
        
        # Get multiple recommendations
        selected_products = set()
        
        for rank in range(1, min(request.limit + 1, len(valid_actions) + 1)):
            # Get available actions
            available_actions = [
                a for a in valid_actions 
                if product_map[a].id not in selected_products
            ]
            
            if not available_actions:
                break
            
            # Select action using model
            if self.model_type == "LINUCB":
                action, score = self.model.select_action(state, available_actions)
            else:  # THOMPSON_SAMPLING
                action, score = self.model.select_action(state, available_actions)
            
            product = product_map[action]
            selected_products.add(product.id)
            
            # Create recommendation item
            recommendations.append(
                RecommendationItem(
                    product_id=product.id,
                    product_name=product.name,
                    category_id=product.category_id,
                    category_name=product.category_name,
                    brand=product.brand,
                    price=product.price,
                    discount_price=product.discount_price,
                    discount_percentage=product.discount_percentage,
                    image_url=product.image_url,
                    rating=product.rating,
                    review_count=product.review_count,
                    confidence_score=min(max(score / 10.0, 0.0), 1.0),
                    rank=rank,
                    reason="Personalized recommendation based on your preferences"
                )
            )
        
        return recommendations
    
    async def _get_fallback_recommendations(
        self,
        products: List,
        request: RecommendationRequest,
        user_features: Any
    ) -> List[RecommendationItem]:
        """Get fallback recommendations (rule-based)."""
        # Filter products
        filtered_products = [
            p for p in products
            if p.id not in request.exclude_products
            and p.stock_quantity > 0
        ]
        
        # Apply price filter
        if request.price_range_min is not None and request.price_range_max is not None:
            filtered_products = [
                p for p in filtered_products
                if request.price_range_min <= p.price <= request.price_range_max
            ]
        
        # Score products
        scored_products = []
        favorite_cats = user_features.favorite_categories if user_features else []
        
        for product in filtered_products:
            score = 0.0
            
            # Category preference
            if product.category_id in favorite_cats:
                score += 5.0
            
            # Product quality
            score += product.rating * 2.0
            
            # Popularity
            score += min(product.review_count / 100.0, 2.0)
            
            # Featured products
            if product.is_featured:
                score += 3.0
            
            # Discount
            if product.discount_percentage > 0:
                score += product.discount_percentage / 20.0
            
            scored_products.append((product, score))
        
        # Sort by score
        scored_products.sort(key=lambda x: x[1], reverse=True)
        
        # Create recommendations
        recommendations = []
        for rank, (product, score) in enumerate(scored_products[:request.limit], 1):
            recommendations.append(
                RecommendationItem(
                    product_id=product.id,
                    product_name=product.name,
                    category_id=product.category_id,
                    category_name=product.category_name,
                    brand=product.brand,
                    price=product.price,
                    discount_price=product.discount_price,
                    discount_percentage=product.discount_percentage,
                    image_url=product.image_url,
                    rating=product.rating,
                    review_count=product.review_count,
                    confidence_score=min(score / 10.0, 1.0),
                    rank=rank,
                    reason="Popular product in this category"
                )
            )
        
        return recommendations
    
    async def process_feedback(self, feedback: FeedbackData):
        """
        Process feedback for online learning.
        
        Args:
            feedback: User feedback data
        """
        try:
            # Extract features for the feedback
            user_features = await self.feature_extractor.extract_user_features(
                feedback.user_id
            )
            
            context_features = self.feature_extractor.build_context_features(
                UserContext(user_id=feedback.user_id, **feedback.context)
            )
            
            state = np.array(
                user_features.feature_vector + context_features,
                dtype=np.float32
            )
            
            # Update model if available
            if self.model and feedback.selected_product:
                # Find product index
                # This is a simplified version - in production, maintain product index
                action = feedback.selected_product  # Placeholder
                
                if self.model_type in ["LINUCB", "THOMPSON_SAMPLING"]:
                    self.model.update(action, state, feedback.reward)
                    logger.info(
                        f"Updated model with feedback: "
                        f"user={feedback.user_id}, "
                        f"product={feedback.selected_product}, "
                        f"reward={feedback.reward}"
                    )
            
        except Exception as e:
            logger.error(f"Error processing feedback: {e}", exc_info=True)
    
    async def get_model_status(self) -> Dict[str, Any]:
        """Get current model status."""
        if self.model:
            return {
                "model_loaded": True,
                "model_type": self.model_type,
                "model_version": self.model_version,
                "total_iterations": getattr(self.model, "n_iterations", 0),
                "total_rewards": getattr(self.model, "total_rewards", 0.0)
            }
        else:
            return {
                "model_loaded": False,
                "message": "No model loaded. Train a model first."
            }
    
    async def get_metrics(self) -> RecommendationMetrics:
        """Get recommendation metrics."""
        # Placeholder implementation
        return RecommendationMetrics(
            total_recommendations=getattr(self.model, "n_iterations", 0),
            avg_confidence=0.75,
            diversity_score=0.65,
            coverage=0.80,
            novelty_score=0.55
        )

