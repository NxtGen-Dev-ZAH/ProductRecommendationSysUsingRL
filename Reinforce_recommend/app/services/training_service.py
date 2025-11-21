
import os
from typing import Optional, Dict, Any, List
from datetime import datetime
from loguru import logger
import numpy as np

from app.config import settings
from app.algorithms.contextual_bandit import LinUCB, ThompsonSampling
from app.services.data_service import DataService
from app.utils.feature_extraction import FeatureExtractor
from app.models.user import UserContext


class TrainingService:
    """Service for training RL models."""
    
    def __init__(self):
        self.data_service = DataService()
        self.feature_extractor = FeatureExtractor()
        self.training_jobs: Dict[str, Dict[str, Any]] = {}
    
    async def train_model(
        self,
        algorithm_type: str,
        training_id: str,
        batch_size: int,
        epochs: int,
        learning_rate: float,
        days_lookback: int
    ):
        """
        Train an RL model.
        
        Args:
            algorithm_type: Algorithm to train
            training_id: Unique training job ID
            batch_size: Batch size for training
            epochs: Number of epochs
            learning_rate: Learning rate
            days_lookback: Days of historical data
        """
        try:
            logger.info(f"Starting training job {training_id}")
            
            # Update job status
            self.training_jobs[training_id] = {
                "status": "running",
                "started_at": datetime.utcnow(),
                "algorithm": algorithm_type,
                "progress": 0.0
            }
            
            # Get training data
            logger.info("Fetching training data...")
            interactions = await self.data_service.get_all_interactions(
                days_lookback=days_lookback,
                min_interactions=settings.min_interactions_for_training
            )
            
            if len(interactions) < settings.min_interactions_for_training:
                error_msg = (
                    f"Insufficient training data: {len(interactions)} interactions "
                    f"(minimum: {settings.min_interactions_for_training})"
                )
                logger.error(error_msg)
                self.training_jobs[training_id]["status"] = "failed"
                self.training_jobs[training_id]["error"] = error_msg
                return
            
            logger.info(f"Loaded {len(interactions)} interactions for training")
            
            # Get all products
            products = await self.data_service.get_active_products()
            product_id_to_idx = {p.id: idx for idx, p in enumerate(products)}
            
            # Calculate feature dimensions
            feature_dim = (
                settings.user_feature_dim +
                settings.context_feature_dim
            )
            n_actions = len(products)
            
            # Initialize model
            logger.info(f"Initializing {algorithm_type} model...")
            if algorithm_type == "LINUCB":
                model = LinUCB(
                    n_features=feature_dim,
                    n_actions=n_actions,
                    alpha=settings.ucb_alpha
                )
            elif algorithm_type == "THOMPSON_SAMPLING":
                model = ThompsonSampling(
                    n_features=feature_dim,
                    n_actions=n_actions
                )
            else:
                raise ValueError(f"Unsupported algorithm: {algorithm_type}")
            
            # Training loop
            logger.info("Starting training...")
            total_reward = 0.0
            
            for epoch in range(epochs):
                epoch_reward = 0.0
                
                for i, interaction in enumerate(interactions):
                    # Extract features
                    try:
                        user_features = await self.feature_extractor.extract_user_features(
                            interaction.user_id
                        )
                        
                        context = UserContext(
                            user_id=interaction.user_id,
                            **(interaction.context or {})
                        )
                        context_features = self.feature_extractor.build_context_features(context)
                        
                        # Combine features
                        state = np.array(
                            user_features.feature_vector + context_features,
                            dtype=np.float32
                        )
                        
                        # Get action (product index)
                        if interaction.product_id in product_id_to_idx:
                            action = product_id_to_idx[interaction.product_id]
                            
                            # Update model
                            model.update(action, state, interaction.reward)
                            epoch_reward += interaction.reward
                        
                    except Exception as e:
                        logger.warning(f"Error processing interaction {i}: {e}")
                        continue
                    
                    # Update progress
                    progress = ((epoch * len(interactions) + i + 1) /
                               (epochs * len(interactions)))
                    self.training_jobs[training_id]["progress"] = progress
                
                total_reward += epoch_reward
                avg_reward = epoch_reward / len(interactions)
                logger.info(
                    f"Epoch {epoch + 1}/{epochs} - "
                    f"Avg Reward: {avg_reward:.4f}"
                )
            
            # Save model
            logger.info("Saving model...")
            model_path = os.path.join(
                settings.model_storage_path,
                f"{algorithm_type.lower()}_model.pkl"
            )
            os.makedirs(settings.model_storage_path, exist_ok=True)
            model.save(model_path)
            
            # Update job status
            self.training_jobs[training_id]["status"] = "completed"
            self.training_jobs[training_id]["completed_at"] = datetime.utcnow()
            self.training_jobs[training_id]["model_path"] = model_path
            self.training_jobs[training_id]["metrics"] = {
                "total_reward": float(total_reward),
                "avg_reward": float(total_reward / (epochs * len(interactions))),
                "n_interactions": len(interactions),
                "n_epochs": epochs
            }
            
            logger.info(f"Training job {training_id} completed successfully")
            
        except Exception as e:
            logger.error(f"Training job {training_id} failed: {e}", exc_info=True)
            self.training_jobs[training_id]["status"] = "failed"
            self.training_jobs[training_id]["error"] = str(e)
    
    async def get_training_status(self, training_id: str) -> Dict[str, Any]:
        """Get status of a training job."""
        if training_id in self.training_jobs:
            return self.training_jobs[training_id]
        else:
            return {"error": f"Training job {training_id} not found"}
    
    async def get_all_training_status(self) -> Dict[str, Any]:
        """Get status of all training jobs."""
        return {
            "jobs": self.training_jobs,
            "total_jobs": len(self.training_jobs)
        }
    
    async def evaluate_model(
        self,
        model_id: Optional[int] = None,
        algorithm_type: Optional[str] = None,
        test_size: float = 0.2
    ) -> Dict[str, Any]:
        """
        Evaluate model performance.
        
        Args:
            model_id: Model ID from database
            algorithm_type: Algorithm type
            test_size: Test set size
            
        Returns:
            Evaluation metrics
        """
        try:
            # Load model
            algo = algorithm_type or settings.algorithm_type
            model_path = os.path.join(
                settings.model_storage_path,
                f"{algo.lower()}_model.pkl"
            )
            
            if not os.path.exists(model_path):
                return {"error": "Model not found"}
            
            if algo == "LINUCB":
                model = LinUCB.load(model_path)
            elif algo == "THOMPSON_SAMPLING":
                model = ThompsonSampling.load(model_path)
            else:
                return {"error": f"Unsupported algorithm: {algo}"}
            
            # Get test data
            interactions = await self.data_service.get_all_interactions(
                days_lookback=30
            )
            
            # Split data
            split_idx = int(len(interactions) * (1 - test_size))
            test_interactions = interactions[split_idx:]
            
            # Evaluate
            total_reward = 0.0
            correct_predictions = 0
            
            products = await self.data_service.get_active_products()
            product_id_to_idx = {p.id: idx for idx, p in enumerate(products)}
            
            for interaction in test_interactions:
                try:
                    # Extract features
                    user_features = await self.feature_extractor.extract_user_features(
                        interaction.user_id
                    )
                    context = UserContext(
                        user_id=interaction.user_id,
                        **(interaction.context or {})
                    )
                    context_features = self.feature_extractor.build_context_features(context)
                    
                    state = np.array(
                        user_features.feature_vector + context_features,
                        dtype=np.float32
                    )
                    
                    # Predict action
                    if algo == "LINUCB":
                        predicted_action, _ = model.select_action(state)
                    else:
                        predicted_action, _ = model.select_action(state)
                    
                    # Check if correct
                    if interaction.product_id in product_id_to_idx:
                        actual_action = product_id_to_idx[interaction.product_id]
                        if predicted_action == actual_action:
                            correct_predictions += 1
                        total_reward += interaction.reward
                
                except Exception as e:
                    logger.warning(f"Error evaluating interaction: {e}")
                    continue
            
            # Calculate metrics
            accuracy = correct_predictions / len(test_interactions) if test_interactions else 0
            avg_reward = total_reward / len(test_interactions) if test_interactions else 0
            
            return {
                "algorithm": algo,
                "test_size": len(test_interactions),
                "accuracy": float(accuracy),
                "avg_reward": float(avg_reward),
                "total_reward": float(total_reward)
            }
            
        except Exception as e:
            logger.error(f"Error evaluating model: {e}", exc_info=True)
            return {"error": str(e)}
    
    async def deploy_model(self, model_id: int) -> Dict[str, Any]:
        """
        Deploy a trained model to production.
        
        Args:
            model_id: Model ID
            
        Returns:
            Deployment info
        """
        # Placeholder implementation
        logger.info(f"Deploying model {model_id}")
        return {
            "model_id": model_id,
            "status": "deployed",
            "deployed_at": datetime.utcnow()
        }

