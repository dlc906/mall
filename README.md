# Mall - 商城微服务项目

基于 **Spring Cloud Alibaba** 微服务生态构建的电商商城系统，覆盖商城核心业务流程。

## 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot + Spring Cloud | 2.7.18 + 2021.0.9 |
| 微服务 | Spring Cloud Alibaba | 2021.0.6.1 |
| 注册/配置 | Nacos | 2.2.3 |
| 网关 | Spring Cloud Gateway + Sentinel | - |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis + Redisson | - |
| 消息队列 | RocketMQ | 5.x |
| 搜索引擎 | Elasticsearch | 7.x (可选) |
| 分布式事务 | Seata (AT模式) | 2.0.0 |
| 任务调度 | XXL-Job | 3.4.2 |
| 前端 | Vue 3 + Element Plus + Pinia | - |

## 项目结构

```
mall/
├── pom.xml                   # 父POM (依赖管理)
├── start-dev-env.bat         # 一键启动中间件
├── stop-dev-env.bat          # 一键停止中间件
├── docker-compose.yml        # Docker Compose 部署
├── conf/                     # 中间件配置 (RocketMQ/MySQL初始化)
├── scripts/                  # 管理脚本
│   ├── start-dev-env.bat     # 启动中间件
│   ├── stop-dev-env.bat      # 停止中间件
│   ├── start-frontend.bat    # 启动前端
│   └── stop-frontend.bat     # 停止前端
├── mall-common/              # 公共模块 (Redis/Seata/Jackson配置)
├── mall-gateway/             # API网关 (8080)
├── mall-auth/                # 认证中心 (8081)
├── mall-user/                # 用户服务 (8082)
├── mall-product/             # 商品服务 (8083)
├── mall-order/               # 订单服务 (8084)
├── mall-payment/             # 支付服务 (8085)
├── mall-distribution/        # 分销服务 (8086)
├── mall-job/                 # 任务调度 (8087)
├── mall-web/                 # Vue 3 前端 (5173)
└── doc/                      # SQL脚本 & Nacos配置
    ├── sql/                  # 各模块建表SQL
    └── nacos-config/         # Nacos配置中心导入
```

## 微服务架构

```
Vue 3 前端 → Gateway(8080) → Nacos注册中心
                ↓
    ┌───────────┼───────────┬───────────┬──────────┐
    ↓           ↓           ↓           ↓          ↓
  auth       user       product      order     payment
 (8081)     (8082)      (8083)      (8084)    (8085)
    │           │           │           │          │
    └───────────┴─────┬─────┴───────────┴──────────┘
                      ↓
    ┌─────────────────┼─────────────────┬──────────┐
    ↓                 ↓                 ↓          ↓
  Redis            RocketMQ        Elasticsearch  Seata
(Redisson)        (异步消息)         (可选)      (AT事务)
    │                 │                          │
    └──── XXL-Job(调度) ── Gateway(Sentinel限流) ──┘
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+ (前端)
- MySQL 8.0+

### 方式一：本机开发

#### 1. 启动中间件

双击 `start-dev-env.bat` 一键启动（会自动跳过已运行的服务），或逐个启动：

| 中间件 | 端口 | 启动方式 |
|--------|------|---------|
| Nacos | 8848 | `nacos/bin/startup.cmd -m standalone` |
| Redis | 6379 | `redis-server.exe` |
| RocketMQ NameServer | 9876 | `mqnamesrv.cmd` |
| RocketMQ Broker | 10911 | `mqbroker.cmd -n 127.0.0.1:9876 -c broker.conf` |
| Seata Server | 8091 | `seata-server.bat -p 8091` |
| XXL-Job Admin | 8100 | `java -jar xxl-job-admin.jar --server.port=8100` |
| Sentinel Dashboard | 18080 | `java -jar sentinel-dashboard.jar --server.port=18080` |

#### 2. 初始化数据库

```bash
mysql -u root -p < doc/sql/init-all.sql
```

XXL-Job 需要额外执行 `tables_xxl_job.sql` 创建 `xxl_job` 数据库。

#### 3. Nacos 配置

在 Nacos 控制台 (http://127.0.0.1:8848/nacos) `public` 命名空间导入：

- `doc/nacos-config/common-config.yaml` — 公共配置

#### 4. 启动微服务

```bash
# 编译
mvn clean install -DskipTests

# 启动 (推荐IDE中逐个启动)
mall-gateway  →  8080   (必需)
mall-auth     →  8081   (必需)
mall-user     →  8082   (必需)
mall-product  →  8083   (必需)
mall-order    →  8084   (必需)
mall-payment  →  8085   (可选，测试支付时才需要)
mall-distribution → 8086 (可选，测试分销时才需要)
mall-job      →  8087   (可选，定时任务)
```

#### 5. 启动前端

```bash
cd mall-web
npm install
npm run dev
```

或双击 `scripts/start-frontend.bat`。

访问 http://localhost:5173

#### 6. 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| admin | 123456 | 管理员 |
| test | 123456 | 测试用户 |

### 方式二：Docker 部署

```bash
docker compose up -d
docker compose exec -T mysql mysql -u root -proot < doc/sql/init-all.sql
```

`conf/mysql/init/init.sql` 会自动创建业务数据库和 XXL-Job 表。

## RocketMQ 主题

| Topic | 生产者 | 消费者 | 用途 |
|-------|--------|--------|------|
| `order-pay-result` | `mall-payment` | `mall-order` | 支付结果通知 |
| `order-completed` | `mall-order` | `mall-distribution` | 订单完成触发分销 |

> RocketMQ Broker 需开启 `autoCreateTopicEnable=true`（`conf/broker.conf`）

## XXL-Job 任务

需要在 Admin 后台 (`http://127.0.0.1:8100`) 手动添加任务：

| 任务名称 | JobHandler | Cron | 说明 |
|---------|-----------|------|------|
| 取消超时订单 | `cancelUnpaidOrders` | `0 */5 * * * ?` | 取消30分钟未支付订单并回滚库存 |
| 月度佣金结算 | `settleCommission` | `0 0 2 1 * ?` | 每月1号结算分销佣金 |
| ES 同步 | `syncProductToES` | `0 0 * * * ?` | 每小时全量同步商品到ES |
| 清理过期Token | `cleanExpiredTokens` | `0 0 3 * * ?` | Redis TTL兜底 |
| 健康检查 | `healthCheck` | `0 * * * * ?` | 保活 |

## 核心功能

### 业务流程

1. **注册/登录** — JWT双Token + Redis黑名单
2. **浏览商品** — 分类筛选 + ES关键词搜索
3. **购物车** — Redis Hash后端持久化 + 实时库存刷新
4. **下单** — Redisson分布式锁扣库存 + 多商品部分失败自动回滚 + Seata AT分布式事务
5. **支付** — 本地消息表 + 同步通知 + MQ兑底 + 定时补偿（最终一致性）
6. **订单管理** — 状态流转 + 条件更新防竞态 + 取消/确认收货
7. **分销** — 邀请码绑定 + 二级佣金 + 月度结算

### 业务风险防护

| 风险 | 防护措施 |
|------|---------|
| 重复下单 | Redis幂等键（60秒内相同内容拦截） |
| 重复支付 | 数据库UNIQUE KEY `(order_no, pay_type)` 硬约束 + 支付前幂等检查 |
| 多商品下单部分失败 | 记录已扣库存商品，失败时逐个回滚（持有锁状态下） |
| 库存超卖 | Redis INCRBY 原子扣减 + 负数回滚 + Redisson分布式锁 |
| 超时取消与支付竞态 | 条件更新 `WHERE status=0`，已支付订单不会被误取消 |
| 支付后订单状态未更新 | 本地消息表(status=0) + 同步Feign + MQ兑底 + 定时任务补偿 |
| 支付重试超限 | Saga反向补偿：自动退款 + 取消订单 + 库存回滚 |
| 取消订单库存加不回 | 重试3次+1秒间隔，全部失败回滚取消 |
| ES不可用时抛异常 | `@Autowired(required=false)` + 判空保护 |
| JS精度丢失 | `@JsonFormat(Shape.STRING)` 19位ID以字符串传输 |
| 越权查看订单 | `X-User-Id` 头校验订单归属 |
| 越权支付 | Feign查询订单后校验userId |
| 日志无限增长 | 统一 logback-spring.xml：单文件10MB轮转 + 保留30天 |

### 中间件接入说明

| 中间件 | 必需 | 说明 |
|--------|------|------|
| Nacos | ✅ | 注册发现 + 配置中心 |
| MySQL | ✅ | 数据持久化 |
| Redis | ✅ | 认证/缓存/分布式锁 |
| RocketMQ | ❌ | 支付和分销链路 |
| Seata | ❌ | 分布式事务 |
| Elasticsearch | ❌ | 商品全文搜索 |
| XXL-Job | ❌ | 定时任务 |
| Sentinel | ❌ | 流量控制/Dashboard |

## 高并发能力

- **Sentinel** 网关层QPS限流 + 熔断降级
- **Redisson** 分布式锁扣库存（防超卖）
- **RocketMQ** 异步消息削峰填谷
- **Redis** 商品详情缓存（5分钟TTL，库存/销量变更时主动失效）
- **Seata AT** 分布式事务保障下单一致性
- **XXL-Job** 定时取消超时订单 + 库存回滚重试

## 支付最终一致性设计

支付采用**本地消息表 + 同步通知 + MQ兑底 + 定时补偿**的 Saga 方案，保证支付服务与订单服务的数据最终一致：

```
用户点击支付
    ↓
① 本地事务：写支付记录 (status=0 处理中)
    ↓
② 同步Feign通知订单服务 paySuccess
    ├─ 成功 → 本地记录标记 status=1 ✅
    └─ 失败 → 发送MQ兑底（订单服务恢复后自动消费）
    ↓
③ 定时任务（每30秒）扫描 status=0 且超3分钟的记录
    ├─ 订单已支付 → 同步本地记录 ✅
    ├─ 订单未支付 → 重投MQ + 重试次数+1
    └─ 重试>3次 → Saga补偿：自动退款 + 取消订单 + 库存回滚
```

**特性：**
- 订单服务宕机不影响支付落账（先记账再通知）
- 消息丢失由定时任务兜底扫描修复
- 订单服务幂等处理，MQ重复投递不重复扣减

## API 文档

启动后访问 Knife4j 文档：

- 认证服务: http://localhost:8081/doc.html
- 用户服务: http://localhost:8082/doc.html
- 商品服务: http://localhost:8083/doc.html
- 订单服务: http://localhost:8084/doc.html
- 支付服务: http://localhost:8085/doc.html
- 分销服务: http://localhost:8086/doc.html

## 监控

| 控制台 | 地址 | 账号 |
|--------|------|------|
| Nacos | http://127.0.0.1:8848/nacos | nacos/nacos |
| XXL-Job | http://127.0.0.1:8100 | admin/123456 |
| Sentinel | http://127.0.0.1:18080 | sentinel/sentinel |
