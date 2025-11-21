"""
Model training endpoints.
"""
from fastapi import APIRouter, HTTPException, status, BackgroundTasks, Depends
from pydantic import BaseModel, Field
from loguru import logger
from typing import Optional, Dict, Any
from datetime import datetime

from app.services.training_service import TrainingService
from app.config import settings

router = APIRouter()


class TrainingRequest(BaseModel):
    """Request to start model training."""
    algorithm_type: Optional[str] = Field(
        default=None,
        description="Algorithm to train (LINUCB, THOMPSON_SAMPLING, DQN)"
    )
    batch_size: Optional[int] = Field(default=None, ge=100, le=10000)
    epochs: Optional[int] = Field(default=None, ge=1, le=1000)
    learning_rate: Optional[float] = Field(default=None, gt=0.0, le=1.0)
    use_latest_data: bool = Field(
        default=True,
        description="Use latest interaction data"
    )
    days_lookback: int = Field(
        default=30,
        ge=1,
        le=365,
        description="Days of historical data to use"
    )


class TrainingResponse(BaseModel):
    """Response for training request."""
    status: str
    message: str
    training_id: str
    algorithm_type: str
    started_at: datetime


class EvaluationRequest(BaseModel):
    """Request to evaluate model."""
    model_id: Optional[int] = None
    algorithm_type: Optional[str] = None
    test_size: float = Field(default=0.2, ge=0.1, le=0.5)


def get_training_service() -> TrainingService:
    """Dependency injection for training service."""
    return TrainingService()


@router.post(
    "/training/train",
    response_model=TrainingResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Start Model Training",
    description="Trigger model training with specified configuration"
)
async def start_training(
    request: TrainingRequest,
    background_tasks: BackgroundTasks,
    service: TrainingService = Depends(get_training_service)
):
    """
    Start model training in the background.
    
    Args:
        request: Training configuration
        background_tasks: FastAPI background tasks
        service: Training service instance
        
    Returns:
        Training job information
    """
    try:
        # Use default algorithm if not specified
        algorithm_type = request.algorithm_type or settings.algorithm_type
        
        logger.info(f"Starting training for algorithm: {algorithm_type}")
        
        # Generate training ID
        training_id = f"train_{algorithm_type}_{int(datetime.utcnow().timestamp())}"
        
        # Start training in background
        background_tasks.add_task(
            service.train_model,
            algorithm_type=algorithm_type,
            training_id=training_id,
            batch_size=request.batch_size or settings.training_batch_size,
            epochs=request.epochs or settings.training_epochs,
            learning_rate=request.learning_rate or settings.learning_rate,
            days_lookback=request.days_lookback
        )
        
        return TrainingResponse(
            status="started",
            message=f"Training job {training_id} started in background",
            training_id=training_id,
            algorithm_type=algorithm_type,
            started_at=datetime.utcnow()
        )
        
    except ValueError as e:
        logger.warning(f"Invalid training request: {e}")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        logger.error(f"Error starting training: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to start training"
        )


@router.get(
    "/training/status",
    status_code=status.HTTP_200_OK,
    summary="Get Training Status",
    description="Get status of model training jobs"
)
async def get_training_status(
    training_id: Optional[str] = None,
    service: TrainingService = Depends(get_training_service)
):
    """
    Get training job status.
    
    Args:
        training_id: Optional specific training job ID
        service: Training service instance
        
    Returns:
        Training status information
    """
    try:
        if training_id:
            status_info = await service.get_training_status(training_id)
        else:
            status_info = await service.get_all_training_status()
            
        return status_info
        
    except Exception as e:
        logger.error(f"Error getting training status: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to get training status"
        )


@router.post(
    "/training/evaluate",
    status_code=status.HTTP_200_OK,
    summary="Evaluate Model",
    description="Evaluate model performance on test data"
)
async def evaluate_model(
    request: EvaluationRequest,
    service: TrainingService = Depends(get_training_service)
):
    """
    Evaluate model performance.
    
    Args:
        request: Evaluation configuration
        service: Training service instance
        
    Returns:
        Evaluation metrics
    """
    try:
        logger.info("Starting model evaluation")
        
        metrics = await service.evaluate_model(
            model_id=request.model_id,
            algorithm_type=request.algorithm_type,
            test_size=request.test_size
        )
        
        return {
            "status": "success",
            "metrics": metrics,
            "timestamp": datetime.utcnow()
        }
        
    except Exception as e:
        logger.error(f"Error evaluating model: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to evaluate model"
        )


@router.post(
    "/training/deploy",
    status_code=status.HTTP_200_OK,
    summary="Deploy Model",
    description="Deploy a trained model to production"
)
async def deploy_model(
    model_id: int,
    service: TrainingService = Depends(get_training_service)
):
    """
    Deploy a trained model to production.
    
    Args:
        model_id: ID of the model to deploy
        service: Training service instance
        
    Returns:
        Deployment confirmation
    """
    try:
        logger.info(f"Deploying model {model_id}")
        
        result = await service.deploy_model(model_id)
        
        return {
            "status": "success",
            "message": f"Model {model_id} deployed successfully",
            "model_info": result,
            "timestamp": datetime.utcnow()
        }
        
    except Exception as e:
        logger.error(f"Error deploying model: {e}", exc_info=True)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to deploy model"
        )

