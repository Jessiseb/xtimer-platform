<div align="center">

# xtimer-platform

**分布式定时任务平台 · 多机幂等 · 秒级精度 · 业务零侵入**

![Java](https://img.shields.io/badge/Java-8-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.7-6DB33F?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021.0.5-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs&logoColor=white)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.8-409EFF?logo=element&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![License](https://img.shields.io/github/license/Jessiseb/xtimer-platform)
![Last Commit](https://img.shields.io/github/last-commit/Jessiseb/xtimer-platform)

[项目简介](#项目简介) · [特性](#特性) · [技术架构](#技术架构) · [技术难点](#技术难点) · [页面效果](#管理端页面) · [快速开始](#快速开始) · [Roadmap](#roadmap)

</div>

---

一个可以替代 `@Scheduled` 的轻量定时任务平台。业务方只需要注册一个 HTTP 回调地址，平台负责「什么时候跑、谁来跑、跑没跑成功」，并提供可视化管理端。

## 目录

- [项目简介](#项目简介)
  - [背景与问题](#背景与问题)
  - [与 xxl-job 的取舍](#与-xxl-job-的取舍)
- [特性](#特性)
- [技术栈](#技术栈)
- [技术架构](#技术架构)
  - [执行链路](#执行链路)
  - [数据模型](#数据模型)
  - [关键设计](#关键设计)
- [技术难点](#技术难点)
  - [高精准](#高精准)
  - [高负载](#高负载)
- [管理端页面](#管理端页面)
- [快速开始](#快速开始)
- [目录结构](#目录结构)
- [接口说明](#接口说明)
- [Roadmap](#roadmap)
- [已知限制](#已知限制)
- [参与贡献](#参与贡献)
- [License](#license)

---

## 项目简介

### 背景与问题

团队有多个项目都带定时业务（库存预热、订单对账、抽奖池补货、缓存刷新等）。原先的实现方式是各业务服务里直接写 Spring 的 `@Scheduled` 注解：

```java
@Scheduled(cron = "0 0 9 * * ?")
public void preheat() { ... }
```

单机部署时没问题，但随着项目演进，暴露了三个绕不过去的问题：

1. **多机部署会重复执行。** 有一个项目是双机部署，`@Scheduled` 只懂「到点就跑」，两台机器各跑一次。对账、库存扣减这类操作重复执行直接导致数据不一致，且很难复现——取决于哪台机器先启动、GC 什么时候停顿。
2. **没有重试。** 回调失败就是失败，业务方只能等下一次调度或人工补数据；`@Scheduled` 本身不提供执行记录，失败了连「失败过几次」都查不到。
3. **没有统一视图。** 任务散落在各服务代码里，要知道「系统里现在有哪些定时任务、各自什么时候跑、昨天有没有失败」，只能逐个服务翻日志。

### 与 xxl-job 的取舍

评估过 xxl-job，结论是不合适——不是它不好，而是能力与诉求不匹配：

| 维度 | 我们的诉求 | xxl-job |
| --- | --- | --- |
| 使用方式 | 业务代码不想改 | 需引入依赖、实现 `IJobHandler`，任务逻辑与执行框架绑定 |
| 功能范围 | 只需要「定时执行 + 失败重试」 | 任务编排、分片广播、GLUE 脚本、调度报表、多执行器路由等 |
| 接入成本 | 一个回调地址 | 接入执行器、配置注册中心、理解调度中心模型 |

对我们来说，xxl-job 里大部分能力永远用不到，却要长期承担部署与维护成本；而且它与业务代码耦合偏高——任务逻辑写在实现类里，想从 A 服务搬到 B 服务，代码得一起搬。

所以决定自研，目标很克制：**把「定时」这件事从业务服务里彻底剥离，做成一个只认 HTTP 回调的基础设施。**

## 特性

| 能力 | 实现方式 |
| --- | --- |
| **多机安全** | Redis 分布式锁 + 任务状态校验 + 数据库唯一键，三层保障同一任务只执行一次 |
| **秒级精度** | 分钟时间桶 + 窗口内每秒扫描，时间片是外部状态而非内存定时器 |
| **业务零侵入** | 只回调 HTTP 接口，业务方无需引入任何 SDK |
| **失败可重试** | 上一分钟批次重放 + 锁超时未续期机制，把「是否重试」编码进锁生命周期 |
| **结果可观测** | 计划时间、实际耗时、状态、响应原文全部落库，管理端可查 |
| **水平扩展** | 无状态多实例，按时间桶与桶号切片，加机器即扩容 |
| **冷热分离** | 只物化未来 2 小时的任务实例，任务表不随时间膨胀 |

## 技术栈

| 层次 | 选型 |
| --- | --- |
| 语言 / 运行时 | Java 8（`maven.compiler.target` = 1.8） |
| 服务框架 | Spring Boot 2.7.7 · Spring Cloud 2021.0.5 · Spring Cloud Alibaba 2021.0.4.0 |
| 服务治理 | Nacos（注册 + 配置）· Spring Cloud Gateway · OpenFeign |
| 数据存储 | MySQL 8（MyBatis）· Redis 7（ZSET + 分布式锁 + Lua） |
| 定时解析 | Quartz `CronExpression` |
| 管理端 | Vue 3 · TypeScript · Vite 5 · Element Plus · Vue Router · axios 统一封装 |
| 工程化 | Maven 多模块 · 统一响应体 · 全局异常处理 · 结构化日志 |

## 技术架构

### 执行链路

任务从「一条 cron 配置」到「一次 HTTP 回调」，拆成四个职责单一的阶段，每段只做一件事：

```mermaid
flowchart LR
    A["业务方 / 管理端"] -->|创建·启停| B[("MySQL<br/>xtimer")]
    B --> C["Migrator<br/>每 10s 抢锁<br/>每小时实际执行一次"]
    C -->|"cron 展开未来 2 小时"| D[("MySQL<br/>timer_task")]
    C -->|"ZADD 批量写入"| E[("Redis ZSET<br/>yyyy-MM-dd HH:mm_bucket")]
    E --> F["Scheduler<br/>每秒 × 5 个桶"]
    F -->|"time_bucket_lock"| G["Trigger<br/>窗口内每秒 ZRANGEBYSCORE"]
    G --> H["Executor<br/>triggerPool 线程"]
    H -->|"HTTP 回调"| I["业务服务"]
    H -->|"回写 status / costTime / output"| D
```

| 阶段 | 核心类 | 触发频率 | 存储介质 | 关键动作 |
| --- | --- | --- | --- | --- |
| 迁移 Migrator | `MigratorWorker` + `MigrateManagerImpl` | 每 10s 尝试抢锁，按小时维度互斥 | MySQL → MySQL + Redis | 用 cron 展开未来 2 小时的执行点，批量落库并写入 ZSET |
| 调度 Scheduler | `SchedulerWorker` + `SchedulerTask` | 每秒 | Redis 锁 | 遍历 5 个桶，对「当前分钟」和「上一分钟」发起异步处理 |
| 触发 Trigger | `TriggerWorker` + `TriggerTimerTask` | 窗口内每秒 | Redis ZSET → MySQL 降级 | 按秒切片捞取待执行任务，投递给执行线程池 |
| 执行 Executor | `TriggerPoolTask` + `ExecutorWorker` | 队列驱动 | MySQL | 幂等校验 → 状态校验 → HTTP 回调 → 回写结果 |

### 数据模型

**`xtimer`（定时器定义）**

| 字段 | 说明 |
| --- | --- |
| `timer_id` | 主键 |
| `app` | 所属业务，管理端按此隔离 |
| `name` | 任务名称 |
| `status` | 0 新建 / 1 未激活 / 2 已激活（以 `TimerStatus` 枚举为准） |
| `cron` | Quartz cron 表达式 |
| `notify_http_param` | 回调配置 JSON：`{method, url, header, body}` |

**`timer_task`（执行实例）**

| 字段 | 说明 |
| --- | --- |
| `task_id` | 主键 |
| `timer_id` | 所属定时器 |
| `run_timer` | 计划执行时间（毫秒时间戳） |
| `status` | 0 未运行 / 1 运行中 / 2 成功 / 3 失败 |
| `cost_time` | 实际耗时（毫秒） |
| `output` | 回调响应原文 / 异常信息 |
| `(timer_id, run_timer)` | **唯一键**，防重复插入的最后一道防线 |

**Redis ZSET 键设计**

```
key    = "2026-10-22 17:30_3"        // 分钟桶_桶号（桶号 = timerId % bucketsNum）
member = "1001_1792661400000"        // timerId_计划执行时间戳
score  = 1792661400000               // 计划执行时间戳，供 ZRANGEBYSCORE 按秒取
```

`member` 里冗余了 `timerId` 与时间戳，从 ZSET 捞出的数据可直接构造任务对象，不必回查数据库。

### 关键设计

**① 冷热分离：只物化未来 2 小时的任务**

`MigrateManagerImpl.migrateTimer` 不把 cron 展开成「永久」任务表，只生成 `[now, now + 2 × migrateStepMinutes]`（当前配置 60 分钟，即 120 分钟）区间内的执行点。一个月跑一次的任务，数据库里始终只有未来一到两个待执行实例。

**② 按桶横向切分**

```
bucketId = timerId % bucketsNum          // 当前配置 bucketsNum = 5
zsetKey  = yyyy-MM-dd HH:mm + "_" + bucketId
lockKey  = "time_bucket_lock_" + yyyy-MM-dd HH:mm + "_" + bucketId
```

定时器被打散到 5 个桶，锁的粒度精确到「分钟 + 桶」，同一时刻最多 5 个分片并行推进、互不阻塞；扩容只需加实例，抢不到锁的实例自动空转。

**③ 三层幂等**

| 层次 | 手段 |
| --- | --- |
| 写入 | `INSERT IGNORE` + `(timer_id, run_timer)` 唯一键，重复迁移不产生重复任务 |
| 消费 | `ExecutorWorker` 先查任务状态，非 `NotRun` 直接 return |
| 状态 | 回调前二次确认定时器仍为「已激活」，用户暂停后立即生效 |

**④ 锁只加不解**

所有锁都不主动释放，只靠 TTL 过期——避免「A 的锁被 B 误删」这类经典问题，同时用 TTL 天然实现频率限制（创建/启停操作 3 秒内只允许一次）。

## 技术难点

### 高精准

**难点一：跨机器的时间精度**

单机 `ScheduledExecutorService` 精度够，但多机部署后不能指望某台机器的本地定时器。做法是把「时间」变成可切片的外部状态：

- 迁移阶段把执行点写进 Redis ZSET，`score` 就是计划执行时间戳；
- 调度阶段每秒遍历桶，对每个桶加 `time_bucket_lock_{分钟}_{桶号}`；
- 触发阶段在**一分钟窗口内每秒**执行一次 `ZRANGEBYSCORE key [t, t+1s)`，只捞当前这一秒该跑的任务。

时间片是数据，不是内存里的定时器状态，所以任何一台机器都能接手任意一个时间片。

**难点二：漏执行与重复执行的取舍**

分布式环境下「不漏」和「不重」天生冲突，这里的策略是**宁可重复投递，靠幂等兜底**：

- `SchedulerWorker` 每轮不只处理「当前分钟」，还会**重放上一分钟**的批次。机器时钟回拨、上一次分片执行异常、线程池打满被拒绝，都能在重放窗口里补上。
- 重放会不会导致重复执行？靠锁的续期策略控制：
  - 分片**执行成功** → `expireLock` 把锁从 70s 延长到 130s，下一分钟的重放抢不到锁，自然不重复；
  - 分片**执行失败** → 不续期，锁在 70s 后过期，下一分钟的重放能抢到锁，完成重试。

这把「是否重试」编码进了锁的生命周期，比额外维护一张重试表简单得多，也避免了重试表本身的一致性问题。

**难点三：消费端必须幂等**

即使投递重复，也不能重复回调业务。`ExecutorWorker.work` 的第一件事就是查任务：

```java
TaskModel task = taskMapper.getTasksByTimerIdUnix(timerId, unix);
if (task.getStatus() != TaskStatus.NotRun.getStatus()) {
    log.warn("重复执行任务： timerId" + timerId + ",runtimer:" + unix);
    return;
}
```

回调前还会重新查一次定时器状态，确认仍为「已激活」——用户点了暂停之后，已进入队列的任务不会再打出回调。

**难点四：一分钟窗口必须跑完才退出**

`TriggerWorker` 用 `java.util.Timer` 每 1 秒驱动 `TriggerTimerTask`，并用 `CountDownLatch` 阻塞等待：只有推进到窗口末尾（`tStart > endTime`）才 `countDown()`。这样「一分钟的任务」与「一分钟的扫描周期」严格对齐，不会出现尾巴被下一轮重复处理。

### 高负载

**① 写放大被冷热分离掐住**

不做全量物化，每次迁移只写 2 小时窗口。10 万个定时器 × 每分钟一次，也只产生 120 分钟内的任务实例，而不是几十万条永久记录。

**② 批量写 + 事务批提交**

- MySQL：一次 `batchSave` 写入整批执行点，`INSERT IGNORE` 让重复迁移变成空操作；
- Redis：`SessionCallback` + `multi/exec`，一批执行点在一个事务里完成 `ZADD`，避免逐条往返。

**③ 读写分离的降级路径**

`TriggerTimerTask.getTasksByTime` 优先走 Redis ZSET，只有缓存异常时才降级到 `taskMapper.getTasksByTimeRange`。热路径（每秒）几乎不碰 MySQL，MySQL 只承担迁移写入与结果回写。

**④ 线程池隔离与背压**

| 线程池 | 用途 | 核心 / 最大 / 队列 |
| --- | --- | --- |
| `schedulerPool` | 分片调度（每桶每分钟一个任务） | 10 / 100 / 99999 |
| `triggerPool` | 任务投递到 Executor | 10 / 100 / 99999 |

两个池子互不干扰：调度被拖慢不阻塞触发，触发被阻塞不影响下一分钟调度。拒绝策略统一 `CallerRunsPolicy`——队列打满时由调用线程自己执行，形成天然背压，而不是静默丢任务。

**⑤ 锁竞争随规模线性摊薄**

锁 key 带分钟和桶号，5 个桶意味着每秒最多 5 个分片争抢、且各自抢的是**不同 key**。实例数从 1 加到 10，锁竞争不会变成 10 倍——多出来的实例大部分时间在空转，这是有意的设计：**用一点空转换取无状态扩展能力**。

**⑥ 关键参数**

| 参数 | 当前值 | 作用 |
| --- | --- | --- |
| `scheduler.bucketsNum` | 5 | 桶数量，决定并行分片上限 |
| `scheduler.tryLockSeconds` | 70 | 首次抢锁后的 TTL，略大于一分钟 |
| `scheduler.successExpireSeconds` | 130 | 执行成功后延长到的 TTL，覆盖下一分钟重放窗口 |
| `trigger.zrangeGapSeconds` | 1 | 窗口内扫描步长（秒） |
| `migrator.migrateStepMinutes` | 60 | 迁移步长，实际窗口 = 2 × 60 = 120 分钟 |

## 管理端页面

管理端基于 Vue 3 + TypeScript + Element Plus，用两个页面覆盖「配置」与「观察」两条主线：在页面里把定时器配出来，再在执行记录里确认它是不是按预期跑了。以下截图来自本地运行实例（数据由 `xtimer-admin/mock` 提供，接口契约与真实服务一致）。

### 定时器管理

<img src="docs/images/01-timers.png" alt="定时器管理列表" width="100%" />

- 支持按 **app / 状态 / 名称** 组合筛选，分页查询
- 状态列直接体现定时器是否在跑（运行中 / 未激活），避免把「配置好了」误当成「已经在跑」
- 行内操作：编辑、启用 / 暂停、删除。启停走分布式锁，重复点击会被后端频率限制拦住

### 新建 / 编辑定时器

<img src="docs/images/02-timer-create.png" alt="新建定时器抽屉" width="100%" />

- **Cron 助手**：8 个常用预设一键填充，实时校验合法性，并预览接下来三次执行时间；支持 Quartz 的 6 位与 7 位写法，`?` 占位符做了兼容转换
- **回调配置**：方法（POST / GET / PUT / DELETE）、地址、Header 动态增删、Body JSON 校验，直接对应 `NotifyHTTPParam` 的四个字段
- 编辑时应用标识锁定不可改，避免把已有任务「搬」到别的 app 下产生孤立执行记录

### 执行记录

<img src="docs/images/03-tasks.png" alt="执行记录列表" width="100%" />

- 按应用、定时器 ID、执行状态、时间范围筛选，直接查 `timer_task`
- 四列对应底层字段：计划执行时间 `run_timer`、耗时 `cost_time`、状态 `status`、输出 `output`
- 失败任务标红，输出列可点击查看完整响应原文——这正是原先 `@Scheduled` 方案完全缺失的能力

## 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 8+ | 项目 `maven.compiler.target` = 1.8 |
| Maven | 3.6+ | 多模块构建 |
| MySQL | 8.x | 存定时器与执行记录 |
| Redis | 7.x | ZSET + 分布式锁 |
| Nacos | 2.x | 注册中心（默认 `127.0.0.1:8848`） |
| Node.js | 18+ | 仅管理端需要 |

### 1. 初始化数据库

```bash
mysql -uroot -p < db/bitstorm-svr-xtimer.sql
```

### 2. 准备配置

仓库不包含真实口令，按示例文件复制后填写：

```bash
cp bitstorm-svr-xtimer/src/main/resources/application-example.yml \
   bitstorm-svr-xtimer/src/main/resources/application.yml
# 其他需要启动的模块（gateway 等）同理，填入本地 MySQL / Redis / Nacos 口令
```

### 3. 启动后端

```bash
mvn clean install -DskipTests

java -jar bitstorm-svr-xtimer/target/bitstorm-svr-xtimer-*.jar     # :8082
java -jar bitstorm-svr-gateway/target/bitstorm-svr-gateway-*.jar   # :10001
```

### 4. 启动管理端

```bash
cd xtimer-admin
npm install
npm run mock   # 可选：后端接口未就绪时用本地 stub（:8082）
npm run dev    # http://localhost:5173
```

管理端通过 `/xtimer/**` 代理直连 xtimer 服务 8082，详细说明见 [xtimer-admin/README.md](xtimer-admin/README.md)。

### 端口一览

| 服务 | 端口 |
| --- | --- |
| 网关 `bitstorm-svr-gateway` | 10001 |
| 定时任务平台 `bitstorm-svr-xtimer` | 8082 |
| 管理端 dev server | 5173 |

## 目录结构

```
bitstorm-java-project-241010/
├── bitstorm-svr-api/          # DTO 与 Feign 接口
├── bitstorm-svr-common/       # 统一响应、分布式锁
├── bitstorm-svr-gateway/      # 网关 :10001
├── bitstorm-svr-xtimer/       # 定时任务平台
│   └── src/main/java/cn/bitoffer/xtimer/
│       ├── service/migrator/  # 迁移：cron → 任务实例
│       ├── service/scheduler/ # 调度：分钟桶 + 分片锁
│       ├── service/trigger/   # 触发：秒级扫描 ZSET
│       ├── service/executor/  # 执行：HTTP 回调 + 结果回写
│       ├── redis/TaskCache    # ZSET 读写
│       └── common/conf/       # 各阶段参数
├── bitstorm-svr-{seckill,lottery,mqtry,leaf,...}/  # 其他业务模块
├── db/                        # 建表脚本
├── docs/images/               # 文档截图
├── scripts/                   # 截图 / 示例配置生成脚本
└── xtimer-admin/              # Vue 3 管理端
```

## 接口说明

核心接口（`XtimerWebController`，前缀 `/xtimer`）：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/createTimer` | 创建定时器，返回 `timerId` |
| GET | `/enableTimer?app=&timerId=` | 激活并触发一次迁移 |
| GET | `/getAppTimers?app=` | 查询应用下定时器 |
| GET | `/getTimer?app=&timerId=` | 查询单个定时器（待暴露） |
| POST | `/updateTimer` | 更新定时器（待暴露） |
| GET | `/deleteTimer?app=&timerId=` | 删除定时器（待暴露） |
| GET | `/unEnableTimer?app=&timerId=` | 暂停定时器（待暴露） |
| GET | `/getTaskList?...` | 执行记录查询（待实现） |

统一响应体：

```json
{
  "code": 0,
  "message": "ok",
  "datetime": "2026-10-22 17:30:00",
  "data": {}
}
```

`code = 0` 表示成功。状态字典以枚举为准：`TimerStatus` 1 = 未激活 / 2 = 已激活；`TaskStatus` 0 = 未运行 / 1 = 运行中 / 2 = 成功 / 3 = 失败。

> `db/bitstorm-svr-xtimer.sql` 里 `status` 字段注释（0 新建 / 1 激活 / 2 未激活）与枚举方向相反，以代码枚举为准，DDL 注释待修正。

## Roadmap

- [x] 四段式调度链路（Migrator / Scheduler / Trigger / Executor）
- [x] 分布式锁 + 状态校验 + 唯一键的三层幂等
- [x] 管理端：定时器管理、执行记录查询
- [x] 配置脱敏与示例文件
- [ ] 实现 `getAppTimers` 分页与状态/名称筛选
- [ ] 暴露 update / delete / unEnable / getTimer 四个接口
- [ ] 执行统计接口与管理端仪表盘
- [ ] `ExecutorWorker` 支持 GET / PUT / DELETE 回调
- [ ] 桶级 ZSET 过期清理，避免内存持续增长
- [ ] 单任务重试计数与退避策略
- [ ] 手动触发一次执行（回调自测）

## 已知限制

| 问题 | 影响 | 方向 |
| --- | --- | --- |
| `ExecutorWorker` 的 `switch` 只有 POST 分支 | 配置 GET/PUT/DELETE 会拿到 null 响应并 NPE | 补齐分支 + 统一用 `RestTemplate.exchange` |
| ZSET 没有 TTL 清理 | 桶 key 只增不减，长期运行内存上涨 | 桶处理完成后按分钟粒度过期，或异步清理历史桶 |
| 调度每秒全桶空扫 | 5 桶 × 2 批次 = 10 次/秒，即使没有任务也在扫 | 空桶短路：先 `ZCARD` 判断再启动触发器 |
| `ExecutorWorker` 用同步 `RestTemplate` | 回调慢时占满 `triggerPool` 线程 | 换 WebClient / OkHttp 异步，或按业务隔离线程池 |
| 迁移每 10s 抢锁、实际每小时一次 | 多实例下产生无意义的锁竞争日志 | 改为按小时对齐触发，抢锁前判断是否已迁移 |
| 回调无超时与重试次数控制 | 单次回调失败只能等批次重放 | 增加单任务重试计数与退避策略 |

## 参与贡献

1. Fork 仓库并创建分支：`git checkout -b feat/your-feature`
2. 提交改动：`git commit -m "feat: 你的改动说明"`
3. 推送分支：`git push origin feat/your-feature`
4. 提交 Pull Request，说明改动动机与验证方式

提交前请确保后端 `mvn -q -DskipTests package` 通过、管理端 `npm run build` 通过。

## License

本项目基于 [MIT License](LICENSE) 开源。

---

<div align="center">

如果这个项目对你有帮助，欢迎点个 Star

感谢狂飙训练营提供的工程骨架与业务场景

</div>
