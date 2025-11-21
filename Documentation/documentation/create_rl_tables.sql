-- ============================================================================
-- RL Recommendation System - Database Migration Script
-- Version: 1.3
-- Date: 2024-11-20
-- Description: Creates tables for RL recommendation system integration
-- ============================================================================

USE ecommercedb;

-- ============================================================================
-- Table 1: user_product_interactions
-- Tracks all user-product interactions for RL training
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_product_interactions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    interaction_type VARCHAR(50) NOT NULL COMMENT 'CLICK, VIEW, CART_ADD, PURCHASE, WISHLIST, REMOVE_CART, REMOVE_WISHLIST',
    session_id VARCHAR(255),
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    context JSON COMMENT 'Additional context: category, price range, device, etc.',
    reward DECIMAL(10,2) DEFAULT 0.00 COMMENT 'Calculated reward for RL',
    
    -- Foreign Keys
    CONSTRAINT fk_interactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_interactions_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE,
    
    -- Indexes for performance
    INDEX idx_user_timestamp (user_id, timestamp),
    INDEX idx_product_timestamp (product_id, timestamp),
    INDEX idx_interaction_type (interaction_type),
    INDEX idx_session (session_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_reward (reward)
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='User-product interaction tracking for RL recommendation system';

-- ============================================================================
-- Table 2: rl_model_metadata
-- Stores metadata about trained RL models
-- ============================================================================

CREATE TABLE IF NOT EXISTS rl_model_metadata (
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
    
    -- Unique constraint
    CONSTRAINT unique_model_version UNIQUE KEY (model_name, version),
    
    -- Indexes
    INDEX idx_algorithm_active (algorithm_type, is_active),
    INDEX idx_training_date (training_date),
    INDEX idx_created_at (created_at)
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='RL model metadata and version tracking';

-- ============================================================================
-- Table 3: user_context_features
-- Stores precomputed user feature vectors for faster inference
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_context_features (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    feature_vector JSON NOT NULL COMMENT 'Precomputed user features for RL',
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign Key
    CONSTRAINT fk_context_features_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_last_updated (last_updated)
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Precomputed user feature vectors for RL models';

-- ============================================================================
-- Verification Queries
-- ============================================================================

-- Check if tables were created successfully
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    CREATE_TIME,
    TABLE_COLLATION,
    TABLE_COMMENT
FROM information_schema.TABLES 
WHERE TABLE_SCHEMA = 'ecommercedb'
  AND TABLE_NAME IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features')
ORDER BY TABLE_NAME;

-- Check indexes
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    INDEX_TYPE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'ecommercedb'
  AND TABLE_NAME IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features')
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- Check foreign keys
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'ecommercedb'
  AND TABLE_NAME IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features')
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

-- ============================================================================
-- Test Data (Optional - for development/testing only)
-- ============================================================================

-- Uncomment to insert test data in development

/*
-- Test interaction logging
INSERT INTO user_product_interactions 
(user_id, product_id, interaction_type, session_id, timestamp, reward, context)
VALUES 
(1, 1, 'VIEW', 'test-session-001', NOW(), 0.1, '{"page": "homepage", "position": 1}'),
(1, 1, 'CLICK', 'test-session-001', NOW(), 0.5, '{"page": "homepage", "position": 1}'),
(1, 2, 'VIEW', 'test-session-001', NOW(), 0.1, '{"page": "category", "category_id": 5}');

-- Test model metadata
INSERT INTO rl_model_metadata
(model_name, algorithm_type, version, is_active, training_date, performance_metrics, model_path, created_at)
VALUES
('ecommerce_recommender', 'LINUCB', 'v0.1.0', TRUE, NOW(), 
 '{"ctr": 0.045, "conversion_rate": 0.012, "avg_reward": 1.25}',
 '/models/linucb_model.pkl', NOW());

-- Test user context features
INSERT INTO user_context_features
(user_id, feature_vector, last_updated)
VALUES
(1, '{"demographics": {"age_group": "25-34"}, "purchase_history": {"total_orders": 10}}', NOW());
*/

-- ============================================================================
-- Completion Message
-- ============================================================================

SELECT 
    'RL Recommendation System tables created successfully!' AS status,
    (SELECT COUNT(*) FROM information_schema.TABLES 
     WHERE TABLE_SCHEMA = 'ecommercedb' 
     AND TABLE_NAME IN ('user_product_interactions', 'rl_model_metadata', 'user_context_features')
    ) AS tables_created;

