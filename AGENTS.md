# AGENTS.md

## 1. 项目定位

本仓库实现一个“学生课程管理系统”的前后端完整本机可运行版本。

本项目不是单体应用，而是严格按照微服务架构实现：

- 每个业务能力拆分为独立服务
- 每个服务独立启动、独立配置、独立端口、独立数据存储逻辑库
- 外部请求统一进入 API Gateway
- 服务之间通过 OpenFeign + 服务注册发现进行调用
- 服务实例由 Eureka 注册和发现
- 服务间调用使用 Spring Cloud LoadBalancer
- 服务故障通过 Resilience4j 熔断降级处理
- 前端只访问 gateway-service，不直接访问任何业务服务
- 数据库层面禁止跨库外键，跨服务一致性通过服务接口校验完成

当前阶段目标是“本机完整运行和演示”，不实现 Kubernetes、Jenkins、Prometheus/Grafana、ELK、Service Mesh 等复杂部署内容。可以预留目录，但不要把这些作为本阶段实现目标。

---

## 2. Java 17 适配原则

用户本机只有 Java 17，因此不要使用旧版技术组合：

- 不要使用 Spring Boot 2.0.9.RELEASE
- 不要使用 Spring Cloud Finchley.SR2
- 不要使用 Ribbon
- 不要使用 Hystrix
- 不要使用 `javax.*`

Java 17 下统一采用：

| 类别 | 选型 |
|---|---|
| JDK | Java 17 |
| 构建工具 | Maven 3.8+ |
| 后端框架 | Spring Boot 3.3.4 |
| 微服务框架 | Spring Cloud 2023.0.3 |
| 注册中心 | Eureka Server / Eureka Client |
| 服务调用 | Spring Cloud OpenFeign |
| 负载均衡 | Spring Cloud LoadBalancer |
| 熔断降级 | Spring Cloud CircuitBreaker + Resilience4j |
| API 网关 | Spring Cloud Gateway |
| 数据库 | MySQL 8.0 |
| 持久层 | MyBatis Spring Boot Starter 3.x + HikariCP |
| 鉴权 | JWT |
| 前端 | Vue 3 + Vite + Vue Router + Axios |
| 接口调试 | curl / Postman / Swagger UI 可选 |

注意：

1. Spring Boot 3 使用 `jakarta.*` 包，不要写旧的 `javax.*`。
2. Gateway 是 WebFlux 响应式网关，gateway-service 不要引入 `spring-boot-starter-web`。
3. 普通业务服务使用 `spring-boot-starter-web`。
4. 熔断降级使用 Resilience4j，不使用 Hystrix。
5. 负载均衡使用 Spring Cloud LoadBalancer，不使用 Ribbon。
6. 服务间调用必须通过服务名和注册中心，不要在代码中硬编码其他服务的具体端口地址。

---

## 3. 项目结构

项目采用前后端同仓库结构：

```text
student-course-cloud
├── pom.xml
├── common-module
├── eureka-service
├── gateway-service
├── student-service
├── course-service
├── teacher-service
├── enrollment-service
├── frontend
├── scripts
│   ├── sql
│   ├── win
│   └── unix
└── docs
    ├── local-run.md
    ├── local-test-flow.md
    ├── api.md
    └── architecture.md
```

后端是 Maven 聚合项目，前端是独立的 Vue 3 + Vite 项目。

---

## 4. 后端模块职责

### 4.1 common-module

公共模块，不作为独立服务运行。

必须包含：

- 统一返回结构 `Result<T>`
- 错误码枚举 `ErrorCode`
- 业务异常 `BusinessException`
- 全局异常响应所需基础结构
- JWT 工具类 `JwtUtil`
- 角色枚举 `RoleEnum`
- 用户上下文 DTO
- 登录、注册、鉴权相关 DTO
- 基础分页对象，可选

禁止在 common-module 中写具体业务逻辑。

---

### 4.2 eureka-service

服务注册中心。

要求：

- 服务名：`eureka-service`
- 端口：`8761`
- 使用 `@EnableEurekaServer`
- 本机访问 `http://localhost:8761` 能看到控制台
- 不向自己注册
- 不从自己拉取注册表

---

### 4.3 gateway-service

系统唯一后端入口。

要求：

- 服务名：`gateway-service`
- 端口：`8080`
- 注册到 Eureka
- 使用 Spring Cloud Gateway
- 使用 `lb://服务名` 转发请求
- 配置 CORS，允许本机前端 `http://localhost:5173`
- 实现 JWT 全局过滤器
- 登录、注册接口放行
- 受保护接口必须校验 `Authorization: Bearer <token>`
- 校验成功后向下游透传：
  - `X-User-Id`
  - `X-Username`
  - `X-Role`
- 未登录返回 401
- token 无效返回 401
- 权限不足返回 403
- 网关层只做通用鉴权和路由，不写具体业务逻辑

路由规则：

| 前端访问路径 | 转发目标 |
|---|---|
| `/api/auth/**` | `lb://student-service` |
| `/api/students/**` | `lb://student-service` |
| `/api/courses/**` | `lb://course-service` |
| `/api/teachers/**` | `lb://teacher-service` |
| `/api/enrollments/**` | `lb://enrollment-service` |

权限规则：

| 资源 | 学生 | 教师 | 管理员 |
|---|---|---|---|
| 注册、登录 | 允许 | 允许 | 允许 |
| 查询课程 | 允许 | 允许 | 允许 |
| 选课、退课、查自己课表 | 允许 | 不允许 | 可管理 |
| 学生查看自己信息 | 允许 | 不允许 | 可管理 |
| 教师查看自己任课 | 不允许 | 允许 | 可管理 |
| 课程新增、修改、删除 | 不允许 | 不允许 | 允许 |
| 教师新增、修改、删除 | 不允许 | 不允许 | 允许 |
| 为课程分配教师 | 不允许 | 不允许 | 允许 |

---

### 4.4 student-service

学生与账户服务，负责用户注册、登录、学生信息管理。

要求：

- 服务名：`student-service`
- 端口：`8081`
- 逻辑库：`db_student`
- 注册到 Eureka
- 不直接访问其他服务数据库
- 写操作使用本地事务

最小数据表：

```text
user_account(
  id,
  username,
  password_hash,
  role,
  related_id,
  status,
  create_time,
  update_time
)

student(
  student_id,
  student_no,
  name,
  major,
  grade,
  phone,
  email,
  status,
  create_time,
  update_time
)
```

最小接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/auth/register` | 学生注册 |
| POST | `/auth/login` | 登录并返回 JWT |
| GET | `/students/{studentId}` | 查询学生信息 |
| PUT | `/students/{studentId}` | 修改学生信息 |
| GET | `/students` | 管理员查询学生列表 |
| GET | `/internal/students/{studentId}/status` | 供 enrollment-service 校验学生状态 |

---

### 4.5 course-service

课程服务，负责课程与课程时间安排。

要求：

- 服务名：`course-service`
- 端口：`8082`
- 逻辑库：`db_course`
- 注册到 Eureka
- 不直接访问其他服务数据库
- 写操作使用本地事务

最小数据表：

```text
course(
  course_id,
  course_code,
  course_name,
  credit,
  capacity,
  selected_count,
  status,
  description,
  create_time,
  update_time
)

course_schedule(
  schedule_id,
  course_id,
  weekday,
  start_time,
  end_time,
  classroom,
  create_time,
  update_time
)
```

最小接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/courses` | 新增课程 |
| PUT | `/courses/{courseId}` | 修改课程 |
| DELETE | `/courses/{courseId}` | 删除或禁用课程 |
| GET | `/courses/{courseId}` | 查询课程详情 |
| GET | `/courses` | 查询课程列表 |
| POST | `/courses/{courseId}/schedules` | 新增课程时间 |
| GET | `/courses/{courseId}/schedules` | 查询课程时间 |
| POST | `/internal/courses/{courseId}/increase-selected-count` | 选课成功后增加已选人数 |
| POST | `/internal/courses/{courseId}/decrease-selected-count` | 退课成功后减少已选人数 |
| GET | `/internal/courses/{courseId}/check-capacity` | 校验课程容量 |
| GET | `/internal/courses/{courseId}/schedule` | 查询课程时间，供冲突检测 |

---

### 4.6 teacher-service

教师服务，负责教师信息和课程教师分配。

要求：

- 服务名：`teacher-service`
- 端口：`8083`
- 逻辑库：`db_teacher`
- 注册到 Eureka
- 不直接访问其他服务数据库
- 写操作使用本地事务

最小数据表：

```text
teacher(
  teacher_id,
  teacher_no,
  name,
  title,
  phone,
  email,
  status,
  create_time,
  update_time
)

teacher_course_assignment(
  id,
  teacher_id,
  course_id,
  status,
  create_time,
  update_time
)
```

最小接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/teachers` | 新增教师 |
| PUT | `/teachers/{teacherId}` | 修改教师 |
| DELETE | `/teachers/{teacherId}` | 删除或禁用教师 |
| GET | `/teachers/{teacherId}` | 查询教师详情 |
| GET | `/teachers` | 查询教师列表 |
| POST | `/teachers/{teacherId}/courses/{courseId}` | 给课程分配教师 |
| DELETE | `/teachers/{teacherId}/courses/{courseId}` | 取消教师课程分配 |
| GET | `/teachers/{teacherId}/courses` | 查询教师任课 |
| GET | `/internal/courses/{courseId}/teacher-assigned` | 校验课程是否已分配教师 |

---

### 4.7 enrollment-service

选课服务，负责选课、退课和学生课表。

要求：

- 服务名：`enrollment-service`
- 端口：`8084`
- 逻辑库：`db_enrollment`
- 注册到 Eureka
- 不直接访问其他服务数据库
- 写操作使用本地事务
- 通过 OpenFeign 调用 student-service、course-service、teacher-service
- Feign 必须配置 fallback
- 服务异常时返回可理解的降级提示

最小数据表：

```text
enrollment(
  enrollment_id,
  student_id,
  course_id,
  status,
  create_time,
  update_time
)
```

最小接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/enrollments` | 学生选课 |
| DELETE | `/enrollments/{enrollmentId}` | 学生退课 |
| GET | `/enrollments/students/{studentId}` | 查询学生选课记录 |
| GET | `/enrollments/students/{studentId}/timetable` | 查询学生课表 |
| GET | `/enrollments` | 管理员查询全部选课记录 |

选课必须校验：

1. 学生存在且状态正常
2. 课程存在且状态可选
3. 课程未满
4. 课程已分配教师
5. 学生不能重复选择同一课程
6. 学生课程时间不能冲突
7. 所有校验通过后写入本服务 enrollment 表
8. 选课成功后调用 course-service 增加已选人数
9. 退课成功后调用 course-service 减少已选人数

说明：

- 课程容量计数和选课记录分属不同服务，本阶段不用分布式事务框架。
- 代码中要清楚说明这里采用的是“本地事务 + 服务接口校验 + 简化补偿”的课堂项目实现方式。
- 如果增加已选人数失败，应返回明确错误，不要静默成功。

---

## 5. 数据库设计要求

使用一个 MySQL 8.0 实例，多个逻辑数据库：

```text
db_student
db_course
db_teacher
db_enrollment
```

严格要求：

1. 每个服务只访问自己的逻辑库。
2. 禁止跨库外键。
3. 禁止在一个服务中直接查询另一个服务的表。
4. 跨服务校验通过 OpenFeign 调用完成。
5. 每个服务的 SQL 独立存放。
6. 每个服务写操作使用本地事务。
7. 初始化 SQL 放在 `scripts/sql/` 下。

建议 SQL 文件：

```text
scripts/sql/00-create-databases.sql
scripts/sql/01-student-service.sql
scripts/sql/02-course-service.sql
scripts/sql/03-teacher-service.sql
scripts/sql/04-enrollment-service.sql
scripts/sql/05-demo-data.sql
```

---

## 6. 前端要求

前端目录：`frontend`

技术栈：

- Vue 3
- Vite
- Vue Router
- Axios
- 原生 CSS 或 Element Plus

优先用原生 CSS，避免复杂依赖。如果使用 Element Plus，只能用于表格、表单、按钮等基础组件，不要过度设计。

### 6.1 前端访问规则

前端只能访问：

```text
http://localhost:8080
```

即只访问 gateway-service。

禁止前端直接访问：

```text
http://localhost:8081
http://localhost:8082
http://localhost:8083
http://localhost:8084
```

### 6.2 前端目录建议

```text
frontend
├── package.json
├── index.html
├── vite.config.js
└── src
    ├── main.js
    ├── App.vue
    ├── router
    │   └── index.js
    ├── api
    │   ├── request.js
    │   ├── auth.js
    │   ├── student.js
    │   ├── course.js
    │   ├── teacher.js
    │   └── enrollment.js
    ├── views
    │   ├── LoginView.vue
    │   ├── RegisterView.vue
    │   ├── StudentLayout.vue
    │   ├── CourseListView.vue
    │   ├── MyTimetableView.vue
    │   ├── AdminLayout.vue
    │   ├── AdminCourseView.vue
    │   ├── AdminTeacherView.vue
    │   ├── AdminStudentView.vue
    │   └── AdminEnrollmentView.vue
    └── assets
        └── style.css
```

### 6.3 页面功能

必须实现：

1. 登录页
   - 用户名、密码
   - 调用 `/api/auth/login`
   - 保存 token、role、userId 到 localStorage
   - 根据角色跳转到对应页面

2. 注册页
   - 学号、姓名、专业、年级、手机号、邮箱、用户名、密码
   - 调用 `/api/auth/register`

3. 学生端课程列表页
   - 查询课程列表
   - 展示课程名、学分、容量、已选人数、状态
   - 点击“选课”
   - 展示错误信息，如课程已满、时间冲突、服务降级

4. 学生端我的课表页
   - 查询当前登录学生课表
   - 展示课程、星期、开始时间、结束时间、教室
   - 支持退课

5. 管理员课程管理页
   - 新增课程
   - 修改课程
   - 删除或禁用课程
   - 新增课程时间
   - 查看课程时间

6. 管理员教师管理页
   - 新增教师
   - 修改教师
   - 删除或禁用教师
   - 为课程分配教师

7. 管理员学生管理页
   - 查看学生列表
   - 查看学生详情

8. 管理员选课记录页
   - 查看全部选课记录
   - 可作为演示系统闭环结果

### 6.4 前端工程要求

1. `src/api/request.js` 统一封装 Axios。
2. baseURL 使用 `http://localhost:8080`。
3. 请求拦截器自动加 `Authorization: Bearer <token>`。
4. 响应拦截器统一处理 401、403、500。
5. 路由守卫检查登录状态。
6. 页面不能依赖 Mock 数据作为最终结果。
7. 所有核心按钮要有 loading 或错误提示。
8. UI 简洁清楚，适合课堂演示。

---

## 7. 本机运行端口

| 服务 | 端口 |
|---|---|
| frontend | 5173 |
| gateway-service | 8080 |
| student-service | 8081 |
| course-service | 8082 |
| teacher-service | 8083 |
| enrollment-service | 8084 |
| eureka-service | 8761 |
| MySQL | 3306 |

---

## 8. 配置要求

每个服务必须有独立 `application.yml`。

必须配置：

- `spring.application.name`
- `server.port`
- `eureka.client.service-url.defaultZone`
- 数据库连接
- MyBatis mapper 路径
- Actuator health
- 日志级别

enrollment-service 必须配置：

- Feign 启用
- CircuitBreaker 启用
- Resilience4j 基础超时、熔断配置

gateway-service 必须配置：

- 路由
- CORS
- JWT 密钥
- 放行路径

---

## 9. 文档要求

必须生成：

```text
docs/architecture.md
docs/api.md
docs/local-run.md
docs/local-test-flow.md
```

### architecture.md

写清楚：

- 为什么这是微服务架构
- 服务拆分依据
- 每个服务职责
- 数据自治设计
- 注册发现
- 网关统一入口
- 服务间调用
- 负载均衡
- 熔断降级
- 前后端交互流程

### api.md

列出所有接口：

- 请求方法
- 路径
- 权限
- 请求体示例
- 响应体示例

### local-run.md

写清楚：

- 环境要求
- MySQL 初始化
- 后端启动顺序
- 前端启动方式
- 端口说明
- 常见错误

### local-test-flow.md

写清楚完整验收流程：

1. 启动 MySQL
2. 执行 SQL
3. 启动 Eureka
4. 启动各业务服务
5. 启动 Gateway
6. 启动前端
7. 注册学生
8. 登录
9. 管理员创建教师
10. 管理员创建课程
11. 管理员创建课程时间
12. 管理员分配教师
13. 学生选课
14. 学生查询课表
15. 学生退课
16. 查看 Eureka 注册情况
17. 启动 course-service 第二实例演示负载均衡
18. 停止 course-service 演示降级

---

## 10. 本机验收标准

最终必须满足：

1. Maven 整体编译通过。
2. 前端 `npm install`、`npm run dev` 能启动。
3. Eureka 页面能看到 gateway、student、course、teacher、enrollment 服务。
4. 所有外部接口通过 gateway 访问。
5. 前端能完成注册、登录、课程管理、教师管理、教师分配、选课、课表查询、退课。
6. student-service 只访问 `db_student`。
7. course-service 只访问 `db_course`。
8. teacher-service 只访问 `db_teacher`。
9. enrollment-service 只访问 `db_enrollment`。
10. enrollment-service 通过 Feign 调用其他服务完成选课校验。
11. 关闭 course-service 后，选课接口能返回降级提示。
12. 启动两个 course-service 实例后，服务调用仍能正常完成。
13. 文档与实际代码一致。

---

## 11. 禁止事项

严格禁止：

- 使用 Nacos
- 使用 Redis
- 使用 RabbitMQ/Kafka
- 使用 Ribbon
- 使用 Hystrix
- 使用 Seata 或其他分布式事务框架
- 实现 Kubernetes 部署
- 实现 Jenkins 流水线
- 引入 Service Mesh
- 后端写成单体应用
- 一个服务直接访问另一个服务的数据库
- 建立跨库外键
- 前端绕过 gateway 直接访问业务服务
- 用 Mock 数据替代最终后端接口
- 为了省事把所有表放在一个业务库里

---

## 12. 每次完成任务后的输出格式

每次完成一个阶段后，请输出：

1. 本阶段完成的文件清单
2. 本阶段实现的能力
3. 如何启动或验证
4. 关键接口路径
5. 是否符合微服务约束
6. 当前未完成项
7. 下一步建议
