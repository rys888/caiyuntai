-- =============================================================================
-- 苍穹外卖 → 企业办公物资采购平台  数据库变更脚本
--
-- 适用库：sky_take_out
-- 说明：
--   1. 本脚本只做「增量变更」，不删除任何表与数据，可回退（回退语句见文件末尾）。
--   2. 按设计文档决策：user 表保留但标记废弃，不物理删除。
--   3. 状态值沿用原订单表，语义重新映射，因此无需 UPDATE 历史数据。
--   4. 脚本可重复执行：MySQL 并不支持 ADD COLUMN IF NOT EXISTS
--      （9.x 亦不支持），因此改用 information_schema 判断 + 动态 SQL 的方式，
--      已经存在的列/索引会被跳过，不会报 Duplicate column name。
-- =============================================================================

USE sky_take_out;

-- -----------------------------------------------------------------------------
-- 1. employee：增加角色与部门，承接原 user 表的身份职能
-- -----------------------------------------------------------------------------
SET @db := DATABASE();

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'employee' AND COLUMN_NAME = 'role') = 0,
        'ALTER TABLE employee ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT ''EMPLOYEE'' COMMENT ''角色：EMPLOYEE普通员工 / PURCHASER采购专员 / ADMIN行政管理员''',
        'SELECT ''employee.role 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'employee' AND COLUMN_NAME = 'dept_id') = 0,
        'ALTER TABLE employee ADD COLUMN dept_id BIGINT NULL COMMENT ''所属部门id''',
        'SELECT ''employee.dept_id 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'employee' AND INDEX_NAME = 'idx_employee_role') = 0,
        'ALTER TABLE employee ADD INDEX idx_employee_role (role)',
        'SELECT ''idx_employee_role 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'employee' AND INDEX_NAME = 'idx_employee_dept') = 0,
        'ALTER TABLE employee ADD INDEX idx_employee_dept (dept_id)',
        'SELECT ''idx_employee_dept 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 存量数据兜底：原有员工都是管理端使用者，统一提升为管理员。
--
-- 注意一个坑：如果这里无差别地把所有 EMPLOYEE 都刷成 ADMIN，
-- 那么「先跑迁移、后跑 demo_data」时，demo 里刚插入的普通员工会被覆盖，
-- 导致员工端登录后拿到 ADMIN 角色、被管理端拦截器放行（越权）。
-- 因此这里只处理「迁移之前就存在的」历史数据，用脚本执行当下的时间做分界。
SET @migrate_cutoff := NOW();

UPDATE employee
SET role = 'ADMIN'
WHERE role = 'EMPLOYEE'
  AND create_time < @migrate_cutoff;

-- -----------------------------------------------------------------------------
-- 2. 新增 department 部门表
--    parent_id 预留多级能力，当前实施按单级处理（统一为 0）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS department
(
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '上级部门id，0为顶级',
    name        VARCHAR(64) NOT NULL COMMENT '部门名称',
    code        VARCHAR(32) NOT NULL COMMENT '部门编码',
    leader_id   BIGINT      NULL COMMENT '部门负责人id',
    sort        INT         NOT NULL DEFAULT 0 COMMENT '排序',
    status      INT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time DATETIME    NULL,
    update_time DATETIME    NULL,
    create_user BIGINT      NULL,
    update_user BIGINT      NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_code (code),
    KEY idx_department_parent (parent_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门表';

-- -----------------------------------------------------------------------------
-- 3. 新增 budget 部门预算表
--    取代原项目的「支付」环节：提交申领单预扣，驳回/撤销返还
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budget
(
    id           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键',
    dept_id      BIGINT         NOT NULL COMMENT '部门id',
    period       VARCHAR(7)     NOT NULL COMMENT '预算周期，格式 yyyy-MM',
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '预算总额',
    used_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0 COMMENT '已使用金额',
    status       INT            NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    create_time  DATETIME       NULL,
    update_time  DATETIME       NULL,
    create_user  BIGINT         NULL,
    update_user  BIGINT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_dept_period (dept_id, period),
    KEY idx_budget_period (period)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='部门预算表';

-- -----------------------------------------------------------------------------
-- 4. dish：增加规格型号与计量单位
--    采购场景必备字段（例如「A4/80g」「包」），外卖场景不需要
-- -----------------------------------------------------------------------------
SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'spec') = 0,
        'ALTER TABLE dish ADD COLUMN spec VARCHAR(64) NULL COMMENT ''规格型号，如 A4/80g、12号''',
        'SELECT ''dish.spec 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'dish' AND COLUMN_NAME = 'unit') = 0,
        'ALTER TABLE dish ADD COLUMN unit VARCHAR(16) NULL COMMENT ''计量单位：支/盒/台/包''',
        'SELECT ''dish.unit 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 5. orders：增加审批与催办相关字段
--    注意：pay_status 字段名保持不变，语义由「支付状态」改为「预算扣减状态」
--          （0未扣减 / 1已扣减 / 2已返还）
-- -----------------------------------------------------------------------------
SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'approve_user') = 0,
        'ALTER TABLE orders ADD COLUMN approve_user BIGINT NULL COMMENT ''审批人id''',
        'SELECT ''orders.approve_user 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'approve_time') = 0,
        'ALTER TABLE orders ADD COLUMN approve_time DATETIME NULL COMMENT ''审批时间''',
        'SELECT ''orders.approve_time 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'approve_remark') = 0,
        'ALTER TABLE orders ADD COLUMN approve_remark VARCHAR(255) NULL COMMENT ''审批备注''',
        'SELECT ''orders.approve_remark 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'requester_dept') = 0,
        'ALTER TABLE orders ADD COLUMN requester_dept BIGINT NULL COMMENT ''申请部门id（冗余，便于统计）''',
        'SELECT ''orders.requester_dept 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'remind_time') = 0,
        'ALTER TABLE orders ADD COLUMN remind_time DATETIME NULL COMMENT ''最近一次催办时间''',
        'SELECT ''orders.remind_time 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND COLUMN_NAME = 'is_returned') = 0,
        'ALTER TABLE orders ADD COLUMN is_returned TINYINT NOT NULL DEFAULT 0 COMMENT ''是否退换单：0否 1是''',
        'SELECT ''orders.is_returned 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_requester_dept') = 0,
        'ALTER TABLE orders ADD INDEX idx_orders_requester_dept (requester_dept)',
        'SELECT ''idx_orders_requester_dept 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'orders' AND INDEX_NAME = 'idx_orders_approve_user') = 0,
        'ALTER TABLE orders ADD INDEX idx_orders_approve_user (approve_user)',
        'SELECT ''idx_orders_approve_user 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 6. address_book：收货地址语义变为「部门 / 工位」
-- -----------------------------------------------------------------------------
SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'address_book' AND COLUMN_NAME = 'dept_name') = 0,
        'ALTER TABLE address_book ADD COLUMN dept_name VARCHAR(64) NULL COMMENT ''部门名称''',
        'SELECT ''address_book.dept_name 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'address_book' AND COLUMN_NAME = 'station') = 0,
        'ALTER TABLE address_book ADD COLUMN station VARCHAR(64) NULL COMMENT ''工位/办公位置''',
        'SELECT ''address_book.station 已存在，跳过'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 7. user 表标记废弃（保留数据，不删除）
--    MySQL 没有「表级废弃标记」，用注释表达；如后续确认不再需要可手工 drop
-- -----------------------------------------------------------------------------
ALTER TABLE user COMMENT ='【已废弃】原微信用户表，身份已合并至 employee，仅保留历史数据';

-- =============================================================================
-- 初始化数据（可选，建议执行以便验证功能）
-- =============================================================================

-- 部门示例
INSERT INTO department (parent_id, name, code, sort, status, create_time, update_time, create_user, update_user)
VALUES (0, '行政部', 'DEPT_ADMIN', 1, 1, NOW(), NOW(), 1, 1),
       (0, '技术部', 'DEPT_TECH', 2, 1, NOW(), NOW(), 1, 1),
       (0, '市场部', 'DEPT_MARKET', 3, 1, NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 预算示例：给各部门设置当月预算（周期按执行时的月份自动计算）
INSERT INTO budget (dept_id, period, total_amount, used_amount, status, create_time, update_time, create_user, update_user)
SELECT d.id, DATE_FORMAT(NOW(), '%Y-%m'), 20000.00, 0.00, 1, NOW(), NOW(), 1, 1
FROM department d
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- =============================================================================
-- 回退脚本（如需还原，取消注释逐条执行）
-- =============================================================================
-- ALTER TABLE employee DROP INDEX idx_employee_role, DROP INDEX idx_employee_dept;
-- ALTER TABLE employee DROP COLUMN role, DROP COLUMN dept_id;
-- DROP TABLE IF EXISTS budget;
-- DROP TABLE IF EXISTS department;
-- ALTER TABLE dish DROP COLUMN spec, DROP COLUMN unit;
-- ALTER TABLE orders DROP INDEX idx_orders_requester_dept, DROP INDEX idx_orders_approve_user;
-- ALTER TABLE orders DROP COLUMN approve_user, DROP COLUMN approve_time, DROP COLUMN approve_remark,
--                    DROP COLUMN requester_dept, DROP COLUMN remind_time, DROP COLUMN is_returned;
-- ALTER TABLE address_book DROP COLUMN dept_name, DROP COLUMN station;
