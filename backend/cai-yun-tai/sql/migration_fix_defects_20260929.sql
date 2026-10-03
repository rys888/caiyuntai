-- =============================================================================
-- 采云台 · 缺陷修复数据变更脚本
--
-- 适用库：sky_take_out
-- 说明：
--   1. 本脚本只做「增量变更」，不物理删除数据，可回退（回退语句见各段注释）。
--   2. 可重复执行：用 information_schema 判断 + 动态 SQL，已存在的索引会被跳过。
--   3. 对应缺陷清单：P1-8（订单号无唯一索引）、P1-9（残留外卖演示数据）。
-- =============================================================================

USE sky_take_out;
SET @db := DATABASE();

-- -----------------------------------------------------------------------------
-- 1. P1-8：orders.number 增加唯一索引，防止并发下产生重复单号
--    回退：ALTER TABLE orders DROP INDEX uk_orders_number;
-- -----------------------------------------------------------------------------
SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND INDEX_NAME = 'uk_orders_number') = 0,
        'ALTER TABLE orders ADD UNIQUE INDEX uk_orders_number (number)',
        'SELECT ''uk_orders_number 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 2. P1-9：停用外卖演示数据（保留可回退，不物理删除）
--    办公物资已带 spec/unit 字段，外卖旧菜品这两列均为 NULL，据此识别；
--    外卖分类 id 集中在 11~21，办公分类 23~28。
--    回退：把下面两处的 status 从 0 改回 1。
-- -----------------------------------------------------------------------------
UPDATE dish SET status = 0 WHERE spec IS NULL AND unit IS NULL AND status = 1;

UPDATE category SET status = 0
WHERE id IN (11, 12, 13, 15, 16, 17, 18, 19, 20, 21) AND status = 1;

-- -----------------------------------------------------------------------------
-- 3. 清理后需清空 Redis 物资缓存，避免 goods_* 旧缓存继续暴露外卖菜品
--    （缓存已补 TTL，但旧的 ttl=-1 永久键需手动清一次）：
--    redis-cli -a 123456 -n 10 --no-auth-warning --scan --pattern 'goods_*' \
--      | xargs redis-cli -a 123456 -n 10 --no-auth-warning del
-- -----------------------------------------------------------------------------
