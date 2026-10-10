# ☁️ 采云台（CaiYunTai）

> 基于 **Spring Boot + MyBatis + MySQL + Redis** 构建的企业办公物资申领与采购管理平台

![Java](https://img.shields.io/badge/Java-8-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.3-6DB33F?logo=springboot&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-2.2.0-000000)
![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-6%2B-DC382D?logo=redis&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-1.20.2-009639?logo=nginx&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

---

## 📖 项目简介

采云台面向企业内部办公物资采购场景，将传统的“员工申请 → 人工审批 → 采购 → 发放”流程进行线上化管理。

系统围绕 **员工申领、部门预算、采购审批、物资管理、配送核销** 等核心业务展开，区分 `EMPLOYEE`、`PURCHASER`、`ADMIN` 三类角色，实现从员工提交物资申领，到预算预扣、采购审批、配送及最终核销的完整业务流程。

项目采用前后端分离架构，后端基于 Spring Boot 构建，并针对实际业务场景实现了 **JWT + Redis 登录态管理、AOP 角色鉴权、部门预算并发控制、Redis Cache-Aside 缓存、申领单事务控制与重复提交防护** 等机制。

> 面试/答辩时的一句话版本：**这是一个把「申领 → 预算预扣 → 审批 → 采购 → 配送 → 核销」全链路线上化的企业物资采购系统，重点在后端的一致性与并发安全，而不是堆中间件。**

---

## 🧭 目录

- [项目简介](#-项目简介)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [核心功能](#-核心功能)
- [角色与权限](#-角色与权限)
- [核心技术设计](#-核心技术设计)
- [核心业务流程](#-核心业务流程)
- [项目结构](#-项目结构)
- [数据库设计](#-数据库设计)
- [快速开始](#-快速开始)
- [接口文档](#-接口文档)
- [项目定位](#-项目定位)

---

## 🛠️ 技术栈

| 分类 | 技术选型 |
| --- | --- |
| **后端** | Spring Boot 2.7.3 · Spring MVC · MyBatis 2.2.0 · MySQL · Redis |
| **认证权限** | JJWT 0.9.1 · HandlerInterceptor · ThreadLocal · Spring AOP (AspectJ 1.9.4) |
| **数据访问** | MyBatis（XML 映射） · PageHelper 1.3.0 · Druid 1.2.1 |
| **接口文档** | Knife4j 3.0.2（Swagger 2） |
| **前端** | 原生 HTML / CSS / JavaScript（无构建步骤） |
| **部署与工具** | Nginx 1.20.2 · Maven · Lombok 1.18.20 |

**运行环境**：JDK 8 · Maven 3.6+ · MySQL 8.0+ · Redis 6+ · Nginx 1.20.2

---

## 🏗️ 系统架构

```
                        ┌──────────────────────────────┐
   管理端浏览器 :8082 ──▶ │  Nginx   admin-ui            │ ──┐
                        │  html/admin-ui               │   │  /api/ ──▶ /admin/**
                        └──────────────────────────────┘   │
                                                           ▼
                        ┌──────────────────────────────┐  ┌─────────────────────────┐
   员工端浏览器 :8083 ──▶ │  Nginx   staff-ui            │  │  Spring Boot  :8084     │
                        │  html/staff-ui               │  │  ├─ Interceptor  认证    │
                        └──────────────────────────────┘  │  ├─ AOP          鉴权    │
                                     /api/ ──▶ /user/** ──▶ │  ├─ Service      业务    │
                                                           │  └─ Mapper       DAO     │
                                                           └─────────┬───────┬───────┘
                                                                     │       │
                                                            ┌────────▼──┐ ┌──▼────────┐
                                                            │  MySQL    │ │  Redis    │
                                                            │  13 张表   │ │  登录态/缓存│
                                                            └───────────┘ └───────────┘
```

**关键设计：端侧隔离。** 管理端与员工端不是同一套接口换角色，而是两条独立的认证链路：

- 管理端走 `JwtTokenAdminInterceptor`，拦截 `/admin/**`，请求头 `token`
- 员工端走 `JwtTokenUserInterceptor`，拦截 `/user/**`，请求头 `authentication`

两侧使用**不同的 SecretKey**，因此管理端的 Token 无法在员工端使用，反之亦然。

---

## ✨ 核心功能

- **员工申领**：物资浏览、组合申领包、申领车、提交申领单、进度查询、催办及确认收货。
- **采购审批**：申领单审批、驳回、采购、配送、核销及状态流转。
- **部门预算**：按部门和月份维护预算，在员工提交申领单时进行预算预扣，并在撤销或驳回等场景进行返还。
- **后台管理**：员工、部门、预算、物资分类、物资及组合申领包等基础数据管理。
- **角色权限**：支持 `EMPLOYEE` / `PURCHASER` / `ADMIN` 三级角色，不同角色拥有不同的业务操作权限。

---

## 🔐 角色与权限

员工端与管理端共用一张 `employee` 表，靠 `role` 字段区分权限。权限判定采用 **拦截器认证 + AOP 授权** 两级过滤：

| 能力 | EMPLOYEE | PURCHASER | ADMIN |
| --- | :---: | :---: | :---: |
| 员工端：浏览物资 / 组合包、申领车、提交申领单、催办、确认收货 | ✅ | — | — |
| 管理端：申领单审批、驳回、采购、配送、核销 | — | ✅ | ✅ |
| 管理端：查看部门与预算 | — | ✅ | ✅ |
| 管理端：部门 / 预算维护 | — | — | ✅ |
| 管理端：物资、分类、组合申领包维护 | — | — | ✅ |
| 管理端：员工账号、企业设置、工作台 | — | — | ✅ |

### 演示账号

执行 `demo_data.sql` 后会生成三个角色的演示账号，**密码统一为 `123456`**：

| 用户名 | 姓名 | 角色 | 入口 |
| --- | --- | --- | --- |
| `admin` | 系统管理员 | `ADMIN` | 管理端 `:8082` |
| `purchaser` | 采购专员小李 | `PURCHASER` | 管理端 `:8082` |
| `zhangsan` | 张三 | `EMPLOYEE` | 员工端 `:8083` |

---

## 🚀 核心技术设计

### 1. JWT + Redis 登录认证

管理端与员工端采用独立 JWT 认证链路，通过不同 SecretKey 与请求头实现端侧隔离。

JWT 仅保存用户身份标识，同时将 Token 写入 Redis 白名单并设置对应 TTL。请求经过 Interceptor 时同时校验 JWT 与 Redis 登录状态，使原本无状态的 JWT 支持主动注销与 Token 即时失效。

### 2. AOP 角色权限控制

基于自定义 `@RequireRole` 注解与 Spring AOP 实现方法级角色鉴权，并通过 ThreadLocal 保存当前请求的用户 ID 与角色信息。

将 Interceptor 身份认证与 AOP 业务授权进行职责分离，使权限控制与具体业务逻辑解耦。

### 3. 部门预算并发控制

员工提交申领单时对所属部门当月预算进行预扣。

预算扣减没有采用“SELECT 查询余额 → Java 判断 → UPDATE”的方式，而是通过带余额条件的 MySQL UPDATE 将额度判断与扣减合并为一次原子操作，结合 InnoDB 并发控制及 SQL 影响行数判断扣减结果，避免并发申领导致部门预算超扣。

### 4. 申领单事务与防重复提交

申领单提交过程中包含预算预扣、主单创建、明细批量写入及申领车清理等多个数据库操作，通过 `@Transactional` 保证核心链路的数据一致性。

订单金额由服务端根据申领车重新计算，避免直接信任客户端金额；同时使用 Redis `SET NX` + TTL 对短时间内的重复提交进行拦截。

### 5. Redis Cache-Aside 缓存

针对物资目录与组合申领包读多写少的特点，引入 Redis Cache-Aside 缓存。

缓存采用随机 TTL 分散集中失效时间，并对空结果设置较短 TTL；数据修改后主动删除相关缓存，在降低数据库查询压力的同时维护缓存与数据库的最终一致性。

### Redis Key 一览

所有 Key 前缀集中在 `RedisKeyConstant` 中统一管理，命名规范为 `业务:对象:标识`：

| Key 前缀 | 用途 |
| --- | --- |
| `login:token:` | 登录态白名单 → `employeeId` |
| `login:fail:` | 登录失败计数（防暴力破解） |
| `order:submit:` | 申领单防重复提交 |
| `order:reminder:` | 申领单催办限流 |
| `order:number:` | 申领单号自增 |
| `goods:list:` | 物资列表缓存 |
| `combo:list:` | 组合申领包列表缓存 |
| `shop:status` | 采购开关 |

---

## 🔄 核心业务流程

```
员工登录
   ↓
浏览物资 / 组合申领包
   ↓
加入申领车
   ↓
提交申领单
   ↓
服务端重新计算金额
   ↓
部门预算预扣
   ↓
生成申领主单 + 明细
   ↓
采购专员审批
   ↓
采购 / 配送
   ↓
员工确认收货
   ↓
申领单完成
```

申领单状态流转：`1 待提交 → 2 待审批 → 3 采购中 → 4 配送中 → 5 已完成`，另可流转至 `6 已取消`。
在**驳回**与**撤销**场景下，已预扣的部门预算会原路返还。

---

## 📁 项目结构

```
caiyuntai/
├── backend/cai-yun-tai/                 # 后端 Maven 多模块工程
│   ├── caiyuntai-common/                # 通用模块：常量、上下文、异常、结果封装、工具类
│   │   └── com/caiyuntai/
│   │       ├── constant/                # RoleConstant / RedisKeyConstant / StatusConstant ...
│   │       ├── context/                 # BaseContext（ThreadLocal 用户上下文）
│   │       ├── exception/               # 业务异常体系
│   │       ├── result/                  # Result / PageResult 统一返回
│   │       └── utils/                   # JwtUtil / HttpClientUtil / AliOssUtil
│   ├── caiyuntai-pojo/                  # 实体模块
│   │   └── com/caiyuntai/{entity,dto,vo}/
│   ├── caiyuntai-server/                # 服务模块（可启动）
│   │   ├── com/caiyuntai/
│   │   │   ├── annotation/              # @AutoFill / @RequireRole
│   │   │   ├── aspect/                  # AutoFillAspect / RoleAspect
│   │   │   ├── config/                  # WebMvcConfiguration / RedisConfiguration / OssConfiguration
│   │   │   ├── controller/admin/        # 管理端接口（/admin/**）
│   │   │   ├── controller/user/         # 员工端接口（/user/**）
│   │   │   ├── interceptor/             # JwtTokenAdminInterceptor / JwtTokenUserInterceptor
│   │   │   ├── handler/                 # GlobalExceptionHandler
│   │   │   ├── mapper/                  # DAO 接口
│   │   │   └── service/impl/            # 业务实现
│   │   └── resources/
│   │       ├── mapper/*.xml             # MyBatis 映射文件
│   │       ├── application.yml
│   │       └── application-dev.yml      # 数据源 / Redis / JWT 配置
│   └── sql/                             # 建表、迁移、演示数据与回归脚本
├── front/                               # 前端静态资源 + Nginx
│   ├── nginx-1.20.2/
│   │   ├── conf/nginx-mac.conf          # 站点配置（8082 管理端 / 8083 员工端）
│   │   ├── html/admin-ui/               # 管理端页面
│   │   ├── html/staff-ui/               # 员工端页面
│   │   └── html/shared/                 # 两端共用样式
│   ├── start-nginx-mac.sh
│   └── restart-nginx.sh                 # 安全重启（只清理占用 8082/8083 的 nginx）
├── interface/                           # Knife4j 导出的接口文档（员工端 / 管理端）
└── 采云台-项目地图.md                     # 架构、模块地图与调用链路梳理
```

---

## 🗄️ 数据库设计

数据库名 `sky_take_out`（沿用课程模板库名，业务语义已切换为企业办公物资采购），共 **13 张表**：

| 数据库表 | 业务含义 |
| --- | --- |
| `employee` | 员工（含 `role` 角色、`dept_id` 部门） |
| `department` | 部门 |
| `budget` | 部门月度预算（`dept_id + period` 唯一） |
| `category` | 分类（`type=1` 物资分类 / `type=2` 组合包分类） |
| `dish` | 物资 |
| `dish_flavor` | 物资规格（颜色 / 型号 / 尺寸等） |
| `setmeal` | 组合申领包 |
| `setmeal_dish` | 组合包内物资明细（冗余存名称与单价，属有意的快照设计） |
| `orders` | 申领单主表 |
| `order_detail` | 申领单明细 |
| `shopping_cart` | 申领车 |
| `address_book` | 收货地址（含部门 + 工位） |
| `user` | 【已废弃】原微信用户表，身份已合并至 `employee`，保留仅作兼容 |

> **关于表名**：项目由课程模板（外卖场景）改造而来，Java 层已按新业务重命名为 `Goods` / `Combo` 等，
> 但**物理表名保留了 `dish` / `setmeal` 等原名**以避免动历史数据；表与列的中文注释已由
> `fix_comments.sql` 修正为办公物资语义。阅读代码时请以业务名称理解。

---

## ⚡ 快速开始

### 1. 准备数据库

按顺序执行 `backend/cai-yun-tai/sql/` 下的脚本（**顺序不可颠倒**，已实测）：

```bash
SQL=backend/cai-yun-tai/sql

# ① 建库 + 建出全部 13 张表（最终态结构）
mysql -uroot -p < $SQL/schema.sql

# ② 播种部门基础数据（行政部 / 技术部 / 市场部）与当月预算
#    注意：此脚本名为「迁移」，但全新库也必须执行，
#    否则 demo_data.sql 里员工的 dept_id 会取不到部门
mysql -uroot -p < $SQL/migration_office_supplies.sql

# ③ 播种演示物资、组合申领包与三类角色演示账号
mysql -uroot -p < $SQL/demo_data.sql
```

<details>
<summary>已有「苍穹外卖」旧库时改走增量路线</summary>

```bash
mysql -uroot -p < $SQL/migration_office_supplies.sql        # 增量迁移
mysql -uroot -p < $SQL/migration_fix_defects_20260929.sql    # 索引与残留数据修复
mysql -uroot -p < $SQL/fix_comments.sql                      # 表/列注释语义修正
mysql -uroot -p < $SQL/demo_data.sql                         # 演示数据
```

上述脚本均用 `information_schema` 判断 + `ON DUPLICATE KEY UPDATE` 实现，
**可重复执行**，已存在的内容会自动跳过。

</details>

### 2. 修改配置

编辑 `backend/cai-yun-tai/caiyuntai-server/src/main/resources/application-dev.yml`，
填入本机的 MySQL 与 Redis 连接信息：

```yaml
sky:
  datasource:
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: <你的 MySQL 密码>
  redis:
    host: localhost
    port: 6379
    password: <你的 Redis 密码>
    database: 10
```

### 3. 启动后端

```bash
cd backend/cai-yun-tai
mvn clean package -DskipTests
java -jar caiyuntai-server/target/caiyuntai-server-1.0-SNAPSHOT.jar
```

后端启动在 **`http://localhost:8084`**（也可直接在 IDEA 中运行 `CaiyuntaiApplication`）。

### 4. 启动前端

```bash
bash front/restart-nginx.sh
```

脚本会先清理占用 `8082` / `8083` 的旧 nginx 进程，再用新配置干净启动：

| 端 | 地址 | 说明 |
| --- | --- | --- |
| 管理端 | http://localhost:8082 | 管理员 / 采购专员 |
| 员工端 | http://localhost:8083 | 普通员工 |

Nginx 会把 `/api/` 反向代理到后端 `8084`：管理端 `/api/` → `/admin/`，员工端 `/api/` → `/user/`。

### 5. 验证

用演示账号登录：`admin` / `purchaser` / `zhangsan`，密码均为 `123456`。

---

## 📘 接口文档

后端集成 Knife4j，启动后访问 **http://localhost:8084/doc.html**，分为两个分组：

- **采云台 · 管理端接口** —— 50 个接口
- **采云台 · 员工端接口** —— 26 个接口

无需启动服务也可查阅 `interface/` 目录下导出的 OpenAPI 文档：

- `采云台-接口文档-管理端.json`
- `采云台-接口文档-用户端.json`

> 管理端接口需在请求头携带 `token`；员工端接口需携带 `authentication`。

---

## 💡 项目定位

采云台主要用于学习和实践 Java Web 后端开发中的典型业务问题，包括：

身份认证、角色权限、Redis 缓存、数据库事务、并发控制、接口幂等及业务状态流转。

项目重点不在于堆叠中间件，而是在具体业务场景下选择合适的实现方案，并理解不同方案在 **一致性、并发安全、性能与复杂度** 之间的取舍。

> 更详细的架构说明、模块地图与调用链路，见 [`采云台-项目地图.md`](./采云台-项目地图.md)。

---

## ⚠️ 安全提示

本仓库中的 `application-dev.yml` 为**本地开发配置**，包含明文数据库密码与占位 JWT 密钥。
部署到公网前请务必：

- 将数据库密码、Redis 密码、JWT SecretKey 改由**环境变量**注入（如 `${DB_PASSWORD}`）；
- 更换示例 JWT 密钥，并关闭 Knife4j 的 `/doc.html` 暴露。

---

## 📄 License

本项目基于 [MIT License](./LICENSE) 开源，仅供学习与交流使用。
