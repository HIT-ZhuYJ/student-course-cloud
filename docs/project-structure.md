# 项目结构说明

本项目采用前后端同仓库、后端 Maven 聚合、服务模块平铺的结构。后端模块保持在仓库根目录，方便 Maven 父工程直接聚合；前端作为独立 Vue 3 + Vite 工程放在 `frontend`；本机脚本和 SQL 放在 `scripts`；说明文档放在 `docs`。

```text
YunSoftwareSystem
├── pom.xml
├── AGENTS.md
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
├── docs
└── logs
```

## 根目录

| 路径 | 说明 |
|---|---|
| `pom.xml` | Maven 聚合父工程 |
| `AGENTS.md` | 项目约束和实现要求 |
| `.gitignore` | 忽略构建产物、依赖目录、日志和本机 IDE 配置 |

## 后端模块

| 路径 | 说明 |
|---|---|
| `common-module` | 公共 DTO、统一响应、异常、JWT 工具 |
| `eureka-service` | Eureka Server 注册中心 |
| `gateway-service` | Spring Cloud Gateway 统一入口 |
| `student-service` | 学生与账号服务 |
| `course-service` | 课程服务 |
| `teacher-service` | 教师服务 |
| `enrollment-service` | 选课服务，包含 Feign 调用和熔断降级 |

## 前端

| 路径 | 说明 |
|---|---|
| `frontend` | Vue 3 + Vite + Vue Router + Axios 前端工程 |
| `frontend/src/api` | Gateway API 封装 |
| `frontend/src/router` | 路由和路由守卫 |
| `frontend/src/views` | 登录、注册、学生端、教师端、管理员端页面 |
| `frontend/src/assets` | 全局样式 |

## 脚本

| 路径 | 说明 |
|---|---|
| `scripts/sql` | MySQL 初始化 SQL |
| `scripts/win/init-db.ps1` | Windows 数据库初始化脚本 |
| `scripts/win/start-all.ps1` | Windows 启动脚本 |
| `scripts/win/stop-all.ps1` | Windows 停止脚本 |
| `scripts/unix/init-db.sh` | Linux/macOS 数据库初始化脚本 |
| `scripts/unix/start-all.sh` | Linux/macOS 启动脚本 |
| `scripts/unix/stop-all.sh` | Linux/macOS 停止脚本 |

## 文档

| 路径 | 说明 |
|---|---|
| `docs/architecture.md` | 架构说明 |
| `docs/api.md` | API 文档 |
| `docs/local-run.md` | 本机运行说明 |
| `docs/local-test-flow.md` | 本机验收流程 |
| `docs/project-structure.md` | 项目结构说明 |

## 运行产物

| 路径 | 说明 |
|---|---|
| `logs` | 本机启动日志和 PID 文件，仅用于本机运行，不应提交到版本库 |
| `*/target` | Maven 构建产物，不应提交 |
| `frontend/node_modules` | 前端依赖目录，不应提交 |
| `frontend/dist` | 前端构建产物，不应提交 |
