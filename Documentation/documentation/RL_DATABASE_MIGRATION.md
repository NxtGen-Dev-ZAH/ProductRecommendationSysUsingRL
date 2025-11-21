# RL Recommendation System - Database Migration

## Overview

This document outlines the database schema changes required for the RL recommendation system integration.

**⚠️ IMPORTANT**: These changes are for PR review and should be executed in a controlled manner on production.

## Migration Version

- **Version**: 1.3
- **Date**: 2024-11-20
- **Author**: Development Team
- **Type**: Schema Extension

## New Tables

### 1. user_product_interactions

Tracks all user-product interactions for RL training and analysis.

```sql
CREATE TABLE user_product_interactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL COMMENT 'CLICK, VIEW, CART_ADD, PURCHASE, WISHLIST, REMOVE_CART, REMOVE_WISHLIST',
    session_id VARCHAR(255),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    context JSON COMMENT 'Additional context: category, price range, device, etc.',
    reward DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Calculated reward for RL',
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_product_timestamp (product_id, timestamp),
    INDEX idx_interaction_type (interaction_type),
    INDEX idx_session (session_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User-product interaction tracking for RL recommendation system';
```

**Interaction Types**:
- `VIEW`: User viewed product (reward: 0.1)
- `CLICK`: User clicked on product (reward: 0.5)
- `CART_ADD`: User added to cart (reward: 2.0)
- `PURCHASE`: User purchased product (reward: 10.0)
- `WISHLIST`: User added to wishlist (reward: 1.0)
- `REMOVE_CART`: User removed from cart (reward: -1.0)
- `REMOVE_WISHLIST`: User removed from wishlist (reward: -0.5)

**Context JSON Example**:
```json
{
  "category_id": 5,
  "price_range": {"min": 10.0, "max": 100.0},
  "device_type": "mobile",
  "referrer": "search",
  "position": 3,
  "page": "homepage"
}
```

### 2. rl_model_metadata

Stores metadata about trained RL models.

```sql
CREATE TABLE rl_model_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    model_name VARCHAR(100) NOT NULL,
    algorithm_type VARCHAR(50) NOT NULL COMMENT 'LINUCB, THOMPSON_SAMPLING, DQN',
    version VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    training_date DATETIME,
    performance_metrics JSON COMMENT 'CTR, conversion rate, avg reward, etc.',
    model_path VARCHAR(500) COMMENT 'Path to model file',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT COMMENT 'User who triggered training',
    
    UNIQUE KEY unique_model_version (model_name, version),
    INDEX idx_algorithm_active (algorithm_type, is_active),
    INDEX idx_training_date (training_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='RL model metadata and version tracking';
```

**Performance Metrics JSON Example**:
```json
{
  "ctr": 0.045,
  "conversion_rate": 0.012,
  "avg_reward": 1.25,
  "total_interactions": 50000,
  "training_duration_seconds": 3600,
  "accuracy": 0.68,
  "diversity_score": 0.72
}
```

### 3. user_context_features

Stores precomputed user feature vectors for faster inference.

```sql
CREATE TABLE user_context_features (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    feature_vector JSON NOT NULL COMMENT 'Precomputed user features for RL',
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_last_updated (last_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Precomputed user feature vectors for RL models';
```

**Feature Vector JSON Example**:
```json
{
  "demographics": {
    "age_group": "25-34",
    "location": "US"
  },
  "purchase_history": {
    "total_orders": 15,
    "avg_order_value": 85.50,
    "total_spent": 1282.50,
    "favorite_categories": [5, 12, 8]
  },
  "engagement": {
    "views_count": 250,
    "clicks_count": 120,
    "cart_adds_count": 30,
    "wishlist_count": 12
  },
  "recency": {
    "last_purchase_days": 14,
    "last_visit_days": 2
  },
  "feature_vector": [0.75, 0.62, 0.85, ...] // 50-dim vector
}
```

## Migration Scripts

### Development Environment

```sql
-- Run in development database

-- Drop tables if they exist (development only!)
DROP TABLE IF EXISTS user_product_interactions;
DROP TABLE IF EXISTS rl_model_metadata;
DROP TABLE IF EXISTS user_context_features;

-- Create new tables
SOURCE create_rl_tables.sql;

-- Verify tables
SHOW TABLES LIKE '%interactions%';
SHOW TABLES LIKE 'rl_%';
SHOW TABLES LIKE '%context_features%';

-- Check table structures
DESCRIBE user_product_interactions;
DESCRIBE rl_model_metadata;
DESCRIBE user_context_features;
```

### Production Environment

```sql
-- Step 1: Create tables (non-destructive)
-- Execute during maintenance window

CREATE TABLE IF NOT EXISTS user_product_interactions (
    -- ... (full definition as above)
);

CREATE TABLE IF NOT EXISTS rl_model_metadata (
    -- ... (full definition as above)
);

CREATE TABLE IF NOT EXISTS user_context_features (
    -- ... (full definition as above)
);

-- Step 2: Verify table creation
SELECT 
    TABLE_NAME, 
    TABLE_ROWS, 
    CREATE_TIME,
    TABLE_COLLATION
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'ecommercedb'
AND TABLE_NAME IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features');

-- Step 3: Grant permissions (if needed)
GRANT SELECT, INSERT, UPDATE, DELETE ON ecommercedb.user_product_interactions TO 'dsazuser'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON ecommercedb.rl_model_metadata TO 'dsazuser'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON ecommercedb.user_context_features TO 'dsazuser'@'%';
FLUSH PRIVILEGES;
```

## Rollback Plan

In case of issues, use this rollback script:

```sql
-- Rollback Script
-- Only use if necessary!

-- Backup data first (if any was inserted)
CREATE TABLE user_product_interactions_backup AS SELECT * FROM user_product_interactions;
CREATE TABLE rl_model_metadata_backup AS SELECT * FROM rl_model_metadata;
CREATE TABLE user_context_features_backup AS SELECT * FROM user_context_features;

-- Drop new tables
DROP TABLE IF EXISTS user_product_interactions;
DROP TABLE IF EXISTS rl_model_metadata;
DROP TABLE IF EXISTS user_context_features;

-- Verify rollback
SELECT 'Rollback completed' AS status;
```

## Data Retention Policy

### user_product_interactions
- **Retention**: 12 months
- **Archival**: Move to cold storage after 6 months
- **Cleanup Query**:
```sql
-- Archive old interactions (run monthly)
DELETE FROM user_product_interactions 
WHERE timestamp < DATE_SUB(NOW(), INTERVAL 12 MONTH);
```

### rl_model_metadata
- **Retention**: Keep all records (lightweight)
- **Inactive Models**: Keep for audit trail

### user_context_features
- **Retention**: Current users only
- **Cleanup**: Remove when user is deleted (CASCADE)

## Performance Considerations

### Estimated Table Growth

Assumptions:
- 10,000 daily active users
- 50 interactions per user per day
- 365 days retention

**user_product_interactions**:
- Daily inserts: ~500,000 rows
- Annual storage: ~180 million rows
- Estimated size: ~25 GB/year

**Indexing Strategy**:
- Primary operations: INSERT (high), SELECT by user_id + timestamp
- Composite indexes on frequently queried columns
- Consider partitioning by month for large datasets

### Partitioning (Future Enhancement)

For high-volume deployments, consider time-based partitioning:

```sql
ALTER TABLE user_product_interactions
PARTITION BY RANGE (TO_DAYS(timestamp)) (
    PARTITION p_2024_11 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p_2024_12 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    -- Add more partitions as needed
    PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

## Testing

### Verification Queries

```sql
-- Check table exists and is empty
SELECT COUNT(*) FROM user_product_interactions;
SELECT COUNT(*) FROM rl_model_metadata;
SELECT COUNT(*) FROM user_context_features;

-- Test insert
INSERT INTO user_product_interactions 
(user_id, product_id, interaction_type, session_id, timestamp, reward)
VALUES (1, 1, 'VIEW', 'test-session-123', NOW(), 0.1);

-- Verify insert
SELECT * FROM user_product_interactions ORDER BY id DESC LIMIT 1;

-- Test foreign key constraints
-- This should fail (invalid user_id)
INSERT INTO user_product_interactions 
(user_id, product_id, interaction_type, timestamp, reward)
VALUES (999999, 1, 'VIEW', NOW(), 0.1);

-- Clean up test data
DELETE FROM user_product_interactions WHERE session_id = 'test-session-123';
```

## Monitoring

After migration, monitor:

1. **Table sizes**: Track growth rate
2. **Query performance**: Monitor slow queries
3. **Index usage**: Verify indexes are being used
4. **Foreign key violations**: Check error logs

```sql
-- Monitor table sizes
SELECT 
    table_name,
    ROUND((data_length + index_length) / 1024 / 1024, 2) AS size_mb,
    table_rows
FROM information_schema.tables
WHERE table_schema = 'ecommercedb'
AND table_name IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features');

-- Check index usage
SHOW INDEX FROM user_product_interactions;
```

## Next Steps

After successful migration:

1. ✅ Create JPA entities in Spring Boot
2. ✅ Implement repository interfaces
3. ✅ Create interaction tracking service
4. ✅ Add logging to existing endpoints
5. ✅ Test integration with RL service
6. ✅ Deploy to staging
7. ✅ Monitor and validate
8. ✅ Deploy to production

## References

- Main Integration Plan: `rl-recommendation-integration.plan.md`
- RL Service Documentation: `Reinforce_recommend/README.md`
- Backend Implementation Guide: `RL_BACKEND_IMPLEMENTATION.md`

