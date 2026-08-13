-- ============================================================
-- 多模态图生文（VLM）数据库变更
-- 商品表新增图片描述字段（幂等，可重复执行）
-- 对应 specs/004-vlm-multimodal/data-model.md
-- ============================================================

SET @dbname = DATABASE();
SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = 'product'
      AND COLUMN_NAME = 'image_description'
);

SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE product ADD COLUMN image_description VARCHAR(1024) NULL COMMENT ''视觉模型生成的图片描述，用于增强检索''',
    'SELECT ''product.image_description 已存在，跳过'' AS info'
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
