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

## 9. 微服务约束

本方案继续满足项目约束：

- API 统一进入 nginx/gateway-service。
- 业务服务仍通过 Eureka 注册发现。
- enrollment-service 仍通过 OpenFeign 调用其他服务。
- 每个服务仍只访问自己的逻辑库。
- 不使用 Nacos、Redis、MQ、Seata、Ribbon、Hystrix。
- MySQL 继续使用 `db_student`、`db_course`、`db_teacher`、`db_enrollment` 四个逻辑库。
