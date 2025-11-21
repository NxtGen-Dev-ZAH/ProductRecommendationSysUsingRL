"""
Feature extraction utilities for RL models.
"""
import numpy as np
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime, timedelta
from loguru import logger

from app.config import settings
from app.models.user import UserFeatures, UserContext
from app.models.product import ProductFeatures
from app.utils.database import execute_query_dict


class FeatureExtractor:
    """Extract and process features for RL models."""
    
    def __init__(self):
        self.user_feature_dim = settings.user_feature_dim
        self.product_feature_dim = settings.product_feature_dim
        self.context_feature_dim = settings.context_feature_dim
    
    async def extract_user_features(self, user_id: int) -> UserFeatures:
        """
        Extract user feature vector.
        
        Args:
            user_id: User ID
            
        Returns:
            User features
        """
        try:
            # Get user interaction history
            query = """
                SELECT 
                    COUNT(*) as total_interactions,
                    SUM(CASE WHEN interaction_type = 'VIEW' THEN 1 ELSE 0 END) as views,
                    SUM(CASE WHEN interaction_type = 'CLICK' THEN 1 ELSE 0 END) as clicks,
                    SUM(CASE WHEN interaction_type = 'CART_ADD' THEN 1 ELSE 0 END) as cart_adds,
                    SUM(CASE WHEN interaction_type = 'PURCHASE' THEN 1 ELSE 0 END) as purchases,
                    MAX(timestamp) as last_interaction
                FROM user_product_interactions
                WHERE user_id = :user_id
                AND timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY)
            """
            
            interaction_stats = execute_query_dict(query, {"user_id": user_id})
            
            # Get user purchase statistics
            purchase_query = """
                SELECT 
                    COUNT(*) as order_count,
                    AVG(total_amount) as avg_order_value,
                    SUM(total_amount) as total_spent,
                    DATEDIFF(NOW(), MAX(created_at)) as days_since_last_purchase
                FROM orders
                WHERE user_id = :user_id
                AND status = 'DELIVERED'
            """
            
            purchase_stats = execute_query_dict(purchase_query, {"user_id": user_id})
            
            # Get favorite categories
            category_query = """
                SELECT 
                    p.category_id,
                    COUNT(*) as interaction_count
                FROM user_product_interactions upi
                JOIN product p ON upi.product_id = p.id
                WHERE upi.user_id = :user_id
                AND upi.timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY)
                GROUP BY p.category_id
                ORDER BY interaction_count DESC
                LIMIT 5
            """
            
            favorite_categories = execute_query_dict(category_query, {"user_id": user_id})
            
            # Build feature vector
            feature_vector = self._build_user_feature_vector(
                interaction_stats[0] if interaction_stats else {},
                purchase_stats[0] if purchase_stats else {},
                favorite_categories
            )
            
            return UserFeatures(
                user_id=user_id,
                feature_vector=feature_vector,
                purchase_history_count=purchase_stats[0].get("order_count", 0) if purchase_stats else 0,
                avg_order_value=float(purchase_stats[0].get("avg_order_value", 0.0) or 0.0) if purchase_stats else 0.0,
                favorite_categories=[cat["category_id"] for cat in favorite_categories],
                last_purchase_days=purchase_stats[0].get("days_since_last_purchase") if purchase_stats else None,
                total_spent=float(purchase_stats[0].get("total_spent", 0.0) or 0.0) if purchase_stats else 0.0
            )
            
        except Exception as e:
            logger.error(f"Error extracting user features: {e}")
            # Return default features for cold start
            return UserFeatures(
                user_id=user_id,
                feature_vector=[0.0] * self.user_feature_dim
            )
    
    def _build_user_feature_vector(
        self,
        interaction_stats: Dict[str, Any],
        purchase_stats: Dict[str, Any],
        favorite_categories: List[Dict[str, Any]]
    ) -> List[float]:
        """Build user feature vector from statistics."""
        features = []
        
        # Interaction features (normalized)
        total_interactions = interaction_stats.get("total_interactions", 0)
        features.extend([
            min(total_interactions / 1000.0, 1.0),  # Normalized interaction count
            interaction_stats.get("views", 0) / max(total_interactions, 1),
            interaction_stats.get("clicks", 0) / max(total_interactions, 1),
            interaction_stats.get("cart_adds", 0) / max(total_interactions, 1),
            interaction_stats.get("purchases", 0) / max(total_interactions, 1)
        ])
        
        # Purchase features (normalized)
        avg_order_value = float(purchase_stats.get("avg_order_value", 0.0) or 0.0)
        total_spent = float(purchase_stats.get("total_spent", 0.0) or 0.0)
        features.extend([
            min(purchase_stats.get("order_count", 0) / 100.0, 1.0),
            min(avg_order_value / 1000.0, 1.0),
            min(total_spent / 10000.0, 1.0)
        ])
        
        # Recency feature
        days_since_last = purchase_stats.get("days_since_last_purchase", 365)
        features.append(1.0 - min(days_since_last / 365.0, 1.0))
        
        # Category preferences (one-hot encoded top 5 categories)
        category_features = [0.0] * 10  # Support up to 10 categories
        for i, cat_info in enumerate(favorite_categories[:5]):
            if i < 10:
                category_features[i] = 1.0
        features.extend(category_features)
        
        # Pad or truncate to desired dimension
        if len(features) < self.user_feature_dim:
            features.extend([0.0] * (self.user_feature_dim - len(features)))
        else:
            features = features[:self.user_feature_dim]
        
        return features
    
    async def extract_product_features(self, product_id: int) -> ProductFeatures:
        """
        Extract product feature vector.
        
        Args:
            product_id: Product ID
            
        Returns:
            Product features
        """
        try:
            # Get product information
            query = """
                SELECT 
                    p.id,
                    p.category_id,
                    p.brand,
                    p.price,
                    p.discount_percentage,
                    COALESCE(AVG(r.rating), 0) as rating,
                    COUNT(DISTINCT r.id) as review_count,
                    p.stock_quantity,
                    p.is_featured,
                    DATEDIFF(NOW(), p.created_at) as days_old,
                    COUNT(DISTINCT upi.id) as view_count,
                    SUM(CASE WHEN upi.interaction_type = 'PURCHASE' THEN 1 ELSE 0 END) as purchase_count
                FROM product p
                LEFT JOIN review r ON p.id = r.product_id
                LEFT JOIN user_product_interactions upi ON p.id = upi.product_id
                    AND upi.timestamp >= DATE_SUB(NOW(), INTERVAL 90 DAY)
                WHERE p.id = :product_id
                GROUP BY p.id
            """
            
            product_data = execute_query_dict(query, {"product_id": product_id})
            
            if not product_data:
                raise ValueError(f"Product {product_id} not found")
            
            product = product_data[0]
            
            # Build feature vector
            feature_vector = self._build_product_feature_vector(product)
            
            return ProductFeatures(
                product_id=product_id,
                feature_vector=feature_vector,
                category_id=product["category_id"],
                brand=product.get("brand"),
                price=float(product["price"]),
                discount_percentage=float(product.get("discount_percentage", 0.0)),
                rating=float(product.get("rating", 0.0)),
                review_count=product.get("review_count", 0),
                stock_quantity=product.get("stock_quantity", 0),
                is_featured=bool(product.get("is_featured", False)),
                is_new_arrival=product.get("days_old", 365) < 30,
                view_count=product.get("view_count", 0),
                purchase_count=product.get("purchase_count", 0)
            )
            
        except Exception as e:
            logger.error(f"Error extracting product features: {e}")
            raise
    
    def _build_product_feature_vector(self, product: Dict[str, Any]) -> List[float]:
        """Build product feature vector from product data."""
        features = []
        
        # Price features (normalized)
        price = float(product.get("price", 0.0))
        discount = float(product.get("discount_percentage", 0.0))
        features.extend([
            min(price / 1000.0, 1.0),  # Normalized price
            discount / 100.0,  # Discount percentage
            1.0 if discount > 0 else 0.0  # Has discount flag
        ])
        
        # Quality features
        rating = float(product.get("rating", 0.0))
        review_count = product.get("review_count", 0)
        features.extend([
            rating / 5.0,  # Normalized rating
            min(review_count / 100.0, 1.0),  # Normalized review count
            1.0 if review_count >= 10 else 0.0  # Has sufficient reviews
        ])
        
        # Popularity features
        view_count = product.get("view_count", 0)
        purchase_count = product.get("purchase_count", 0)
        features.extend([
            min(view_count / 1000.0, 1.0),
            min(purchase_count / 100.0, 1.0),
            purchase_count / max(view_count, 1)  # Conversion rate
        ])
        
        # Inventory and status
        stock = product.get("stock_quantity", 0)
        features.extend([
            1.0 if stock > 0 else 0.0,  # In stock
            min(stock / 100.0, 1.0),  # Stock level
            1.0 if product.get("is_featured", False) else 0.0,
            1.0 if product.get("days_old", 365) < 30 else 0.0  # New arrival
        ])
        
        # Category encoding (placeholder - can be enhanced with embeddings)
        category_id = product.get("category_id", 0)
        category_features = [0.0] * 20  # Support up to 20 categories
        if category_id < 20:
            category_features[category_id] = 1.0
        features.extend(category_features)
        
        # Pad or truncate to desired dimension
        if len(features) < self.product_feature_dim:
            features.extend([0.0] * (self.product_feature_dim - len(features)))
        else:
            features = features[:self.product_feature_dim]
        
        return features
    
    def build_context_features(self, context: UserContext) -> List[float]:
        """
        Build context feature vector.
        
        Args:
            context: User context
            
        Returns:
            Context feature vector
        """
        features = []
        
        # Category context
        if context.current_category:
            features.append(1.0)
            features.append(float(context.current_category) / 20.0)
        else:
            features.extend([0.0, 0.0])
        
        # Price range context
        if context.price_range_min is not None and context.price_range_max is not None:
            features.extend([
                1.0,  # Has price filter
                min(context.price_range_min / 1000.0, 1.0),
                min(context.price_range_max / 1000.0, 1.0)
            ])
        else:
            features.extend([0.0, 0.0, 0.0])
        
        # Recent activity
        features.append(min(len(context.recent_views) / 10.0, 1.0))
        features.append(min(len(context.recent_searches) / 5.0, 1.0))
        
        # Time context
        if context.time_of_day:
            hour = datetime.now().hour
            features.extend([
                np.sin(2 * np.pi * hour / 24),  # Cyclical time encoding
                np.cos(2 * np.pi * hour / 24)
            ])
        else:
            features.extend([0.0, 0.0])
        
        # Day of week
        if context.day_of_week is not None:
            features.extend([
                np.sin(2 * np.pi * context.day_of_week / 7),
                np.cos(2 * np.pi * context.day_of_week / 7)
            ])
        else:
            features.extend([0.0, 0.0])
        
        # Device type (one-hot)
        device_features = [0.0, 0.0, 0.0]  # mobile, tablet, desktop
        if context.device_type == "mobile":
            device_features[0] = 1.0
        elif context.device_type == "tablet":
            device_features[1] = 1.0
        elif context.device_type == "desktop":
            device_features[2] = 1.0
        features.extend(device_features)
        
        # Pad or truncate
        if len(features) < self.context_feature_dim:
            features.extend([0.0] * (self.context_feature_dim - len(features)))
        else:
            features = features[:self.context_feature_dim]
        
        return features
    
    def combine_features(
        self,
        user_features: List[float],
        product_features: List[float],
        context_features: Optional[List[float]] = None
    ) -> np.ndarray:
        """
        Combine user, product, and context features.
        
        Args:
            user_features: User feature vector
            product_features: Product feature vector
            context_features: Optional context feature vector
            
        Returns:
            Combined feature vector
        """
        combined = user_features + product_features
        
        if context_features:
            combined += context_features
        
        return np.array(combined, dtype=np.float32)

