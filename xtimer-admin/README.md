# xtimer-admin

定时任务可视化管理端，对接 `bitstorm-svr-xtimer`，经网关 `:10001` 的 `/xtimer/**` 路由访问。
技术栈：Vue 3 + TypeScript + Vite + Element Plus。

## 快速开始

```bash
npm install
npm run mock     # 终端 1：本地 stub，监听 8082，带种子数据
npm run dev      # 终端 2：http://localhost:5173
npm run build    # 类型检查 + 产出 dist/
```

代理规则：`/xtimer/**` → `http://localhost:8082`（可用环境变量 `XTIMER_API` 覆盖）。
前端请求 `/xtimer/getAppTimers`，8082 上的 xtimer 服务由 `@RequestMapping("/xtimer")` 接收，路径无需改写。

### 关于网关（重要）

`bitstorm-svr-gateway` 的 xtimer 路由配了 `RewritePath=/xtimer/(?<segment>.*), /${segment}`，会把 `/xtimer/createTimer` 改写成 `/createTimer`，
而 `XtimerWebController` 与 `XTimerFeignController` 都挂在 `/xtimer` 前缀下 —— 经网关访问会 404。
前端因此直连 8082，不走网关。若要走网关，需删除该 RewritePath 过滤器（另有 Nacos 未启动时 `lb://` 无法解析的问题）。

### mock 说明

`mock/server.mjs` 是零依赖 Node 脚本，按本文档的接口契约实现全部 7 个接口 + 内存数据（3 个定时器、13 条执行记录，含失败样例），
只用于前端开发联调，不参与生产构建。真实服务起来后，停掉 mock、把 8082 让给 xtimer 即可，前端零改动。


## 目录结构

```
src/
  api/          # 接口封装（timer / task）
  components/   # StatusTag 状态标签、CronHelper 表达式助手、NotifyParamForm 回调配置
  router/       # /timers 定时器管理、/tasks 执行记录
  types/        # DTO 与状态字典
  utils/        # request 封装、时间格式化、cron 解析
  views/        # TimerList 列表页、TimerFormDrawer 新建/编辑抽屉、TaskList 执行记录
```

## 接口契约（前后端联调依据）

统一约定：

- 入口 `/xtimer/**`，直连 xtimer 服务 8082（不经网关，原因见上）
- 响应包装 `{code, message, datetime, data}`，`code=0` 成功，非 0 前端弹出 `message`
- 日期字段序列化为 `yyyy-MM-DD HH:mm:ss` 字符串（后端需配 Jackson date-format）
- `runTimer` / `costTime` 单位为毫秒

已有接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/createTimer` | body: TimerDTO，data: timerId |
| GET | `/enableTimer?app=&timerId=` | 启用 |

前端依赖、后端待补：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/getAppTimers?app=&status=&name=&page=&pageSize=` | data: `{list, total}` |
| GET | `/getTimer?app=&timerId=` | data: TimerDTO |
| POST | `/updateTimer` | body: TimerDTO |
| GET | `/deleteTimer?app=&timerId=` | 注意创建/删除 3 秒频率锁 |
| GET | `/unEnableTimer?app=&timerId=` | 暂停 |
| GET | `/getTaskList?app=&timerId=&status=&startTime=&endTime=&page=&pageSize=` | data: `{list, total}`，执行记录 |
| GET | `/getTaskStats?app=` | data: `{total, enabled, disabled, todayRun, successRate, recentFailed}`，仪表盘用（二期） |

DTO 字段：

- `TimerDTO`：timerId、app、name、status、cron、notifyHTTPParam{method,url,header,body}、createTime、modifyTime
- `TaskDTO`：taskId、timerId、app、status、runTimer、costTime、output、createTime、modifyTime

状态字典（以 `TimerStatus` / `TaskStatus` 枚举为准，DB 注释与旧原型 HTML 均不一致，勿参考）：

- Timer：1=未激活（暂停），2=已激活（运行中）
- Task：0=未运行，1=运行中，2=成功，3=失败

## 后端待办

1. `XTimerServiceImpl.GetAppTimers` 当前 `return null`，需实现并支持分页与筛选
2. 暴露 update / delete / unEnable / get 四个接口（Service 逻辑已存在）
3. 新增 `timer_task` 执行记录查询接口（`TaskMapper.getTasksByTimeRange` 可复用）
4. 修 `doUnEnableTimer` 状态校验写反：现在要求「已是未激活才能去激活」，暂停必报错
5. （建议）修 `ExecutorWorker.executeTimerCallBack` switch 只有 POST 分支，其他方法会 NPE
6. （建议）Jackson 配置 `spring.jackson.date-format=yyyy-MM-dd HH:mm:ss`、`time-zone=GMT+8`

## 分期

- 一期（本次）：定时器管理（列表/创建/编辑/启停/删除）+ 执行记录
- 二期：仪表盘（`/getTaskStats`）
- 三期：测试回调（手动触发一次执行）、运行监控
