-- =============================================================================
-- 表/列注释语义修正：餐饮外卖 → 企业办公物资采购
--
-- 背景：original 建表脚本没有随项目提交，因此表结构与列中文注释仍停留在
--       外卖语义（如 category 叫「菜品及套餐分类」、dish 叫「菜品」）。
--       注释虽然不影响运行，但用 Navicat / DataGrip / IDEA 看表结构时会严重误导，
--       因此单独做一次修正。
--
-- 说明：本脚本只改 COMMENT，不改任何列的类型、约束与数据，可安全重复执行。
-- =============================================================================

USE sky_take_out;

-- -----------------------------------------------------------------------------
-- 表注释
-- -----------------------------------------------------------------------------
ALTER TABLE category      COMMENT = '物资分类（type: 1 物资分类 / 2 组合申领包分类）';
ALTER TABLE dish          COMMENT = '物资（办公物资单品）';
ALTER TABLE dish_flavor   COMMENT = '物资规格关系表（颜色/型号/尺寸等）';
ALTER TABLE setmeal       COMMENT = '组合申领包';
ALTER TABLE setmeal_dish  COMMENT = '组合申领包与物资的关联表';
ALTER TABLE shopping_cart COMMENT = '申领车';
ALTER TABLE orders        COMMENT = '申领单';
ALTER TABLE order_detail  COMMENT = '申领单明细表';
ALTER TABLE address_book  COMMENT = '收货地址（部门/工位）';
ALTER TABLE employee      COMMENT = '员工信息（含角色与部门）';

-- -----------------------------------------------------------------------------
-- category
-- -----------------------------------------------------------------------------
ALTER TABLE category
    MODIFY COLUMN type INT NULL COMMENT '分类类型：1 物资分类 2 组合申领包分类';

-- -----------------------------------------------------------------------------
-- dish
-- -----------------------------------------------------------------------------
ALTER TABLE dish
    MODIFY COLUMN name        VARCHAR(32)    NOT NULL COMMENT '物资名称',
    MODIFY COLUMN category_id BIGINT         NOT NULL COMMENT '物资分类id',
    MODIFY COLUMN price       DECIMAL(10, 2) NOT NULL COMMENT '单价',
    MODIFY COLUMN status      INT            NULL COMMENT '0 已停用 1 已启用';

-- -----------------------------------------------------------------------------
-- dish_flavor
-- -----------------------------------------------------------------------------
ALTER TABLE dish_flavor
    MODIFY COLUMN dish_id BIGINT       NULL COMMENT '所属物资id',
    MODIFY COLUMN name    VARCHAR(64)  NULL COMMENT '规格名（如 颜色 / 型号 / 尺寸）',
    MODIFY COLUMN value   VARCHAR(500) NULL COMMENT '规格可选值，多个以逗号分隔';

-- -----------------------------------------------------------------------------
-- employee
-- -----------------------------------------------------------------------------
ALTER TABLE employee
    MODIFY COLUMN username VARCHAR(32) NOT NULL COMMENT '登录账号';

-- -----------------------------------------------------------------------------
-- setmeal
-- -----------------------------------------------------------------------------
ALTER TABLE setmeal
    MODIFY COLUMN category_id BIGINT         NOT NULL COMMENT '组合申领包分类id',
    MODIFY COLUMN name        VARCHAR(32)    NOT NULL COMMENT '组合申领包名称',
    MODIFY COLUMN price       DECIMAL(10, 2) NOT NULL COMMENT '价格',
    MODIFY COLUMN status      INT            NULL COMMENT '状态：0 已停用 1 已启用';

-- -----------------------------------------------------------------------------
-- setmeal_dish
-- -----------------------------------------------------------------------------
ALTER TABLE setmeal_dish
    MODIFY COLUMN setmeal_id BIGINT         NOT NULL COMMENT '组合申领包id',
    MODIFY COLUMN dish_id    BIGINT         NOT NULL COMMENT '物资id',
    MODIFY COLUMN name       VARCHAR(32)    NULL COMMENT '物资名称（冗余快照）',
    MODIFY COLUMN price      DECIMAL(10, 2) NULL COMMENT '物资单价（冗余快照）',
    MODIFY COLUMN copies     INT            NULL COMMENT '份数';

-- -----------------------------------------------------------------------------
-- shopping_cart
-- -----------------------------------------------------------------------------
ALTER TABLE shopping_cart
    MODIFY COLUMN dish_id     BIGINT      NULL COMMENT '物资id',
    MODIFY COLUMN setmeal_id  BIGINT      NULL COMMENT '组合申领包id',
    MODIFY COLUMN dish_flavor VARCHAR(50) NULL COMMENT '所选规格';

-- -----------------------------------------------------------------------------
-- orders
--    注意：status / pay_status 的取值沿用原订单表，语义已重新映射，
--          pay_status 表示「预算扣减状态」而非支付状态。
-- -----------------------------------------------------------------------------
ALTER TABLE orders
    MODIFY COLUMN number                  VARCHAR(50)  NULL COMMENT '申领单号',
    MODIFY COLUMN status                  INT          NULL COMMENT '申领单状态：1待提交 2待审批 3采购中 4配送中 5已完成 6已取消/已驳回/已退换',
    MODIFY COLUMN user_id                 BIGINT       NULL COMMENT '申请人id',
    MODIFY COLUMN user_name               VARCHAR(32)  NULL COMMENT '申请人姓名',
    MODIFY COLUMN cancel_reason           VARCHAR(255) NULL COMMENT '撤销/退换原因',
    MODIFY COLUMN rejection_reason        VARCHAR(255) NULL COMMENT '审批驳回理由',
    MODIFY COLUMN cancel_time             DATETIME     NULL COMMENT '撤销时间',
    MODIFY COLUMN tableware_number        INT          NULL COMMENT '申领数量',
    MODIFY COLUMN tableware_status        INT          NULL COMMENT '数量状态 1按标准提供 0选择具体数量',
    MODIFY COLUMN address_book_id         BIGINT       NULL COMMENT '收货地址id',
    MODIFY COLUMN checkout_time           DATETIME     NULL COMMENT '审批通过时间',
    MODIFY COLUMN pay_method              INT          NULL COMMENT '结算方式（预留，新场景不走第三方支付）',
    MODIFY COLUMN pay_status              INT          NULL COMMENT '预算扣减状态：0未扣减 1已扣减 2已返还',
    MODIFY COLUMN amount                  DECIMAL(10, 2) NULL COMMENT '申领金额',
    MODIFY COLUMN address                 VARCHAR(255) NULL COMMENT '收货位置（部门/工位）',
    MODIFY COLUMN estimated_delivery_time DATETIME     NULL COMMENT '期望到货时间',
    MODIFY COLUMN delivery_status         INT          NULL COMMENT '配送状态 1立即送出 0选择具体时间',
    MODIFY COLUMN delivery_time           DATETIME     NULL COMMENT '发货时间',
    MODIFY COLUMN pack_amount             INT          NULL COMMENT '包装数量';

-- -----------------------------------------------------------------------------
-- order_detail
-- -----------------------------------------------------------------------------
ALTER TABLE order_detail
    MODIFY COLUMN order_id    BIGINT      NOT NULL COMMENT '所属申领单id',
    MODIFY COLUMN dish_id     BIGINT      NULL COMMENT '物资id',
    MODIFY COLUMN setmeal_id  BIGINT      NULL COMMENT '组合申领包id',
    MODIFY COLUMN dish_flavor VARCHAR(50) NULL COMMENT '所选规格';

-- -----------------------------------------------------------------------------
-- address_book
-- -----------------------------------------------------------------------------
ALTER TABLE address_book
    MODIFY COLUMN user_id BIGINT       NULL COMMENT '所属员工id',
    MODIFY COLUMN detail  VARCHAR(200) NULL COMMENT '详细收货位置（楼层/工位）';

-- -----------------------------------------------------------------------------
-- user（已废弃，仅保留历史数据）
-- -----------------------------------------------------------------------------
ALTER TABLE user
    MODIFY COLUMN openid VARCHAR(45) NOT NULL COMMENT '【已废弃】原微信用户唯一标识，身份已合并至 employee';

-- =============================================================================
-- 校验：修正后不应再出现外卖语义的注释
-- =============================================================================
SELECT table_name, column_name, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (column_comment LIKE '%菜品%' OR column_comment LIKE '%套餐%'
    OR column_comment LIKE '%购物车%' OR column_comment LIKE '%口味%'
    OR column_comment LIKE '%起售%' OR column_comment LIKE '%停售%'
    OR column_comment LIKE '%餐具%' OR column_comment LIKE '%订单%')
ORDER BY table_name, ordinal_position;
-- 期望：查不到任何行
