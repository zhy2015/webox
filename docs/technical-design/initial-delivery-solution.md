# WeBox 90 分钟初版技术方案

## 1. 方案目标

在严格 90 分钟内交付一个可本地启动、使用真实 MySQL 持久化、可以从浏览器完成员工下单和管理员维护菜单的初版。优先级依次是：能启动、端到端正确、数据不出错、可验证、再补体验。

本方案不追求一次性建成完整生产平台，但关键商业数据不能使用假数据替代，权限、价格、幂等、有效餐次和库存必须由后端控制。

## 2. 当前环境事实

| 项目 | 当前状态 | 执行要求 |
| --- | --- | --- |
| Java | 默认 Corretto 8；已安装 OpenJDK 17.0.16 | 为项目 shell 激活 JDK 17，无需重新安装 |
| Maven | 3.9.10 | 工程生成 Maven Wrapper，之后统一使用 `./mvnw` |
| Node.js | 24.3.0 | 可用；工程提交 lockfile |
| npm | 11.14.0 | 可用；统一使用 `npm ci` |
| Docker | 28.2.2 | CLI 可用 |
| Docker Compose | 2.35.1 | 可用 |
| Docker daemon | 未运行 | 启动 Docker Desktop 后才能运行 MySQL |

环境未满足时不能把构建失败误判成代码问题。

## 3. 固定技术选型

### 前端

- React + TypeScript + Vite，单 SPA 同时承载 Employee 与 Console。
- React Router 管理角色路由；TanStack Query 管理服务端数据。
- Cart 使用轻量 Context/reducer 并版本化写入 `localStorage`，避免再引入全局状态框架。
- 表单优先使用受控组件和共享校验函数；只有复杂表单确实需要时再引入 React Hook Form。
- 普通 CSS + design tokens 完成响应式布局，不在时间盒内引入大型 UI 框架或动画体系。
- Vitest + Testing Library 做核心状态测试；Playwright 只保一条关键冒烟流程。

### 后端

- Java 17、Spring Boot 3.x、Maven Wrapper。
- Spring Web、Validation、Security、Data JPA、Flyway、Actuator、MySQL Driver、springdoc-openapi。
- 模块化单体，按 `auth`、`catalog`、`menu`、`preference`、`order`、`inventory`、`admin` 分包。
- 单节点初版使用 Spring Security 服务端 HTTP Session 和 `HttpOnly` Cookie，满足浏览器刷新保持登录；多节点和服务重启保持会话延后到 Spring Session JDBC。
- 密码使用 BCrypt；权限同时在 HTTP 规则和服务层校验。
- 错误统一返回稳定 `code`、英文 `message`、`correlationId` 和可选字段错误。

### 数据与运行

- Docker Compose 启动独立 MySQL 8，使用命名卷持久化。
- Flyway 管理结构与基础数据；应用启动时补齐“当前业务日期”的演示菜单，避免固定日期失效。
- 不使用 Redis、H2、消息队列或额外对象存储。
- 开发时 Vite 代理 `/api` 到 Spring Boot，浏览器保持同源交互。

## 4. 初版数据模型

为节省建模和 CRUD 时间，只有需要关联查询和并发控制的数据做规范化表；菜品配置和订单选项快照使用 MySQL JSON。

| 表 | 关键字段与约束 |
| --- | --- |
| `users` | email 唯一、BCrypt 密码、`EMPLOYEE/ADMIN` 角色 |
| `user_preferences` | user 唯一，过敏原/菜系 JSON，辣度、口味、预算上下限 |
| `dishes` | 英文名称/描述、`DECIMAL(12,2)` 价格、分类、蛋白质、过敏原 JSON、辣度、选项定义 JSON、图片 URL、上架状态 |
| `daily_menu_items` | 日期 + dish 唯一，初始库存、剩余库存、版本字段 |
| `orders` | 订单号、用户、日期、餐次、状态、地址、总额、幂等键、可空 `active_slot_key` 唯一 |
| `order_items` | 菜品 ID、名称/描述/单价快照、数量、选项 JSON、行小计 |

初版不单建 `order_idempotency` 表：`orders` 上使用唯一 `(user_id, idempotency_key)`。`active_slot_key` 在 `Pending/Confirmed` 时写入 `userId|date|meal`，取消时置空，用 MySQL 唯一约束作为重复有效餐次的最终保护。

## 5. 核心业务实现

### 认证

注册写入 MySQL，登录建立服务端 Session。Cookie 在生产配置 `Secure`、`HttpOnly`、`SameSite=Lax`。初版通过 Vite 同源代理降低跨域和 Cookie 配置成本。

### 菜品选项与价格

前端只提交 `dishId`、选项 ID 和数量。后端从菜品 JSON 定义中校验必选组、单选/多选规则和附加价，并使用 `BigDecimal` 重算单价、行小计和总价。API 金额以固定两位小数字符串传输。

### 下单事务

1. 检查用户、幂等键和请求结构。
2. 校验总份数不超过 5，并根据服务端时钟规范化日期和餐次。
3. 按菜品 ID 排序，用 `SELECT ... FOR UPDATE` 锁定当日菜单行。
4. 检查上架、菜单资格、库存和选项，后端重算价格。
5. 写入订单及不可变快照，扣减库存，提交事务。
6. 幂等键冲突时查询并返回已有订单；有效餐次冲突时返回已有订单 ID。

### 取消事务

锁定订单，校验本人且状态为 `Pending`，置为 `Cancelled`，逐项恢复库存并清空 `active_slot_key`。重复取消返回稳定冲突，不能再次恢复库存。

### 库存刷新

初版不做 SSE。菜单、下单成功、取消成功和 Console 更新后由 TanStack Query 主动失效并重新请求。数据库事务仍然提供真实防超卖。

## 6. API 最小集合

| 模块 | API |
| --- | --- |
| Auth | `POST /api/v1/auth/register`, `login`, `logout`; `GET /auth/me` |
| Menu | `GET /api/v1/menus/{date}`, `GET /dishes/{id}` |
| Preferences | `GET/PUT /api/v1/me/preferences` |
| Orders | `POST /api/v1/orders`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel` |
| Console dishes | `GET/POST /api/v1/console/dishes`, `PUT /console/dishes/{id}`, `PATCH .../status` |
| Console menu | `GET/PUT /api/v1/console/menus/{date}` |
| Operations | `GET /actuator/health`, OpenAPI/Swagger UI |

管理员图片初版允许选择预置素材路径或填写图片 URL。multipart 本地上传属于 P1，不能阻塞菜品管理闭环。

## 7. 简化但不造假的范围

可以简化：

- 演示账号和九个菜品由种子固定生成。
- 菜品配置与过敏原使用结构化 JSON，不在初版拆成多张字典表。
- 地址直接保存在订单；历史地址可以从订单去重读取。
- 菜单使用普通响应式网格，不提前做虚拟列表。
- Console 初版只做必要表单，不做批量导入、复杂审计和订单运营。
- 图片先使用预置路径/URL；真实上传有时间再补。

不能简化：

- 不能用 H2 或前端 mock 代替 MySQL。
- 不能把明文密码、前端价格或前端角色当真值。
- 不能跳过幂等、重复餐次和库存事务。
- 不能用固定过期日期冒充当天可演示菜单。
- 不能把未完成的 AI、看板或 SSE 显示成可用功能。

## 8. 测试最小集

- 后端单元测试：价格、总量、截单切换、状态转换。
- MySQL 集成测试：幂等重复请求、有效餐次唯一、最后库存竞争、重复取消。
- 前端测试：购物车配置身份、金额、5 份限制、过敏原确认。
- Playwright：一条员工完整下单/取消流程，以及一个管理员修改菜单后的可见性检查。

测试失败时的修复顺序：数据库事务和数据错误 > 权限 > 下单主流程 > 页面阻断 > 非关键视觉问题。

## 9. 完成定义

企业级初版不是指功能名称全部出现，而是以下事实同时成立：

- 新环境可以按 README 启动独立 MySQL、后端和前端。
- 浏览器能走完注册/登录、菜单、定制、购物车、下单、订单详情和取消。
- 管理员能修改菜品与每日菜单，并真实影响员工端数据。
- 订单、快照、库存和偏好真实保存在 MySQL。
- 重复提交不重复下单，并发库存不会为负。
- 核心自动测试和真实 MySQL 冒烟通过。
- 所有可见产品文案和种子数据均为英文。
- README、OpenAPI、已知问题和 AI 对话导出完整。

详细执行顺序和逐项校验以项目根目录 `TODO.md` 为准。
