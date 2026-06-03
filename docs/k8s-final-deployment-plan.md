# Kubernetes 三节点最终部署计划书

## 1. 部署目标

将学生课程管理系统从 Docker Compose 演示部署迁移到三节点 Kubernetes 集群，保留项目的微服务边界、配置中心、注册中心、统一网关、前端入口、MySQL 数据库和基础观测链路。

本阶段采用课堂演示级 Kubernetes 部署，不引入 Kubernetes 之外的复杂平台能力。MySQL 使用单实例 StatefulSet，微服务使用 Deployment，服务发现仍由 Eureka + Spring Cloud LoadBalancer 完成，Kubernetes Service 只负责 Pod 网络访问和外部入口暴露。

## 2. 节点规划

| 节点 | IP | 角色 | 部署内容 |
|---|---|---|---|
| k8s-master | 192.168.40.129 | 控制面 | Kubernetes 控制面、etcd、kubectl 管理，不部署项目业务 Pod |
| k8s-worker1 | 192.168.40.130 | 应用入口与核心服务 | nginx、frontend、gateway-service、config-service、eureka-service、student-service、course-service |
| k8s-worker2 | 192.168.40.131 | 数据与观测服务 | mysql、teacher-service、enrollment-service、prometheus、grafana、elasticsearch、kibana |

## 3. 访问链路

```text
浏览器
  -> NodePort nginx:30080
  -> frontend 静态页面
  -> /api/** 反向代理到 gateway-service:8080
  -> Spring Cloud Gateway
  -> Eureka 服务发现
  -> student/course/teacher/enrollment 微服务
  -> MySQL 多逻辑库
```

前端仍只访问统一入口，不直接访问任何业务服务。

## 4. 数据库方案

数据库部署为单实例 MySQL StatefulSet，并固定调度到 `k8s-worker2`。

数据库初始化沿用 Docker Compose 使用的文件：

```text
scripts/sql/00-create-databases.sql
scripts/sql/01-student-service.sql
scripts/sql/02-course-service.sql
scripts/sql/03-teacher-service.sql
scripts/sql/04-enrollment-service.sql
scripts/sql/05-demo-data.sql
```

这些 SQL 会以 ConfigMap 形式挂载到 MySQL 容器的 `/docker-entrypoint-initdb.d`，保持和 Docker 部署一致。

## 5. 组件部署顺序

1. 创建 namespace、节点标签、Secret、ConfigMap、PV/PVC。
2. 部署 MySQL，并等待健康。
3. 部署 config-service。
4. 部署 eureka-service。
5. 部署 student-service、course-service、teacher-service。
6. 部署 enrollment-service。
7. 部署 gateway-service。
8. 部署 frontend 和 nginx。
9. 部署 Prometheus、Grafana、Elasticsearch、Logstash、Kibana。
10. 验证 Eureka 注册、API 响应、前端入口和观测链路。

## 6. 观测链路

指标链路：

```text
Spring Boot Actuator /actuator/prometheus
  -> Prometheus
  -> Grafana
```

日志链路：

```text
Pod stdout/stderr
  -> /var/log/containers/*.log
  -> Logstash DaemonSet
  -> Elasticsearch
  -> Kibana
```

Kubernetes 中不再依赖 Docker Compose 的 `./log/docker` 本地目录，因为 Pod 会分布在不同节点。使用节点级容器日志目录更适合 K8s。

## 7. 外部访问端口

| 服务 | NodePort | 用途 |
|---|---:|---|
| nginx | 30080 | 系统统一入口 |
| gateway-service | 30081 | Gateway 调试入口 |
| eureka-service | 30061 | Eureka 控制台 |
| prometheus | 30090 | 指标采集页面 |
| grafana | 30300 | 指标看板，默认 admin/admin |
| elasticsearch | 30200 | Elasticsearch 调试端口 |
| logstash | 30504 / 30600 | Beats 输入与 Logstash 监控端口 |
| kibana | 30601 | 日志检索页面 |

## 8. 资源与风险

worker2 已扩容到 8G，最终部署启用完整 ELK 日志链路。Elasticsearch 使用 512m JVM heap，Logstash 使用 256m JVM heap，满足课堂演示和基础日志检索需求。

当前三节点环境资源比较紧，尤其是 `k8s-worker2` 同时承载 MySQL、teacher-service、enrollment-service 和观测组件。为了避免滚动更新时额外创建一个同类 Pod 导致 `Insufficient memory`，业务服务和前端入口的 Deployment 使用如下滚动策略：

```yaml
strategy:
  type: RollingUpdate
  rollingUpdate:
    maxSurge: 0
    maxUnavailable: 1
```

这表示先停止旧 Pod，再启动新 Pod。课堂演示环境中可接受短暂服务不可用，换取部署稳定性。

## 9. 微服务约束

本方案继续满足项目约束：

- API 统一进入 nginx/gateway-service。
- 业务服务仍通过 Eureka 注册发现。
- enrollment-service 仍通过 OpenFeign 调用其他服务。
- 每个服务仍只访问自己的逻辑库。
- 不使用 Nacos、Redis、MQ、Seata、Ribbon、Hystrix。
- MySQL 继续使用 `db_student`、`db_course`、`db_teacher`、`db_enrollment` 四个逻辑库。

## 10. 一键重部署脚本

Windows 本机推荐使用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\win\redeploy-k8s.ps1 `
  -SshPassword "123456" `
  -ApplyDemoData `
  -RunSmokeTest `
  -RunDegradationTest
```

脚本默认使用以下机器：

| 参数 | 默认值 |
|---|---|
| `-MasterHost` | `192.168.40.129` |
| `-WorkerHosts` | `192.168.40.130, 192.168.40.131` |
| `-SshUser` | `zyj` |
| `-Namespace` | `student-course` |
| `-MysqlRootPassword` | `123888` |

如果不希望在命令中写密码，可以省略 `-SshPassword`，脚本会交互式提示输入。

脚本依赖：

- Java 17
- Maven 3.8+
- Node.js / npm
- Docker Desktop
- Python 3
- Python 包 `paramiko`

如果 `paramiko` 不存在，脚本会自动执行：

```powershell
python -m pip install --user paramiko
```

## 11. 脚本执行内容

`scripts/win/redeploy-k8s.ps1` 会按顺序执行：

1. 本机检查 `mvn`、`npm.cmd`、`docker`、`python`。
2. 执行 `mvn clean package`。
3. 执行前端 `npm ci` 和 `npm run build`。
4. 构建以下镜像：
   - `yunsoftwaresystem-config-service:latest`
   - `yunsoftwaresystem-eureka-service:latest`
   - `yunsoftwaresystem-gateway-service:latest`
   - `yunsoftwaresystem-student-service:latest`
   - `yunsoftwaresystem-course-service:latest`
   - `yunsoftwaresystem-teacher-service:latest`
   - `yunsoftwaresystem-enrollment-service:latest`
   - `yunsoftwaresystem-frontend:latest`
5. 使用 `docker save` 生成 `deploy-images.tar`。
6. 通过 SSH/SFTP 上传镜像包到两个 worker。
7. 在 worker 上执行 `ctr -n k8s.io images import /tmp/scms-deploy-images.tar`。
8. 上传 `k8s/`、`config-repo/`、`scripts/sql/` 到 master 的 `/tmp/scms-redeploy`。
9. 更新 ConfigMap：
   - `config-repo`
   - `mysql-init-sql`
   - `nginx-conf`
   - `logstash-pipeline`
10. `kubectl apply` 当前仓库的 K8s 清单。
11. 执行 `06-upgrade-course-schedule-weeks.sql`。
12. 如果指定 `-ApplyDemoData`，执行 `05-demo-data.sql`。
13. 按依赖顺序滚动重启核心组件。
14. 校验 Pod、Deployment、Endpoint 和外部访问端口。
15. 如果指定 `-RunSmokeTest`，执行完整 API 闭环测试。
16. 如果指定 `-RunDegradationTest`，演示 course-service 双实例和降级恢复。
17. 清理本地和 worker 上的临时镜像包。

## 12. 常用参数

| 参数 | 用途 |
|---|---|
| `-SkipBackendBuild` | 跳过 Maven 后端构建 |
| `-SkipFrontendBuild` | 跳过前端构建 |
| `-SkipImageBuild` | 跳过 Docker 镜像构建 |
| `-SkipImageUpload` | 跳过镜像上传和导入 |
| `-ApplyDemoData` | 执行演示数据 SQL，确保 `admin/admin123` 等账号存在 |
| `-RunSmokeTest` | 运行完整 API 闭环测试 |
| `-RunDegradationTest` | 运行 course-service 双实例与降级测试 |
| `-RestartObservability` | 同时重启 Prometheus、Grafana、Elasticsearch、Kibana、Logstash |
| `-KeepImageArchive` | 保留本地 `deploy-images.tar` |

日常只更新后端或前端代码时，使用完整命令即可。

如果只是更新 K8s YAML 或配置，并且镜像已经导入过，可以使用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\win\redeploy-k8s.ps1 `
  -SshPassword "123456" `
  -SkipBackendBuild `
  -SkipFrontendBuild `
  -SkipImageBuild `
  -SkipImageUpload
```

## 13. 部署后验收

脚本会自动检查这些入口：

| 地址 | 预期 |
|---|---|
| `http://192.168.40.129:30080/` | 前端 HTML 返回 200 |
| `http://192.168.40.129:30080/api/courses` | Gateway API 返回课程列表 |
| `http://192.168.40.129:30081/actuator/health` | Gateway health 为 `UP` |
| `http://192.168.40.129:30061/` | Eureka 页面可打开 |
| `http://192.168.40.129:30090/-/healthy` | Prometheus healthy |
| `http://192.168.40.129:30300/api/health` | Grafana health 正常 |
| `http://192.168.40.129:30601/api/status` | Kibana status 正常 |

指定 `-RunSmokeTest` 后，会复用 `scripts/win/api-smoke-test.ps1`，覆盖：

- 管理员登录
- 公共课程查询
- 未登录访问受保护接口返回 401
- 学生权限不足返回 403
- 创建课程、更新课程、创建课程时间
- 创建教师、教师登录、更新教师
- 分配教师到课程
- 学生注册、登录、修改个人信息
- 学生选课、重复选课拒绝
- 课表查询、周课表查询
- 教师查询课程学生
- 学生退课

指定 `-RunDegradationTest` 后，会额外验证：

1. 将 `course-service` 扩容到 2 个实例。
2. 确认 Endpoint 中出现两个 course-service Pod。
3. 连续访问课程列表，确认 Gateway 和服务发现仍正常。
4. 将 `course-service` 缩容到 0。
5. 调用选课接口，确认 enrollment-service 返回明确降级提示。
6. 最后自动恢复 `course-service` 为 1 个实例。

## 14. Jenkins CI/CD

本机已提供 Jenkins 启动包和 Jenkins Home，可使用下面脚本创建/更新流水线任务，并启动 Jenkins：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\win\setup-jenkins-pipeline.ps1
```

默认访问地址：

```text
http://localhost:8090/job/student-course-cloud-cicd/
```

流水线定义文件为仓库根目录的 `Jenkinsfile`。该流水线会调用 `scripts/win/redeploy-k8s.ps1`，按顺序完成：

1. Maven 后端构建。
2. 前端 `npm ci` 与 `npm run build`。
3. 构建 8 个项目镜像。
4. 将镜像包导入两个 worker 节点的 containerd。
5. 同步 `k8s/`、`config-repo/`、`scripts/sql/` 到 master。
6. 应用 Kubernetes 清单与 ConfigMap。
7. 重启核心服务和可观测组件。
8. 执行 API smoke test。
9. 可选执行负载均衡与降级验证。

首次运行时在 Jenkins 页面选择 `Build with Parameters`，填写：

| 参数 | 推荐值 |
|---|---|
| `LOCAL_WORKSPACE` | `D:/demo/YunSoftwareSystem` |
| `MASTER_HOST` | `192.168.40.129` |
| `WORKER_HOSTS` | `192.168.40.130,192.168.40.131` |
| `SSH_USER` | `zyj` |
| `SSH_PASSWORD` | 虚拟机 SSH 密码 |
| `APPLY_DEMO_DATA` | `true` |
| `RUN_SMOKE_TEST` | `true` |
| `RESTART_OBSERVABILITY` | `true` |

当前课堂环境采用离线镜像分发方式：Jenkins 构建镜像后通过 `docker save`、SFTP 和 `ctr -n k8s.io images import` 导入 worker 节点。若后续要求严格演示 Harbor/Gitee Webhook 流程，可在此流水线基础上增加镜像仓库登录、`docker push` 和镜像 tag 更新步骤。

## 15. Grafana 看板

部署脚本会自动创建以下 ConfigMap：

- `prometheus-conf`
- `grafana-datasource`
- `grafana-dashboard-provider`
- `grafana-dashboard-scms`

Grafana 启动后会自动导入 `Student Course Cloud Overview` 看板，包含：

- Prometheus target UP 数量
- 各服务 HTTP 请求速率
- 抓取健康状态表
- JVM 内存使用

验证方式：

```powershell
curl http://192.168.40.129:30090/api/v1/targets
curl -u admin:admin http://192.168.40.129:30300/api/search
```

## 16. 常见问题

### 16.1 新 Pod 一直 ContainerCreating

如果事件中出现：

```text
plugin type="calico" failed (add): error getting ClusterInformation: connection is unauthorized
```

说明 Calico CNI 的访问凭据可能过期或状态异常。可在 master 上执行：

```bash
kubectl rollout restart daemonset/calico-node -n calico-system
kubectl rollout status daemonset/calico-node -n calico-system --timeout=300s
```

然后删除卡住的业务 Pod，让 Deployment 重新创建。

### 16.2 teacher-service 或 enrollment-service 调度失败

如果出现：

```text
0/3 nodes are available: 1 Insufficient memory
```

优先确认 YAML 中已经使用 `maxSurge: 0`。如果仍失败，说明 worker2 资源不足，可以临时关闭或降低观测组件资源，或者给 worker2 增加内存。

### 16.3 Smoke test 偶发 503

刚滚动重启后，Eureka 服务发现可能需要几十秒收敛。脚本中的 smoke test 对 503 有重试，但如果某个写请求已经在下游成功、而 Gateway 侧短暂返回 503，重复执行可能遇到唯一键冲突。等待 30 秒后重新运行 smoke test 即可。

### 16.4 ConfigMap 已更新但服务没有生效

ConfigMap 挂载或配置中心文件变更后，需要重启相关 Pod。`redeploy-k8s.ps1` 会默认重启核心 Deployment。手工操作时可执行：

```bash
kubectl rollout restart deployment/config-service -n student-course
kubectl rollout restart deployment/gateway-service -n student-course
kubectl rollout restart deployment/student-service -n student-course
kubectl rollout restart deployment/course-service -n student-course
kubectl rollout restart deployment/teacher-service -n student-course
kubectl rollout restart deployment/enrollment-service -n student-course
```
