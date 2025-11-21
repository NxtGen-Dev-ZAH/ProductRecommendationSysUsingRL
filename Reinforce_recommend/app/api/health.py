"""
Health check endpoints.
"""
from fastapi import APIRouter, status
from pydantic import BaseModel
from datetime import datetime
from typing import Dict, Any
import psutil
import os

from app.config import settings
from app.utils.database import get_db_status

router = APIRouter()


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    timestamp: datetime
    version: str
    algorithm: str
    database: str
    system: Dict[str, Any]


@router.get(
    "/health",
    response_model=HealthResponse,
    status_code=status.HTTP_200_OK,
    summary="Health Check",
    description="Check service health and status"
)
async def health_check():
    """
    Comprehensive health check endpoint.
    
    Returns:
        Service health status including database connectivity and system metrics
    """
    # Check database
    db_status = await get_db_status()
    
    # System metrics
    cpu_percent = psutil.cpu_percent(interval=1)
    memory = psutil.virtual_memory()
    disk = psutil.disk_usage('/')
    
    return HealthResponse(
        status="healthy" if db_status == "connected" else "degraded",
        timestamp=datetime.utcnow(),
        version="0.1.0",
        algorithm=settings.algorithm_type,
        database=db_status,
        system={
            "cpu_percent": cpu_percent,
            "memory_percent": memory.percent,
            "memory_available_gb": round(memory.available / (1024**3), 2),
            "disk_percent": disk.percent,
            "disk_free_gb": round(disk.free / (1024**3), 2)
        }
    )


@router.get(
    "/health/ready",
    status_code=status.HTTP_200_OK,
    summary="Readiness Check",
    description="Check if service is ready to serve requests"
)
async def readiness_check():
    """
    Kubernetes-style readiness probe.
    
    Returns:
        Simple ready status
    """
    db_status = await get_db_status()
    
    if db_status == "connected":
        return {"status": "ready"}
    else:
        return {"status": "not_ready", "reason": "database_disconnected"}


@router.get(
    "/health/live",
    status_code=status.HTTP_200_OK,
    summary="Liveness Check",
    description="Check if service is alive"
)
async def liveness_check():
    """
    Kubernetes-style liveness probe.
    
    Returns:
        Simple alive status
    """
    return {"status": "alive"}

