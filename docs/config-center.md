# Spring Cloud Config 配置中心说明

## 1. 方案选择

本项目新增 `config-service` 作为独立配置中心微服务，使用 Spring Cloud Config Server 的 `native` 模式读取仓库内 `config-repo/` 目录。

选择 `native` 模式的原因：

1. 符合当前“本机完整运行和 Docker 演示”的阶段目标。
2. 不引入 Nacos、Redis、消息队列或额外部署组件。
3. 后续如果需要远程集中管理，可以把 Config Server 后端从 `native` 切换为 Git。

## 2. 模块和端口

| 模块 | 端口 | 说明 |
|---|---:|---|
| `config-service` | `8888` | 配置中心，只负责配置读取和分发 |
| `eureka-service` | `8761` | 服务注册中心 |
| `gateway-service` | `8080` | 后端统一入口 |
| `student-service` | `8081` | 学生与账户服务 |
| `course-service` | `8082` | 课程服务 |
| `teacher-service` | `8083` | 教师服务 |
| `enrollment-service` | `8084` | 选课服务 |

## 3. 配置目录

集中配置文件放在：

```text
config-repo/
├── application.yml
├── application-local.yml
├── application-docker.yml
├── eureka-service.yml
├── gateway-service.yml
├── student-service.yml
├── course-service.yml
├── teacher-service.yml
└── enrollment-service.yml
```

其中：

- `application.yml`：所有服务共享配置，例如 Actuator、Eureka 默认地址、MyBatis、JWT 默认配置。
- `application-local.yml`：本机部署配置，例如 MySQL 使用 `localhost:3306`。
- `application-docker.yml`：Docker 部署配置，例如 MySQL 使用 `mysql:3306`，Eureka 使用 `eureka-service:8761`。
- `<service-name>.yml`：每个微服务自己的端口、数据源、路由、Feign、Resilience4j 等配置。

## 4. 各服务本地配置

除 `config-service` 外，各微服务的 `src/main/resources/application.yml` 只保留最小启动配置：

```yaml
spring:
  application:
    name: student-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
  config:
    import: "configserver:${CONFIG_SERVER_URL:http://localhost:8888}"
```

这样服务启动时会先访问配置中心，再加载自身集中配置。

## 5. 本机启动顺序

```text
MySQL
-> config-service
-> eureka-service
-> student-service / course-service / teacher-service / enrollment-service
-> gateway-service
-> frontend
```

Windows：

```powershell
.\scripts\win\start-all.ps1 -MysqlUser root -MysqlPassword "123888"
```

Linux/macOS：

```bash
MYSQL_USER=root MYSQL_PASSWORD='123888' bash scripts/unix/start-all.sh
```

脚本会让 `config-service` 使用 `native` profile 启动，让其他服务使用 `local` profile 读取配置。

## 6. Docker 启动方式

```bash
docker compose up -d --build
```

Docker 中：

- `config-service` 使用 `native` profile。
- 其他后端服务使用 `docker` profile。
- 其他后端服务通过 `CONFIG_SERVER_URL=http://config-service:8888` 读取配置。

## 7. 验证方式

查看配置中心健康状态：

```bash
curl http://localhost:8888/actuator/health
```

查看某个服务的本机配置：

```bash
curl http://localhost:8888/student-service/local
curl http://localhost:8888/gateway-service/local
```

查看某个服务的 Docker 配置：

```bash
curl http://localhost:8888/student-service/docker
curl http://localhost:8888/enrollment-service/docker
```

## 8. 微服务约束说明

配置中心不会改变服务边界：

1. 每个业务服务仍独立启动、独立端口、独立逻辑数据库。
2. 前端仍只访问 `gateway-service`。
3. 服务注册发现仍由 Eureka 完成。
4. 服务间调用仍由 OpenFeign + Spring Cloud LoadBalancer 完成。
5. `enrollment-service` 仍只通过 Feign 调用其他服务做跨服务校验。
6. 配置中心只管理配置，不写业务逻辑，不访问业务数据库。
