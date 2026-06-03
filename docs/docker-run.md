# Docker 本地一键部署说明

本文档说明如何使用 Docker Compose 在本机启动学生课程管理系统的完整容器化环境，包括：

- MySQL 8.0
- Eureka Server
- gateway-service
- student-service
- course-service
- teacher-service
- enrollment-service
- Nginx 统一入口（镜像内直接包含 Vue 前端静态资源）
- Prometheus
- Grafana
- Elasticsearch
- Logstash
- Kibana

> 说明：这是本机 Docker 演示部署，不是 Kubernetes 或生产部署方案。

## 1. 前置要求

本机需要安装：

- Docker Desktop
- Docker Compose v2

建议 Docker Desktop 至少分配 6GB 内存。ELK 组件比较占资源，如果机器内存不足，可以先临时关闭 `elasticsearch`、`logstash`、`kibana` 三个服务。

## 2. 一键启动

在项目根目录执行：

```bash
docker compose up -d --build
```

如果课堂网络下载 Maven 依赖不稳定，建议先使用顺序构建，避免多个 Maven 容器同时拉取依赖：

Windows PowerShell：

```powershell
$env:COMPOSE_PARALLEL_LIMIT="1"
docker compose up -d --build
```

Linux/macOS Bash：

```bash
COMPOSE_PARALLEL_LIMIT=1 docker compose up -d --build
```

项目已提供 Docker 构建专用 Maven 配置：

```text
docker/maven/settings.xml
```

该配置使用 Maven 镜像源，降低本机 Docker 环境访问 Maven Central 失败的概率。

首次启动会做这些事情：

1. 构建每个后端微服务镜像。
2. 构建 Nginx 镜像，并在镜像构建阶段生成 Vue 前端静态文件。
3. 启动 MySQL，并自动执行 `scripts/sql/` 下的初始化 SQL。
4. 启动 Eureka、各业务服务、Gateway、Nginx。
5. 启动 Prometheus/Grafana/ELK。

## 3. 访问地址

| 组件 | 地址 | 说明 |
|---|---|---|
| 系统入口 | <http://localhost> | 通过 Nginx 访问前端和 `/api/**` |
| HTTPS 系统入口 | <https://localhost> | 通过 Nginx HTTPS 访问前端和 `/api/**`，使用本机演示自签名证书 |
| Gateway | <http://localhost:8080> | 调试用，正式演示建议走 Nginx |
| Eureka | <http://localhost:8761> | 查看服务注册情况 |
| Prometheus | <http://localhost:9090> | 查看指标抓取状态 |
| Grafana | <http://localhost:3000> | 默认账号 `admin/admin` |
| Elasticsearch | <http://localhost:9200> | 日志存储 |
| Kibana | <http://localhost:5601> | 日志查询 |
| MySQL | `localhost:3307` | 容器内端口 3306，宿主机映射 3307 |

前端在 Docker 部署中会请求同源 `/api/**`，由 Nginx 转发到 `gateway-service:8080`。

HTTPS 入口使用镜像构建阶段生成的 `localhost` 自签名证书，仅用于本机课堂演示。首次用浏览器打开 `https://localhost` 时，浏览器可能提示证书不受信任，需要手动继续访问。

## 4. Nginx 转发关系

Docker 环境中的请求链路：

```text
Browser
  -> http://localhost 或 https://localhost
  -> nginx:80 或 nginx:443
     -> /          直接返回镜像内的 Vue 静态资源
     -> /api/**    转发到 gateway-service:8080
  -> gateway-service
  -> lb://student-service / course-service / teacher-service / enrollment-service
```

Nginx 只作为外部统一入口和反向代理，不直接转发到业务服务。业务服务发现和负载均衡仍由 Spring Cloud Gateway、Eureka、Spring Cloud LoadBalancer 完成。

## 5. 日志挂载目录

Docker 部署日志挂载到项目目录：

```text
log/docker/
├── eureka-service/
├── gateway-service/
├── student-service/
├── course-service/
├── teacher-service/
├── enrollment-service/
├── nginx/
├── mysql/
└── elasticsearch/
```

Spring Boot 服务通过 `LOGGING_FILE_NAME=/logs/<service>.log` 写入容器内 `/logs`，并映射到项目的 `log/docker/<service>/`。

Logstash 会读取这些日志文件并写入 Elasticsearch，索引名格式：

```text
student-course-cloud-YYYY.MM.dd
```

## 6. Prometheus/Grafana

后端服务已加入 `micrometer-registry-prometheus`，并暴露：

```text
/actuator/prometheus
```

Prometheus 配置文件：

```text
docker/prometheus/prometheus.yml
```

Grafana 已自动配置 Prometheus 数据源：

```text
docker/grafana/provisioning/datasources/prometheus.yml
```

进入 Grafana 后可以手动创建 Dashboard，常用查询示例：

```promql
http_server_requests_seconds_count
jvm_memory_used_bytes
process_cpu_usage
```

## 7. Kibana 查看日志

访问：

```text
http://localhost:5601
```

在 Kibana 中创建 Data View：

```text
student-course-cloud-*
```

然后进入 Discover 查看微服务、Nginx、MySQL 日志。

## 8. 常用命令

查看容器状态：

```bash
docker compose ps
```

查看某个服务日志：

```bash
docker compose logs -f gateway-service
docker compose logs -f enrollment-service
docker compose logs -f nginx
```

停止所有容器：

```bash
docker compose down
```

停止并清空数据库、Prometheus、Grafana、Elasticsearch 数据卷：

```bash
docker compose down -v
```

重新构建某个服务：

```bash
docker compose build gateway-service
docker compose up -d gateway-service
```

## 9. 演示账号

初始化 SQL 会创建演示账号：

| 角色 | 用户名 | 密码 |
|---|---|---|
| 管理员 | `admin` | `admin123` |
| 学生 | `student001` | `student123` |
| 学生 | `student002` | `student123` |
| 学生 | `student003` | `student123` |
| 教师 | `teacher001` | `teacher123` |
| 教师 | `teacher002` | `teacher123` |
| 教师 | `teacher003` | `teacher123` |
| 教师 | `teacher004` | `teacher123` |

## 10. 常见问题

### 10.1 80 端口被占用

修改 `docker-compose.yml` 中 nginx 的端口映射：

```yaml
ports:
  - "8088:80"
```

然后访问：

```text
http://localhost:8088
```

### 10.2 443 端口被占用

如果宿主机已有 HTTPS 服务占用 443，可以修改 `docker-compose.yml` 中 nginx 的 HTTPS 端口映射：

```yaml
ports:
  - "8443:443"
```

然后访问：

```text
https://localhost:8443
```

### 10.3 3307 端口被占用

修改 MySQL 端口映射，例如：

```yaml
ports:
  - "3310:3306"
```

容器内部服务仍访问 `mysql:3306`，不受宿主机端口变化影响。

### 10.4 MySQL 初始化数据没有变化

MySQL 初始化 SQL 只会在数据卷第一次创建时执行。需要重建数据库时执行：

```bash
docker compose down -v
docker compose up -d --build
```

### 10.5 Eureka 页面暂时没有服务

业务服务启动和注册需要一点时间。可等待 30-60 秒后刷新：

```text
http://localhost:8761
```

### 10.6 ELK 启动慢

Elasticsearch 和 Kibana 首次启动较慢。如果 Docker 内存不足，Kibana 或 Logstash 可能反复重启。建议提高 Docker Desktop 内存，或临时关闭 ELK 服务。

## 11. Docker 部署验收步骤

1. 执行 `docker compose up -d --build`。
2. 打开 <http://localhost> 或 <https://localhost>。
3. 使用 `admin/admin123` 登录。
4. 查看课程、教师、学生、选课记录页面。
5. 打开 <http://localhost:8761>，确认服务注册。
6. 打开 <http://localhost:9090/targets>，确认服务指标为 `UP`。
7. 打开 <http://localhost:3000>，确认 Grafana Prometheus 数据源可用。
8. 打开 <http://localhost:5601>，创建 `student-course-cloud-*` Data View 查看日志。
