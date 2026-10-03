-- =============================================================================
-- 企业办公物资采购平台  演示数据初始化
--
-- 前置：先执行 migration_office_supplies.sql
-- 用途：创建三个角色的演示账号，便于验证员工端与管理端。
-- 密码说明：所有演示账号密码均为 123456，
--          库中存的是 MD5("123456") = e10adc3949ba59abbe56e057f20f883e，
--          与原项目 EmployeeServiceImpl 的 DigestUtils.md5DigestAsHex 一致。
--
-- 本脚本可重复执行：所有 INSERT 都做了存在性判断。
-- =============================================================================

USE sky_take_out;

-- -----------------------------------------------------------------------------
-- 1. 演示账号
--    admin     行政管理员  → 管理端全部权限（物资、部门、预算、员工）
--    purchaser 采购专员    → 管理端审批、发货、核销
--    zhangsan  普通员工    → 员工端（:8082）提报申领
-- -----------------------------------------------------------------------------
INSERT INTO employee (name, username, password, phone, sex, id_number, role, dept_id, status,
                      create_time, update_time, create_user, update_user)
VALUES ('系统管理员', 'admin', 'e10adc3949ba59abbe56e057f20f883e', '13800000001', '1', '110101199001010011',
        'ADMIN', NULL, 1, NOW(), NOW(), 1, 1),
       ('采购专员小李', 'purchaser', 'e10adc3949ba59abbe56e057f20f883e', '13800000002', '1', '110101199001010022',
        'PURCHASER', NULL, 1, NOW(), NOW(), 1, 1),
       ('张三', 'zhangsan', 'e10adc3949ba59abbe56e057f20f883e', '13800000003', '1', '110101199001010033',
        'EMPLOYEE', (SELECT id FROM department WHERE code = 'DEPT_TECH'), 1, NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    role = VALUES(role),
    password = VALUES(password),
    dept_id = VALUES(dept_id),
    status = VALUES(status);

-- -----------------------------------------------------------------------------
-- 2. 物资分类（type=1 为物资，type=2 为组合申领包）
-- -----------------------------------------------------------------------------
INSERT INTO category (type, name, sort, status, create_time, update_time, create_user, update_user)
VALUES (1, '办公耗材', 1, 1, NOW(), NOW(), 1, 1),
       (1, '办公设备', 2, 1, NOW(), NOW(), 1, 1),
       (1, '清洁用品', 3, 1, NOW(), NOW(), 1, 1),
       (1, '员工福利', 4, 1, NOW(), NOW(), 1, 1),
       (2, '入职组合包', 1, 1, NOW(), NOW(), 1, 1),
       (2, '部门标准配置', 2, 1, NOW(), NOW(), 1, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status);

-- -----------------------------------------------------------------------------
-- 3. 物资示例（含新场景新增的 spec 规格 与 unit 单位）
--    每条都先判断是否存在同名物资，保证可重复执行
-- -----------------------------------------------------------------------------
INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT 'A4 复印纸', c.id, 25.00, '', '70g 双面打印纸，日常办公必备', 'A4/70g/500张', '包', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '办公耗材'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = 'A4 复印纸');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '中性签字笔', c.id, 3.50, '', '0.5mm 黑色，书写顺滑', '0.5mm/黑', '支', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '办公耗材'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '中性签字笔');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '订书机', c.id, 18.00, '', '标准型 12 号订书机', '12号', '台', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '办公耗材'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '订书机');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '无线键鼠套装', c.id, 129.00, '', '2.4G 无线连接，静音按键', '2.4G/静音', '套', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '办公设备'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '无线键鼠套装');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '显示器支架', c.id, 199.00, '', '单臂气压式，支持 17-32 英寸', '17-32英寸', '个', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '办公设备'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '显示器支架');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '抽纸', c.id, 12.00, '', '3 层加厚，整箱装', '3层/6包', '提', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '清洁用品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '抽纸');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '洗手液', c.id, 15.50, '', '抑菌型，按压式', '500ml', '瓶', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '清洁用品'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '洗手液');

INSERT INTO dish (name, category_id, price, image, description, spec, unit, status,
                  create_time, update_time, create_user, update_user)
SELECT '保温杯', c.id, 89.00, '', '316 不锈钢内胆，员工福利', '500ml', '个', 1,
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '员工福利'
  AND NOT EXISTS (SELECT 1 FROM dish d WHERE d.name = '保温杯');

-- -----------------------------------------------------------------------------
-- 4. 组合申领包示例 + 包内物资关联
--    说明：setmeal_dish 冗余保存了物资名称与单价，属于有意的快照设计，
--          使套餐价格与物资后续调价解耦。
-- -----------------------------------------------------------------------------
INSERT INTO setmeal (category_id, name, price, status, description, image,
                     create_time, update_time, create_user, update_user)
SELECT c.id, '新员工入职办公包', 150.00, 1, '入职第一天所需的全部办公用品', '',
       NOW(), NOW(), 1, 1
FROM category c
WHERE c.name = '入职组合包'
  AND NOT EXISTS (SELECT 1 FROM setmeal s WHERE s.name = '新员工入职办公包');

-- 关联明细：仅在尚未建立关联时插入
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies)
SELECT s.id, d.id, d.name, d.price, 1
FROM setmeal s
         JOIN dish d ON d.name IN ('中性签字笔', '订书机')
WHERE s.name = '新员工入职办公包'
  AND NOT EXISTS (SELECT 1 FROM setmeal_dish sd WHERE sd.setmeal_id = s.id AND sd.dish_id = d.id);

INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies)
SELECT s.id, d.id, d.name, d.price, 2
FROM setmeal s
         JOIN dish d ON d.name = 'A4 复印纸'
WHERE s.name = '新员工入职办公包'
  AND NOT EXISTS (SELECT 1 FROM setmeal_dish sd WHERE sd.setmeal_id = s.id AND sd.dish_id = d.id);

-- -----------------------------------------------------------------------------
-- 5. 给所有部门补一份当月预算，便于直接验证申领流程
-- -----------------------------------------------------------------------------
INSERT INTO budget (dept_id, period, total_amount, used_amount, status,
                    create_time, update_time, create_user, update_user)
SELECT d.id, DATE_FORMAT(NOW(), '%Y-%m'), 20000.00, 0.00, 1, NOW(), NOW(), 1, 1
FROM department d
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- -----------------------------------------------------------------------------
-- 6. 给演示员工配一个默认收货地址（部门 + 工位）
-- -----------------------------------------------------------------------------
INSERT INTO address_book (user_id, consignee, phone, sex, detail, dept_name, station, label, is_default)
SELECT e.id, e.name, e.phone, '1', 'A座12层技术部办公区', '技术部', 'A座12层工位03', '公司', 1
FROM employee e
WHERE e.username = 'zhangsan'
  AND NOT EXISTS (SELECT 1 FROM address_book a WHERE a.user_id = e.id);
