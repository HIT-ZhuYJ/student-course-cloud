# 学生课程管理系统架构说明

## 1. 系统目标

本项目实现一个本机可运行、可演示的学生课程管理系统，用于展示前后端分离与 Spring Cloud 微服务架构的基本实践。系统面向课堂演示场景，核心目标包括：

1. 学生可以注册、登录、查询课程、选课、退课和查看个人课表。
2. 管理员可以管理课程、课程时间、教师、学生和选课记录。
3. 教师可以查看自己的任课信息。
4. 所有外部请求统一从 API Gateway 进入，前端不直接访问任何业务服务。
5. 各业务服务独立启动、独立端口、独立配置、独立逻辑数据库。
6. 服务之间通过注册中心发现服务，通过 OpenFeign 调用接口完成协作。
7. 在本机环境中完成注册发现、网关转发、JWT 鉴权、服务调用、负载均衡和熔断降级演示。

本架构文档重点说明学生课程管理系统的微服务拆分、数据自治和本机运行链路。Kubernetes 三节点部署、Jenkins CI/CD 与观测组件的验收说明见 `docs/k8s-final-deployment-plan.md` 和 `docs/k8s-scheduling-and-master-jenkins.md`。

## 2. 为什么本项目是微服务架构

本项目不是单体应用，而是按照微服务架构拆分和实现。判断依据如下：

1. 系统被拆分为多个独立服务，包括 `student-service`、`course-service`、`teacher-service`、`enrollment-service`、`gateway-service` 和 `eureka-service`。
2. 每个业务服务都有独立的启动类、独立端口、独立 `application.yml` 配置。
3. 每个业务服务只访问自己的逻辑数据库，不直接访问其他服务的数据表。
4. 外部请求统一进入 `gateway-service`，由网关根据路径转发到对应服务。
5. 服务实例注册到 `eureka-service`，调用方通过服务名发现目标服务。
6. 服务间通信通过 OpenFeign 完成，不在代码中硬编码其他服务的主机和端口。
7. 负载均衡使用 Spring Cloud LoadBalancer，故障处理使用 Resilience4j 熔断降级。
8. 跨服务业务一致性不依赖跨库事务，而是通过服务接口校验、本地事务和简化补偿方式完成。

因此，本项目符合微服务架构中“服务自治、数据库自治、服务发现、统一入口、服务间远程调用”的基本特征。

## 3. 服务拆分依据

本系统按照业务能力进行拆分，每个服务负责一个清晰的业务边界：

| 服务 | 拆分依据 |
|---|---|
| `common-module` | 公共代码复用，不作为独立服务运行 |
| `eureka-service` | 服务注册中心，负责服务注册与发现 |
| `gateway-service` | 系统统一入口，负责路由、CORS、JWT 鉴权和第一层权限控制 |
| `student-service` | 学生与账号能力，包括注册、登录、学生信息管理 |
| `course-service` | 课程与课程时间能力，包括容量、状态、课程安排 |
| `teacher-service` | 教师信息与课程教师分配能力 |
| `enrollment-service` | 选课、退课、课表和选课记录能力 |
| `frontend` | Vue 3 前端工程，只访问 gateway-service |

这种拆分方式使每个服务的职责集中，减少模块之间的直接耦合，也便于分别启动、测试和演示。

## 4. 每个服务职责

### 4.1 common-module

`common-module` 是公共模块，不独立启动。它提供后端服务共享的基础能力：

- 统一响应结构 `Result<T>`
- 错误码枚举 `ErrorCode`
- 业务异常 `BusinessException`
- 全局错误响应结构
- JWT 工具类 `JwtUtil`
- 角色枚举 `RoleEnum`
- 登录、注册、用户上下文等公共 DTO
- 分页返回对象 `PageResult<T>`

该模块不包含具体业务逻辑。

### 4.2 eureka-service

`eureka-service` 是注册中心，端口为 `8761`。它使用 Eureka Server，负责接收其他服务注册，并向调用方提供服务发现能力。

### 4.3 gateway-service

`gateway-service` 是系统唯一后端入口，端口为 `8080`。它使用 Spring Cloud Gateway，主要职责包括：

- 接收前端所有 `/api/**` 请求
- 根据路径转发到对应业务服务
- 配置 CORS，允许 `http://localhost:5173`
- 校验 JWT
- 放行登录和注册接口
- 校验通过后向下游服务透传 `X-User-Id`、`X-Username`、`X-Role`、`X-Related-Id`
- 对学生、教师、管理员做第一层权限控制

Gateway 是 WebFlux 响应式网关，不引入 `spring-boot-starter-web`。

### 4.4 student-service

`student-service` 是学生与账号服务，端口为 `8081`，只访问 `db_student`。主要职责包括：

- 学生注册
- 用户登录并签发 JWT
- 查询学生详情
- 修改学生信息
- 管理员查询学生列表
- 为选课服务提供学生状态校验接口

### 4.5 course-service

`course-service` 是课程服务，端口为 `8082`，只访问 `db_course`。主要职责包括：

- 新增、修改、禁用课程
- 查询课程详情和课程列表
- 新增和查询课程时间
- 校验课程是否可选、容量是否未满
- 选课成功后增加已选人数
- 退课成功后减少已选人数
- 为选课服务提供课程时间，用于时间冲突检测

### 4.6 teacher-service

`teacher-service` 是教师服务，端口为 `8083`，只访问 `db_teacher`。主要职责包括：

- 新增、修改、禁用教师
- 查询教师详情和教师列表
- 为课程分配教师
- 取消课程教师分配
- 查询教师任课
- 为选课服务提供课程是否已分配教师的校验接口

教师服务只记录 `courseId`，不直接访问课程服务数据库。

### 4.7 enrollment-service

`enrollment-service` 是选课服务，端口为 `8084`，只访问 `db_enrollment`。主要职责包括：

- 学生选课
- 学生退课
- 查询学生选课记录
- 查询学生课表
- 管理员查询全部选课记录
- 通过 OpenFeign 调用学生、课程、教师服务完成选课校验

该服务启用 OpenFeign、Spring Cloud CircuitBreaker 和 Resilience4j。当远程服务不可用时，Feign fallback 返回明确的服务不可用提示。

### 4.8 frontend

`frontend` 是 Vue 3 + Vite 前端工程，端口为 `5173`。它只访问：

```text
http://localhost:8080
```

前端不直接访问 `student-service`、`course-service`、`teacher-service`、`enrollment-service` 的端口。

## 5. 整体架构

系统整体采用“前端 + 网关 + 注册中心 + 多业务服务 + 多逻辑数据库”的结构。

```text
Vue 3 Frontend
    |
    | HTTP /api/**
    v
Spring Cloud Gateway :8080
    |
    | lb://service-name
    v
+---------------------+---------------------+---------------------+------------------------+
| student-service     | course-service      | teacher-service     | enrollment-service     |
| :8081               | :8082               | :8083               | :8084                  |
| db_student          | db_course           | db_teacher          | db_enrollment          |
+---------------------+---------------------+---------------------+------------------------+
           \                 |                    |                    /
            \                |                    |                   /
             v               v                    v                  v
                         Eureka Server :8761
```

各服务启动后会作为 Eureka Client 注册到 Eureka Server。Gateway 和需要调用其他服务的业务服务通过服务名访问目标服务，底层由服务发现和负载均衡完成实例选择。

## 6. 数据自治设计

本系统采用数据库自治设计。每个服务拥有自己的逻辑数据库：

| 服务 | 逻辑库 | 主要表 |
|---|---|---|
| `student-service` | `db_student` | `user_account`、`student` |
| `course-service` | `db_course` | `course`、`course_schedule` |
| `teacher-service` | `db_teacher` | `teacher`、`teacher_course_assignment` |
| `enrollment-service` | `db_enrollment` | `enrollment` |

数据层约束如下：

1. 每个服务只连接自己的逻辑库。
2. 禁止跨库外键。
3. 禁止一个服务直接查询另一个服务的数据表。
4. 跨服务校验通过服务接口完成。
5. 服务内写操作使用本地事务。

例如，`enrollment-service` 在选课时不会直接查询 `db_student.student` 或 `db_course.course`，而是通过 Feign 调用 `student-service` 和 `course-service` 暴露的内部接口完成校验。

这种设计使每个服务的数据边界清晰，符合微服务架构中“数据归服务所有”的原则。

## 7. 服务注册发现

本项目使用 Eureka 实现服务注册发现。

### 7.1 Eureka Server

`eureka-service` 作为 Eureka Server，端口为 `8761`。它负责维护服务实例注册表，可以通过浏览器访问：

```text
http://localhost:8761
```

在控制台中可以看到已注册的 `gateway-service`、`student-service`、`course-service`、`teacher-service` 和 `enrollment-service`。

### 7.2 Eureka Client

除注册中心以外，各后端服务都作为 Eureka Client 注册到 Eureka Server。服务启动后会向 Eureka 上报自己的服务名、实例地址和健康状态。

服务名示例：

```text
student-service
course-service
teacher-service
enrollment-service
gateway-service
```

调用方不需要知道目标服务的具体端口，只需要使用服务名即可。

## 8. 服务间调用

服务间调用使用 Spring Cloud OpenFeign。典型场景是 `enrollment-service` 在选课时调用其他服务：

- 调用 `student-service` 校验学生是否存在且状态正常
- 调用 `course-service` 校验课程容量、获取课程时间、增加或减少已选人数
- 调用 `teacher-service` 校验课程是否已经分配教师

Feign Client 使用服务名声明目标服务，例如：

```text
student-service
course-service
teacher-service
```

这种方式避免在代码中硬编码 `localhost:8081`、`localhost:8082` 等具体地址，也便于后续扩展多个服务实例。

## 9. 负载均衡

本项目使用 Spring Cloud LoadBalancer，不使用 Ribbon。

当调用方通过服务名访问目标服务时，Spring Cloud LoadBalancer 会从 Eureka 注册表中获取该服务的可用实例，并选择一个实例发起请求。

例如，如果启动两个 `course-service` 实例，`enrollment-service` 调用 `course-service` 时仍然只需要使用服务名：

```text
lb://course-service
```

具体请求会由负载均衡组件分配到可用实例。

## 10. 熔断降级

本项目使用 Spring Cloud CircuitBreaker + Resilience4j 实现熔断降级，不使用 Hystrix。

在 `enrollment-service` 中，Feign Client 都配置了 fallback。当学生服务、课程服务或教师服务不可用时，fallback 会返回明确的服务不可用信息，而不是让调用方看到不友好的底层异常。

典型降级场景：

1. 学生点击选课。
2. `enrollment-service` 调用 `course-service` 校验课程容量。
3. 如果 `course-service` 已停止或超时，Feign fallback 返回“课程服务不可用”。
4. `enrollment-service` 返回选课失败结果。
5. 前端页面展示清晰错误提示。

这种方式适合课堂演示服务故障处理流程。

## 11. 统一入口

系统统一入口是 `gateway-service`，使用 Spring Cloud Gateway。

路由规则如下：

| 前端访问路径 | 转发目标 |
|---|---|
| `/api/auth/**` | `lb://student-service` |
| `/api/students/**` | `lb://student-service` |
| `/api/courses/**` | `lb://course-service` |
| `/api/teachers/**` | `lb://teacher-service` |
| `/api/enrollments/**` | `lb://enrollment-service` |

前端只需要知道 Gateway 地址：

```text
http://localhost:8080
```

业务服务的端口不会暴露给前端调用。

## 12. 鉴权设计

系统使用 JWT 实现登录态和基础权限控制。

### 12.1 登录签发 token

用户通过 `/api/auth/login` 登录。请求经 Gateway 转发到 `student-service`，学生服务校验账号密码后生成 JWT 并返回给前端。

前端保存：

- `token`
- `role`
- `userId`
- `username`

后续请求自动携带：

```text
Authorization: Bearer <token>
```

### 12.2 Gateway 校验 token

Gateway 对受保护接口执行 JWT 校验：

1. 未携带 token 返回 `401`。
2. token 无效或过期返回 `401`。
3. 权限不足返回 `403`。
4. 校验成功后向下游透传用户信息。

下游请求头包括：

```text
X-User-Id
X-Username
X-Role
X-Related-Id
```

Gateway 负责第一层通用权限控制，业务服务保持业务职责清晰。

## 13. 前后端交互流程

前端与后端的交互流程如下：

1. 用户访问 Vue 前端页面。
2. 前端通过 Axios 请求 `http://localhost:8080/api/**`。
3. Axios 请求拦截器自动添加 JWT。
4. Gateway 接收请求并进行 CORS、路由、JWT 和权限校验。
5. Gateway 根据路径转发到对应业务服务。
6. 业务服务处理请求，只访问自己的数据库。
7. 如果业务服务需要其他服务数据，则通过 OpenFeign 调用目标服务接口。
8. 业务服务返回统一 `Result<T>` 响应。
9. Gateway 将响应返回给前端。
10. 前端根据响应展示成功结果或错误提示。

整个流程中，前端不知道业务服务的具体地址，业务服务之间也不直接访问彼此数据库。

## 14. 选课业务完整调用链

选课是本系统最能体现微服务协作的业务流程。

调用入口：

```text
POST /api/enrollments
```

完整调用链如下：

1. 学生在前端课程列表点击“选课”。
2. 前端请求 Gateway：`POST http://localhost:8080/api/enrollments`。
3. Gateway 校验 JWT，确认当前用户具有学生选课权限。
4. Gateway 将请求转发到 `enrollment-service`。
5. `enrollment-service` 调用 `student-service`，校验学生存在且状态正常。
6. `enrollment-service` 调用 `course-service`，校验课程存在、状态可选、容量未满。
7. `enrollment-service` 调用 `teacher-service`，校验课程已分配教师。
8. `enrollment-service` 查询自己的 `db_enrollment.enrollment` 表，检查该学生是否重复选择同一课程。
9. `enrollment-service` 调用 `course-service` 获取当前课程时间。
10. `enrollment-service` 查询该学生在本服务中已有的有效选课记录。
11. `enrollment-service` 对已有课程时间和新课程时间进行冲突检测。
12. 校验通过后，`enrollment-service` 在本地事务中写入选课记录。
13. `enrollment-service` 调用 `course-service` 增加课程已选人数。
14. 如果增加已选人数失败，返回明确错误。
15. 选课成功后返回统一响应给 Gateway。
16. Gateway 将结果返回给前端。
17. 前端展示选课成功或失败原因。

本阶段不引入 Seata 等分布式事务框架。课程容量计数和选课记录分属不同服务，课堂项目中采用“本地事务 + 服务接口校验 + 简化补偿”的方式处理跨服务一致性。

## 15. 本机部署拓扑

本机部署时，各服务端口如下：

| 组件 | 端口 | 说明 |
|---|---:|---|
| `frontend` | `5173` | Vue 3 + Vite 前端 |
| `gateway-service` | `8080` | API Gateway，系统统一入口 |
| `student-service` | `8081` | 学生与账号服务 |
| `course-service` | `8082` | 课程服务 |
| `teacher-service` | `8083` | 教师服务 |
| `enrollment-service` | `8084` | 选课服务 |
| `eureka-service` | `8761` | Eureka 注册中心 |
| MySQL | `3306` | 一个 MySQL 实例，多个逻辑库 |

推荐本机启动顺序：

1. 启动 MySQL。
2. 执行 `scripts/sql` 下的初始化 SQL。
3. 启动 `eureka-service`。
4. 启动 `student-service`、`course-service`、`teacher-service`、`enrollment-service`。
5. 启动 `gateway-service`。
6. 启动 `frontend`。
7. 访问 `http://localhost:5173` 进行系统演示。

Eureka 控制台访问地址：

```text
http://localhost:8761
```

前端访问地址：

```text
http://localhost:5173
```

后端统一访问入口：

```text
http://localhost:8080
```
