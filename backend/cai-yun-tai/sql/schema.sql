-- =============================================================================
-- 采云台（CaiYunTai）· 企业办公物资申领与采购管理平台 —— 完整建表脚本
--
-- 数据库：sky_take_out（沿用课程模板库名，业务语义已切换为企业办公物资采购）
-- 字符集：utf8mb4 / utf8mb4_general_ci
--
-- 用途
--   全新部署时一次性建出当前完整表结构（共 13 张表），无需再逐个执行历史迁移脚本。
--   本文件由 mysqldump --no-data 从已迁移完成的库中导出，等价于「最终态」结构。
--
-- 与其它 SQL 脚本的关系
--   schema.sql                       最终态建表（本文件）── 全新部署用它
--   migration_office_supplies.sql    增量迁移：department / budget 等新表与新列
--   migration_fix_defects_20260929.sql 缺陷修复：索引与残留数据清理
--   fix_comments.sql                 表/列注释语义修正（外卖 → 办公物资）
--   demo_data.sql                    演示数据与三类角色演示账号
--
--   若你的库原本就是「苍穹外卖」旧库，请改走增量路线：
--       migration_office_supplies.sql → migration_fix_defects_20260929.sql → fix_comments.sql
--
--   若你是全新部署（推荐顺序，已实测通过）：
--       schema.sql                      建出 13 张表（最终态结构）
--       migration_office_supplies.sql   播种部门基础数据（行政部/技术部/市场部）与当月预算
--       demo_data.sql                   播种演示物资、组合申领包与三类角色演示账号
--   注意：migration_office_supplies.sql 虽名为「迁移」，但其中第 214 行起是
--         部门/预算的基础数据 INSERT，且用 ON DUPLICATE KEY UPDATE 保证可重复执行，
--         因此全新库也必须执行它，否则 demo_data.sql 里员工的 dept_id 会取不到部门。
--
-- 注意
--   * 本脚本只建表，不写入任何业务数据；演示数据请另跑 demo_data.sql。
--   * 建表语句中不包含 DROP TABLE，重复执行会因表已存在而报错；
--     需要重建请先手动 DROP DATABASE / DROP TABLE。
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `sky_take_out` DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;

USE `sky_take_out`;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint DEFAULT NULL COMMENT '所属员工id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '详细收货位置（楼层/工位）',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  `dept_name` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '部门名称',
  `station` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '工位/办公位置',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='收货地址（部门/工位）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `budget` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dept_id` bigint NOT NULL COMMENT '部门id',
  `period` varchar(7) NOT NULL COMMENT '预算周期，格式 yyyy-MM',
  `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '预算总额',
  `used_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '已使用金额',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_budget_dept_period` (`dept_id`,`period`),
  KEY `idx_budget_period` (`period`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门预算表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '分类类型：1 物资分类 2 组合申领包分类',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '分类名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int DEFAULT NULL COMMENT '分类状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='物资分类（type: 1 物资分类 / 2 组合申领包分类）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '上级部门id，0为顶级',
  `name` varchar(64) NOT NULL COMMENT '部门名称',
  `code` varchar(32) NOT NULL COMMENT '部门编码',
  `leader_id` bigint DEFAULT NULL COMMENT '部门负责人id',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `create_user` bigint DEFAULT NULL,
  `update_user` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_department_code` (`code`),
  KEY `idx_department_parent` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) COLLATE utf8mb3_bin NOT NULL COMMENT '物资名称',
  `category_id` bigint NOT NULL COMMENT '物资分类id',
  `price` decimal(10,2) NOT NULL COMMENT '单价',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT NULL COMMENT '0 已停用 1 已启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `spec` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格型号，如 A4/80g、12号',
  `unit` varchar(16) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '计量单位：支/盒/台/包',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='物资（办公物资单品）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dish_flavor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '所属物资id',
  `name` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格名（如 颜色 / 型号 / 尺寸）',
  `value` varchar(500) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格可选值，多个以逗号分隔',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=104 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='物资规格关系表（颜色/型号/尺寸等）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) COLLATE utf8mb3_bin NOT NULL COMMENT '登录账号',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  `role` varchar(20) COLLATE utf8mb3_bin NOT NULL DEFAULT 'EMPLOYEE' COMMENT '角色：EMPLOYEE普通员工 / PURCHASER采购专员 / ADMIN行政管理员',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`),
  KEY `idx_employee_role` (`role`),
  KEY `idx_employee_dept` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息（含角色与部门）';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名字',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '所属申领单id',
  `dish_id` bigint DEFAULT NULL COMMENT '物资id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '组合申领包id',
  `dish_flavor` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '所选规格',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=114 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='申领单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '申领单号',
  `status` int DEFAULT NULL COMMENT '申领单状态：1待提交 2待审批 3采购中 4配送中 5已完成 6已取消/已驳回/已退换',
  `user_id` bigint DEFAULT NULL COMMENT '申请人id',
  `address_book_id` bigint DEFAULT NULL COMMENT '收货地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '审批通过时间',
  `pay_method` int DEFAULT NULL COMMENT '结算方式（预留，新场景不走第三方支付）',
  `pay_status` int DEFAULT NULL COMMENT '预算扣减状态：0未扣减 1已扣减 2已返还',
  `amount` decimal(10,2) DEFAULT NULL COMMENT '申领金额',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货位置（部门/工位）',
  `user_name` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '申请人姓名',
  `consignee` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '撤销/退换原因',
  `rejection_reason` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '审批驳回理由',
  `cancel_time` datetime DEFAULT NULL COMMENT '撤销时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '期望到货时间',
  `delivery_status` int DEFAULT NULL COMMENT '配送状态 1立即送出 0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '发货时间',
  `pack_amount` int DEFAULT NULL COMMENT '包装数量',
  `tableware_number` int DEFAULT NULL COMMENT '申领数量',
  `tableware_status` int DEFAULT NULL COMMENT '数量状态 1按标准提供 0选择具体数量',
  `approve_user` bigint DEFAULT NULL COMMENT '审批人id',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_remark` varchar(255) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '审批备注',
  `requester_dept` bigint DEFAULT NULL COMMENT '申请部门id（冗余，便于统计）',
  `remind_time` datetime DEFAULT NULL COMMENT '最近一次催办时间',
  `is_returned` tinyint NOT NULL DEFAULT '0' COMMENT '是否退换单：0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_number` (`number`),
  KEY `idx_orders_requester_dept` (`requester_dept`),
  KEY `idx_orders_approve_user` (`approve_user`)
) ENGINE=InnoDB AUTO_INCREMENT=80 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='申领单';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '组合申领包分类id',
  `name` varchar(32) COLLATE utf8mb3_bin NOT NULL COMMENT '组合申领包名称',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `status` int DEFAULT NULL COMMENT '状态：0 已停用 1 已启用',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='组合申领包';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `setmeal_dish` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `setmeal_id` bigint NOT NULL COMMENT '组合申领包id',
  `dish_id` bigint NOT NULL COMMENT '物资id',
  `name` varchar(32) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '物资名称（冗余快照）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '物资单价（冗余快照）',
  `copies` int DEFAULT NULL COMMENT '份数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=55 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='组合申领包与物资的关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shopping_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '主键',
  `dish_id` bigint DEFAULT NULL COMMENT '物资id',
  `setmeal_id` bigint DEFAULT NULL COMMENT '组合申领包id',
  `dish_flavor` varchar(50) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '所选规格',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=149 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='申领车';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) COLLATE utf8mb3_bin NOT NULL COMMENT '【已废弃】原微信用户唯一标识，身份已合并至 employee',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='【已废弃】原微信用户表，身份已合并至 employee，仅保留历史数据';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

