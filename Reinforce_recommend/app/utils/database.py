"""
Database connection and utilities.
"""
from sqlalchemy import create_engine, text, MetaData
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import declarative_base, sessionmaker
from loguru import logger
from typing import Optional

from app.config import settings

# Database engine and session
engine: Optional[create_engine] = None
async_engine: Optional[create_async_engine] = None
SessionLocal: Optional[sessionmaker] = None
AsyncSessionLocal: Optional[async_sessionmaker] = None

# Base class for models
Base = declarative_base()
metadata = MetaData()


async def init_db():
    """Initialize database connection."""
    global engine, async_engine, SessionLocal, AsyncSessionLocal
    
    try:
        # Create synchronous engine (for migrations and simple queries)
        engine = create_engine(
            settings.database_url,
            pool_size=settings.database_pool_size,
            max_overflow=settings.database_max_overflow,
            pool_pre_ping=True,
            echo=False
        )
        
        # Create session factory
        SessionLocal = sessionmaker(
            autocommit=False,
            autoflush=False,
            bind=engine
        )
        
        # Test connection
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
            
        logger.info("Database connection initialized successfully")
        
    except Exception as e:
        logger.error(f"Failed to initialize database: {e}")
        raise


async def close_db():
    """Close database connections."""
    global engine, async_engine
    
    try:
        if engine:
            engine.dispose()
            logger.info("Database engine disposed")
            
        if async_engine:
            await async_engine.dispose()
            logger.info("Async database engine disposed")
            
    except Exception as e:
        logger.error(f"Error closing database: {e}")


async def get_db_status() -> str:
    """
    Check database connection status.
    
    Returns:
        Connection status string
    """
    try:
        if engine is None:
            return "not_initialized"
            
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
            return "connected"
            
    except Exception as e:
        logger.error(f"Database health check failed: {e}")
        return "disconnected"


def get_db():
    """
    Get database session.
    
    Yields:
        Database session
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


async def get_async_db():
    """
    Get async database session.
    
    Yields:
        Async database session
    """
    async with AsyncSessionLocal() as session:
        try:
            yield session
        finally:
            await session.close()


def execute_query(query: str, params: Optional[dict] = None):
    """
    Execute a SQL query.
    
    Args:
        query: SQL query string
        params: Query parameters
        
    Returns:
        Query results
    """
    try:
        with engine.connect() as conn:
            result = conn.execute(text(query), params or {})
            return result.fetchall()
    except Exception as e:
        logger.error(f"Error executing query: {e}")
        raise


def execute_query_dict(query: str, params: Optional[dict] = None):
    """
    Execute a SQL query and return results as dictionaries.
    
    Args:
        query: SQL query string
        params: Query parameters
        
    Returns:
        List of dictionaries
    """
    try:
        with engine.connect() as conn:
            result = conn.execute(text(query), params or {})
            columns = result.keys()
            return [dict(zip(columns, row)) for row in result.fetchall()]
    except Exception as e:
        logger.error(f"Error executing query: {e}")
        raise

