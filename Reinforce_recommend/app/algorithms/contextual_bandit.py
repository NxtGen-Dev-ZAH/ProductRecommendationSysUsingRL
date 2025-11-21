"""
Contextual bandit algorithms: LinUCB and Thompson Sampling.
"""
import numpy as np
from typing import List, Dict, Any, Optional, Tuple
import pickle
import os
from loguru import logger
from scipy.stats import beta

from app.config import settings


class LinUCB:
    """
    Linear Upper Confidence Bound (LinUCB) algorithm.
    
    Balances exploration and exploitation using confidence bounds on reward estimates.
    """
    
    def __init__(
        self,
        n_features: int,
        n_actions: int,
        alpha: float = 1.0,
        lambda_reg: float = 1.0
    ):
        """
        Initialize LinUCB.
        
        Args:
            n_features: Feature dimension
            n_actions: Number of actions (products)
            alpha: Exploration parameter
            lambda_reg: Regularization parameter
        """
        self.n_features = n_features
        self.n_actions = n_actions
        self.alpha = alpha
        self.lambda_reg = lambda_reg
        
        # Initialize matrices for each action
        self.A = [np.identity(n_features) * lambda_reg for _ in range(n_actions)]
        self.b = [np.zeros((n_features, 1)) for _ in range(n_actions)]
        
        # Track statistics
        self.action_counts = np.zeros(n_actions)
        self.total_rewards = 0.0
        self.n_iterations = 0
        
        logger.info(
            f"Initialized LinUCB with n_features={n_features}, "
            f"n_actions={n_actions}, alpha={alpha}"
        )
    
    def select_action(
        self,
        context: np.ndarray,
        valid_actions: Optional[List[int]] = None
    ) -> Tuple[int, float]:
        """
        Select action using LinUCB algorithm.
        
        Args:
            context: Context feature vector
            valid_actions: List of valid action indices
            
        Returns:
            Selected action index and UCB score
        """
        if valid_actions is None:
            valid_actions = list(range(self.n_actions))
        
        context = context.reshape(-1, 1)
        ucb_scores = []
        
        for action in valid_actions:
            # Compute theta (reward estimate)
            A_inv = np.linalg.inv(self.A[action])
            theta = A_inv @ self.b[action]
            
            # Compute UCB
            uncertainty = np.sqrt(context.T @ A_inv @ context)
            ucb = (theta.T @ context) + self.alpha * uncertainty
            
            ucb_scores.append((action, float(ucb)))
        
        # Select action with highest UCB
        best_action, best_score = max(ucb_scores, key=lambda x: x[1])
        
        return best_action, best_score
    
    def update(
        self,
        action: int,
        context: np.ndarray,
        reward: float
    ):
        """
        Update model with observed reward.
        
        Args:
            action: Taken action
            context: Context features
            reward: Observed reward
        """
        context = context.reshape(-1, 1)
        
        # Update A and b for the action
        self.A[action] += context @ context.T
        self.b[action] += reward * context
        
        # Update statistics
        self.action_counts[action] += 1
        self.total_rewards += reward
        self.n_iterations += 1
    
    def get_expected_rewards(self, context: np.ndarray) -> np.ndarray:
        """
        Get expected rewards for all actions.
        
        Args:
            context: Context features
            
        Returns:
            Expected rewards for each action
        """
        context = context.reshape(-1, 1)
        rewards = np.zeros(self.n_actions)
        
        for action in range(self.n_actions):
            A_inv = np.linalg.inv(self.A[action])
            theta = A_inv @ self.b[action]
            rewards[action] = float(theta.T @ context)
        
        return rewards
    
    def save(self, path: str):
        """Save model to disk."""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        
        model_data = {
            "n_features": self.n_features,
            "n_actions": self.n_actions,
            "alpha": self.alpha,
            "lambda_reg": self.lambda_reg,
            "A": self.A,
            "b": self.b,
            "action_counts": self.action_counts,
            "total_rewards": self.total_rewards,
            "n_iterations": self.n_iterations
        }
        
        with open(path, "wb") as f:
            pickle.dump(model_data, f)
        
        logger.info(f"LinUCB model saved to {path}")
    
    @classmethod
    def load(cls, path: str) -> "LinUCB":
        """Load model from disk."""
        with open(path, "rb") as f:
            model_data = pickle.load(f)
        
        model = cls(
            n_features=model_data["n_features"],
            n_actions=model_data["n_actions"],
            alpha=model_data["alpha"],
            lambda_reg=model_data["lambda_reg"]
        )
        
        model.A = model_data["A"]
        model.b = model_data["b"]
        model.action_counts = model_data["action_counts"]
        model.total_rewards = model_data["total_rewards"]
        model.n_iterations = model_data["n_iterations"]
        
        logger.info(f"LinUCB model loaded from {path}")
        return model


class ThompsonSampling:
    """
    Thompson Sampling for contextual bandits.
    
    Uses Bayesian approach with posterior sampling for exploration.
    """
    
    def __init__(
        self,
        n_features: int,
        n_actions: int,
        lambda_reg: float = 1.0,
        nu: float = 1.0
    ):
        """
        Initialize Thompson Sampling.
        
        Args:
            n_features: Feature dimension
            n_actions: Number of actions
            lambda_reg: Regularization parameter
            nu: Noise parameter
        """
        self.n_features = n_features
        self.n_actions = n_actions
        self.lambda_reg = lambda_reg
        self.nu = nu
        
        # Initialize parameters
        self.B = [np.identity(n_features) * lambda_reg for _ in range(n_actions)]
        self.mu = [np.zeros((n_features, 1)) for _ in range(n_actions)]
        self.f = [np.zeros((n_features, 1)) for _ in range(n_actions)]
        
        # Track statistics
        self.action_counts = np.zeros(n_actions)
        self.total_rewards = 0.0
        self.n_iterations = 0
        
        logger.info(
            f"Initialized Thompson Sampling with n_features={n_features}, "
            f"n_actions={n_actions}"
        )
    
    def select_action(
        self,
        context: np.ndarray,
        valid_actions: Optional[List[int]] = None
    ) -> Tuple[int, float]:
        """
        Select action using Thompson Sampling.
        
        Args:
            context: Context feature vector
            valid_actions: List of valid action indices
            
        Returns:
            Selected action index and sampled value
        """
        if valid_actions is None:
            valid_actions = list(range(self.n_actions))
        
        context = context.reshape(-1, 1)
        sampled_values = []
        
        for action in valid_actions:
            # Sample from posterior distribution
            B_inv = np.linalg.inv(self.B[action])
            mu_hat = B_inv @ self.f[action]
            
            # Sample theta from multivariate normal
            theta_sample = np.random.multivariate_normal(
                mu_hat.flatten(),
                self.nu * B_inv
            ).reshape(-1, 1)
            
            # Compute expected reward with sampled theta
            value = float(theta_sample.T @ context)
            sampled_values.append((action, value))
        
        # Select action with highest sampled value
        best_action, best_value = max(sampled_values, key=lambda x: x[1])
        
        return best_action, best_value
    
    def update(
        self,
        action: int,
        context: np.ndarray,
        reward: float
    ):
        """
        Update model with observed reward.
        
        Args:
            action: Taken action
            context: Context features
            reward: Observed reward
        """
        context = context.reshape(-1, 1)
        
        # Update B and f
        self.B[action] += context @ context.T
        self.f[action] += reward * context
        
        # Update mu
        self.mu[action] = np.linalg.inv(self.B[action]) @ self.f[action]
        
        # Update statistics
        self.action_counts[action] += 1
        self.total_rewards += reward
        self.n_iterations += 1
    
    def get_expected_rewards(self, context: np.ndarray) -> np.ndarray:
        """
        Get expected rewards for all actions.
        
        Args:
            context: Context features
            
        Returns:
            Expected rewards for each action
        """
        context = context.reshape(-1, 1)
        rewards = np.zeros(self.n_actions)
        
        for action in range(self.n_actions):
            rewards[action] = float(self.mu[action].T @ context)
        
        return rewards
    
    def save(self, path: str):
        """Save model to disk."""
        os.makedirs(os.path.dirname(path), exist_ok=True)
        
        model_data = {
            "n_features": self.n_features,
            "n_actions": self.n_actions,
            "lambda_reg": self.lambda_reg,
            "nu": self.nu,
            "B": self.B,
            "mu": self.mu,
            "f": self.f,
            "action_counts": self.action_counts,
            "total_rewards": self.total_rewards,
            "n_iterations": self.n_iterations
        }
        
        with open(path, "wb") as f:
            pickle.dump(model_data, f)
        
        logger.info(f"Thompson Sampling model saved to {path}")
    
    @classmethod
    def load(cls, path: str) -> "ThompsonSampling":
        """Load model from disk."""
        with open(path, "rb") as f:
            model_data = pickle.load(f)
        
        model = cls(
            n_features=model_data["n_features"],
            n_actions=model_data["n_actions"],
            lambda_reg=model_data["lambda_reg"],
            nu=model_data["nu"]
        )
        
        model.B = model_data["B"]
        model.mu = model_data["mu"]
        model.f = model_data["f"]
        model.action_counts = model_data["action_counts"]
        model.total_rewards = model_data["total_rewards"]
        model.n_iterations = model_data["n_iterations"]
        
        logger.info(f"Thompson Sampling model loaded from {path}")
        return model

