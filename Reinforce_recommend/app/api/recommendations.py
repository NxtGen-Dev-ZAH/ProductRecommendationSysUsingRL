"""
Recommendation endpoints.
"""
from fastapi import APIRouter, HTTPException, status, Depends
from loguru import logger
from typing import List

from app.models.recommendation import (
    RecommendationRequest,
    RecommendationResponse
)
from app.models.interaction import FeedbackData
from app.services.recommendation_service import RecommendationService
from app.config import settings

router = APIRouter()


def get_recommendation_service() -> RecommendationService:
    """Dependency injection for recommendation service."""
    return RecommendationService()


@router.post(
    "/recommendations/get",
    response_model=RecommendationResponse,
    status_code=status.HTTP_200_OK,
    summary="Get Product Recommendations",
    description="Get personalized product recommendations for a user"
)
async def get_recommendations(
    request: RecommendationRequest,
    service: RecommendationService = Depends(get_recommendation_service)
):
    """
    Get personalized product recommendations using RL algorithms.
    
    Args:
        request: Recommendation request with user_id and preferences
        service: Recommendation service instance
        
    Returns:
        List of recommended products with confidence scores
    """
    try:
        logger.info(f"Getting recommendations for user {request.user_id}")
        
        # Validate limit
        if request.limit > settings.max_recommendation_limit:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"Limit cannot exceed {settings.max_recommendation_limit}"
            )
        
        # Get recommendations
        recommendations = await service.get_recommendations(request)
        
        logger.info(
            f"Generated {len(recommendations.recommendations)} "
            f"recommendations for user {request.user_id}"
        )
        
        return recommendations
        
    except ValueError as e:
        logger.warning(f"Invalid request: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Error getting recommendations: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate recommendations"
        )


@router.post(
    "/recommendations/feedback",
    status_code=status.HTTP_200_OK,
    summary="Submit Recommendation Feedback",
    description="Submit feedback on recommendation performance for online learning"
)
async def submit_feedback(
    feedback: FeedbackData,
    service: RecommendationService = Depends(get_recommendation_service)
):
    """
    Submit feedback on recommendations for online learning.
    
    Args:
        feedback: Feedback data including interaction and reward
        service: Recommendation service instance
        
    Returns:
        Success confirmation
    """
    try:
        logger.info(f"Received feedback for user {feedback.user_id}")
        
        # Process feedback for online learning
        await service.process_feedback(feedback)
        
        return {
            "status": "success",
            "message": "Feedback received and processed",
            "user_id": feedback.user_id
        }
        
    except Exception as e:
        logger.error(f"Error processing feedback: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to process feedback"
        )


@router.get(
    "/recommendations/model/status",
    status_code=status.HTTP_200_OK,
    summary="Get Model Status",
    description="Get current RL model status and metadata"
)
async def get_model_status(
    service: RecommendationService = Depends(get_recommendation_service)
):
    """
    Get current model status and metadata.
    
    Args:
        service: Recommendation service instance
        
    Returns:
        Model status information
    """
    try:
        status_info = await service.get_model_status()
        return status_info
        
    except Exception as e:
        logger.error(f"Error getting model status: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get model status"
        )


@router.get(
    "/recommendations/metrics",
    status_code=status.HTTP_200_OK,
    summary="Get Recommendation Metrics",
    description="Get performance metrics for the recommendation system"
)
async def get_recommendation_metrics(
    service: RecommendationService = Depends(get_recommendation_service)
):
    """
    Get recommendation performance metrics.
    
    Args:
        service: Recommendation service instance
        
    Returns:
        Performance metrics
    """
    try:
        metrics = await service.get_metrics()
        return metrics
        
    except Exception as e:
        logger.error(f"Error getting metrics: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get metrics"
        )

