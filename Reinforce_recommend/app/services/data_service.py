"""
Data service for fetching and processing data.
"""
from typing import List, Dict, Any, Optional, Tuple
from datetime import datetime, timedelta
from loguru import logger

from app.utils.database import execute_query_dict
from app.models.product import Product
from app.models.interaction import InteractionLog, InteractionType


class DataService:
    """Service for data operations."""
    
    async def get_active_products(
        self,
        category_id: Optional[int] = None,
        limit: Optional[int] = None
    ) -> List[Product]:
        """
        Get active products from database.
        
        Args:
            category_id: Optional category filter
            limit: Optional result limit
            
        Returns:
            List of active products
        """
        try:
            query = """
                SELECT 
                    p.id,
                    p.name,
                    p.description,
                    p.category_id,
                    c.name as category_name,
                    p.brand,
                    p.price,
                    p.discount_price,
                    p.discount_percentage,
                    p.image_url,
                    COALESCE(AVG(r.rating), 0) as rating,
                    COUNT(DISTINCT r.id) as review_count,
                    p.stock_quantity,
                    p.is_active,
                    p.is_featured,
                    p.created_at,
                    p.updated_at
                FROM product p
                LEFT JOIN category c ON p.category_id = c.id
                LEFT JOIN review r ON p.id = r.product_id
                WHERE p.is_active = 1
                AND p.stock_quantity > 0
            """
            
            params = {}
            
            if category_id:
                query += " AND p.category_id = :category_id"
                params["category_id"] = category_id
            
            query += " GROUP BY p.id ORDER BY p.created_at DESC"
            
            if limit:
                query += f" LIMIT {limit}"
            
            results = execute_query_dict(query, params)
            
            products = []
            for row in results:
                products.append(Product(
                    id=row["id"],
                    name=row["name"],
                    description=row.get("description"),
                    category_id=row["category_id"],
                    category_name=row.get("category_name"),
                    brand=row.get("brand"),
                    price=float(row["price"]),
                    discount_price=float(row["discount_price"]) if row.get("discount_price") else None,
                    discount_percentage=float(row.get("discount_percentage", 0.0)),
                    image_url=row.get("image_url"),
                    rating=float(row.get("rating", 0.0)),
                    review_count=row.get("review_count", 0),
                    stock_quantity=row.get("stock_quantity", 0),
                    is_active=bool(row.get("is_active", True)),
                    is_featured=bool(row.get("is_featured", False)),
                    created_at=row.get("created_at"),
                    updated_at=row.get("updated_at")
                ))
            
            return products
            
        except Exception as e:
            logger.error(f"Error fetching active products: {e}")
            return []
    
    async def get_user_interactions(
        self,
        user_id: int,
        days_lookback: int = 30,
        limit: Optional[int] = None
    ) -> List[InteractionLog]:
        """
        Get user interaction history.
        
        Args:
            user_id: User ID
            days_lookback: Number of days to look back
            limit: Optional result limit
            
        Returns:
            List of user interactions
        """
        try:
            query = """
                SELECT 
                    id,
                    user_id,
                    product_id,
                    interaction_type,
                    session_id,
                    context,
                    reward,
                    timestamp
                FROM user_product_interactions
                WHERE user_id = :user_id
                AND timestamp >= DATE_SUB(NOW(), INTERVAL :days DAY)
                ORDER BY timestamp DESC
            """
            
            params = {
                "user_id": user_id,
                "days": days_lookback
            }
            
            if limit:
                query += f" LIMIT {limit}"
            
            results = execute_query_dict(query, params)
            
            interactions = []
            for row in results:
                interactions.append(InteractionLog(
                    id=row["id"],
                    user_id=row["user_id"],
                    product_id=row["product_id"],
                    interaction_type=InteractionType(row["interaction_type"]),
                    session_id=row.get("session_id"),
                    context=row.get("context", {}),
                    reward=float(row.get("reward", 0.0)),
                    timestamp=row["timestamp"]
                ))
            
            return interactions
            
        except Exception as e:
            logger.error(f"Error fetching user interactions: {e}")
            return []
    
    async def get_all_interactions(
        self,
        days_lookback: int = 30,
        min_interactions: int = 1
    ) -> List[InteractionLog]:
        """
        Get all interactions for training.
        
        Args:
            days_lookback: Number of days to look back
            min_interactions: Minimum interactions per user
            
        Returns:
            List of all interactions
        """
        try:
            query = """
                SELECT 
                    upi.id,
                    upi.user_id,
                    upi.product_id,
                    upi.interaction_type,
                    upi.session_id,
                    upi.context,
                    upi.reward,
                    upi.timestamp
                FROM user_product_interactions upi
                INNER JOIN (
                    SELECT user_id
                    FROM user_product_interactions
                    WHERE timestamp >= DATE_SUB(NOW(), INTERVAL :days DAY)
                    GROUP BY user_id
                    HAVING COUNT(*) >= :min_interactions
                ) active_users ON upi.user_id = active_users.user_id
                WHERE upi.timestamp >= DATE_SUB(NOW(), INTERVAL :days DAY)
                ORDER BY upi.timestamp DESC
            """
            
            params = {
                "days": days_lookback,
                "min_interactions": min_interactions
            }
            
            results = execute_query_dict(query, params)
            
            interactions = []
            for row in results:
                interactions.append(InteractionLog(
                    id=row["id"],
                    user_id=row["user_id"],
                    product_id=row["product_id"],
                    interaction_type=InteractionType(row["interaction_type"]),
                    session_id=row.get("session_id"),
                    context=row.get("context", {}),
                    reward=float(row.get("reward", 0.0)),
                    timestamp=row["timestamp"]
                ))
            
            logger.info(f"Fetched {len(interactions)} interactions for training")
            return interactions
            
        except Exception as e:
            logger.error(f"Error fetching all interactions: {e}")
            return []
    
    async def get_product_by_id(self, product_id: int) -> Optional[Product]:
        """
        Get product by ID.
        
        Args:
            product_id: Product ID
            
        Returns:
            Product or None
        """
        try:
            products = await self.get_active_products(limit=1)
            # For now, query all and filter - can be optimized
            query = """
                SELECT 
                    p.id,
                    p.name,
                    p.description,
                    p.category_id,
                    c.name as category_name,
                    p.brand,
                    p.price,
                    p.discount_price,
                    p.discount_percentage,
                    p.image_url,
                    COALESCE(AVG(r.rating), 0) as rating,
                    COUNT(DISTINCT r.id) as review_count,
                    p.stock_quantity,
                    p.is_active,
                    p.is_featured,
                    p.created_at,
                    p.updated_at
                FROM product p
                LEFT JOIN category c ON p.category_id = c.id
                LEFT JOIN review r ON p.id = r.product_id
                WHERE p.id = :product_id
                GROUP BY p.id
            """
            
            results = execute_query_dict(query, {"product_id": product_id})
            
            if not results:
                return None
            
            row = results[0]
            return Product(
                id=row["id"],
                name=row["name"],
                description=row.get("description"),
                category_id=row["category_id"],
                category_name=row.get("category_name"),
                brand=row.get("brand"),
                price=float(row["price"]),
                discount_price=float(row["discount_price"]) if row.get("discount_price") else None,
                discount_percentage=float(row.get("discount_percentage", 0.0)),
                image_url=row.get("image_url"),
                rating=float(row.get("rating", 0.0)),
                review_count=row.get("review_count", 0),
                stock_quantity=row.get("stock_quantity", 0),
                is_active=bool(row.get("is_active", True)),
                is_featured=bool(row.get("is_featured", False)),
                created_at=row.get("created_at"),
                updated_at=row.get("updated_at")
            )
            
        except Exception as e:
            logger.error(f"Error fetching product {product_id}: {e}")
            return None

