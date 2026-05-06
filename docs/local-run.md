# 本机运行说明

## 1. 运行目标

本文档说明如何在本机启动“学生课程管理系统”的完整演示环境。系统采用前后端分离和微服务架构，前端只访问 `gateway-service`，后端服务通过 Eureka 注册发现，通过 Gateway 对外暴露 `/api/**` 接口。

本阶段只包含本机运行内容，不包含 Kubernetes、Jenkins、Service Mesh 等部署脚本。

## 2. 环境要求

| 环境 | 要求 |
|---|---|
| JDK | Java 17 |
| Maven | Maven 3.8+ |
| MySQL | MySQL 8.0 |
| Node.js | 建议 Node.js 18+ |
| npm | 随 Node.js 安装 |

检查命令：

```powershell
java -version
mvn -version
mysql --version
node -v
npm -v
```

本机默认 MySQL 信息：

| 项目 | 值 |
|---|---|
| 地址 | `localhost` |
| 端口 | `3306` |
| 用户名 | `root` |
| 密码 | `123888` |

后端服务的 `application.yml` 使用环境变量读取 MySQL 用户名和密码：

```text
MYSQL_USER
MYSQL_PASSWORD
```

因此启动后端前需要设置：

```powershell
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
```

Linux/macOS：

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
```

## 3. MySQL 初始化

SQL 文件位于：

```text
scripts/sql
```

执行顺序：

1. `00-create-databases.sql`
2. `01-student-service.sql`
3. `02-course-service.sql`
4. `03-teacher-service.sql`
5. `04-enrollment-service.sql`
6. `05-demo-data.sql`

### 3.1 Windows PowerShell 初始化

方式一：使用脚本。

```powershell
cd D:\demo\YunSoftwareSystem
.\scripts\win\init-db.ps1 -MysqlUser root -MysqlPassword "123888"
```

方式二：手动执行。

```powershell
cd D:\demo\YunSoftwareSystem
Get-Content -Raw -Encoding UTF8 .\scripts\sql\00-create-databases.sql | mysql -h localhost -P 3306 -u root -p123888
Get-Content -Raw -Encoding UTF8 .\scripts\sql\01-student-service.sql | mysql -h localhost -P 3306 -u root -p123888
Get-Content -Raw -Encoding UTF8 .\scripts\sql\02-course-service.sql | mysql -h localhost -P 3306 -u root -p123888
Get-Content -Raw -Encoding UTF8 .\scripts\sql\03-teacher-service.sql | mysql -h localhost -P 3306 -u root -p123888
Get-Content -Raw -Encoding UTF8 .\scripts\sql\04-enrollment-service.sql | mysql -h localhost -P 3306 -u root -p123888
Get-Content -Raw -Encoding UTF8 .\scripts\sql\05-demo-data.sql | mysql -h localhost -P 3306 -u root -p123888
```

### 3.2 Linux/macOS Bash 初始化

方式一：使用脚本。

```bash
cd /path/to/YunSoftwareSystem
MYSQL_USER=root MYSQL_PASSWORD='123888' bash scripts/unix/init-db.sh
```

方式二：手动执行。

```bash
cd /path/to/YunSoftwareSystem
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/00-create-databases.sql
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/01-student-service.sql
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/02-course-service.sql
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/03-teacher-service.sql
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/04-enrollment-service.sql
mysql -h localhost -P 3306 -u root -p123888 < scripts/sql/05-demo-data.sql
```

## 4. 后端启动顺序

必须先启动 Eureka，再启动业务服务，最后启动 Gateway。

启动顺序：

1. `eureka-service`
2. `student-service`
3. `course-service`
4. `teacher-service`
5. `enrollment-service`
6. `gateway-service`

### 4.1 Windows PowerShell 自动启动

```powershell
cd D:\demo\YunSoftwareSystem
.\scripts\win\start-all.ps1 -MysqlUser root -MysqlPassword "123888"
```

如果只启动后端，不启动前端：

```powershell
.\scripts\win\start-all.ps1 -MysqlUser root -MysqlPassword "123888" -SkipFrontend
```

脚本会：

1. 检查 `java`、`mvn`。
2. 执行 `mvn clean package -DskipTests`。
3. 按顺序启动 Eureka、业务服务、Gateway。
4. 将日志输出到 `logs/` 目录。
5. 默认尝试启动前端。

### 4.2 Windows PowerShell 手动启动

先打包：

```powershell
cd D:\demo\YunSoftwareSystem
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
$env:JWT_SECRET="local-demo-secret-change-me"
mvn clean package -DskipTests
```

然后按顺序分别打开多个 PowerShell 窗口执行：

```powershell
cd D:\demo\YunSoftwareSystem
java -jar .\eureka-service\target\eureka-service-0.0.1-SNAPSHOT.jar
```

```powershell
cd D:\demo\YunSoftwareSystem
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
java -jar .\student-service\target\student-service-0.0.1-SNAPSHOT.jar
```

```powershell
cd D:\demo\YunSoftwareSystem
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
java -jar .\course-service\target\course-service-0.0.1-SNAPSHOT.jar
```

```powershell
cd D:\demo\YunSoftwareSystem
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
java -jar .\teacher-service\target\teacher-service-0.0.1-SNAPSHOT.jar
```

```powershell
cd D:\demo\YunSoftwareSystem
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
java -jar .\enrollment-service\target\enrollment-service-0.0.1-SNAPSHOT.jar
```

```powershell
cd D:\demo\YunSoftwareSystem
$env:JWT_SECRET="local-demo-secret-change-me"
java -jar .\gateway-service\target\gateway-service-0.0.1-SNAPSHOT.jar
```

### 4.3 Linux/macOS Bash 自动启动

```bash
cd /path/to/YunSoftwareSystem
MYSQL_USER=root MYSQL_PASSWORD='123888' bash scripts/unix/start-all.sh
```

如果只启动后端，不启动前端：

```bash
SKIP_FRONTEND=1 MYSQL_USER=root MYSQL_PASSWORD='123888' bash scripts/unix/start-all.sh
```

脚本会将日志和 PID 文件写入 `logs/` 目录。

### 4.4 Linux/macOS Bash 手动启动

先打包：

```bash
cd /path/to/YunSoftwareSystem
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
export JWT_SECRET='local-demo-secret-change-me'
mvn clean package -DskipTests
```

然后按顺序分别打开多个终端执行：

```bash
cd /path/to/YunSoftwareSystem
java -jar eureka-service/target/eureka-service-0.0.1-SNAPSHOT.jar
```

```bash
cd /path/to/YunSoftwareSystem
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
java -jar student-service/target/student-service-0.0.1-SNAPSHOT.jar
```

```bash
cd /path/to/YunSoftwareSystem
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
java -jar course-service/target/course-service-0.0.1-SNAPSHOT.jar
```

```bash
cd /path/to/YunSoftwareSystem
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
java -jar teacher-service/target/teacher-service-0.0.1-SNAPSHOT.jar
```

```bash
cd /path/to/YunSoftwareSystem
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
java -jar enrollment-service/target/enrollment-service-0.0.1-SNAPSHOT.jar
```

```bash
cd /path/to/YunSoftwareSystem
export JWT_SECRET='local-demo-secret-change-me'
java -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

## 5. 前端启动方式

前端目录是 `frontend`。

Windows PowerShell：

```powershell
cd D:\demo\YunSoftwareSystem\frontend
npm install
npm run dev
```

Linux/macOS Bash：

```bash
cd /path/to/YunSoftwareSystem/frontend
npm install
npm run dev
```

启动后访问：

```text
http://localhost:5173
```

前端请求会访问：

```text
http://localhost:8080
```

## 6. 端口清单

| 组件 | 端口 | 访问地址 |
|---|---:|---|
| `frontend` | `5173` | `http://localhost:5173` |
| `gateway-service` | `8080` | `http://localhost:8080` |
| `student-service` | `8081` | 不直接给前端访问 |
| `course-service` | `8082` | 不直接给前端访问 |
| `teacher-service` | `8083` | 不直接给前端访问 |
| `enrollment-service` | `8084` | 不直接给前端访问 |
| `eureka-service` | `8761` | `http://localhost:8761` |
| MySQL | `3306` | 本机数据库 |

## 7. 验证方式

### 7.1 验证 Eureka

浏览器访问：

```text
http://localhost:8761
```

应能看到以下服务实例：

```text
GATEWAY-SERVICE
STUDENT-SERVICE
COURSE-SERVICE
TEACHER-SERVICE
ENROLLMENT-SERVICE
```

### 7.2 验证 Gateway

查询课程列表：

```powershell
curl http://localhost:8080/api/courses
```

返回 `Result<T>` 格式 JSON，说明 Gateway 路由和课程服务正常。

### 7.3 验证前端

浏览器访问：

```text
http://localhost:5173
```

可以完成登录、注册、课程管理、教师管理、选课、退课和课表查询。

## 8. 常见错误

### 8.1 Java 版本错误

现象：

```text
release version 17 not supported
Unsupported class file major version
```

原因：当前 Java 版本不是 Java 17，或 `JAVA_HOME` 指向了旧版本 JDK。

处理：

```powershell
java -version
$env:JAVA_HOME
```

确保输出为 Java 17。

### 8.2 Maven 依赖下载失败

现象：

```text
Could not resolve dependencies
Connection timed out
```

原因：网络不稳定、Maven 仓库访问慢或本地仓库缓存损坏。

处理：

1. 检查网络。
2. 重新执行：

```powershell
mvn clean package -DskipTests
```

3. 如果仍失败，检查 Maven `settings.xml` 是否配置了可用镜像。

### 8.3 MySQL 密码错误

现象：

```text
Access denied for user 'root'@'localhost'
```

原因：`MYSQL_PASSWORD` 与本机 MySQL 密码不一致。

处理：

Windows：

```powershell
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123888"
```

Linux/macOS：

```bash
export MYSQL_USER=root
export MYSQL_PASSWORD='123888'
```

也可以直接验证：

```powershell
mysql -h localhost -P 3306 -u root -p123888
```

### 8.4 Eureka 未启动

现象：

```text
Connection refused: localhost:8761
Cannot execute request on any known server
```

原因：业务服务启动时无法连接 Eureka。

处理：

1. 先启动 `eureka-service`。
2. 浏览器确认 `http://localhost:8761` 可访问。
3. 再启动其他业务服务。

### 8.5 Gateway CORS 错误

现象：浏览器控制台出现 CORS 报错。

原因：前端地址不在 Gateway CORS 允许列表中，或前端没有通过 `http://localhost:5173` 访问。

处理：

1. 使用 `http://localhost:5173` 访问前端。
2. 确认 Gateway 正在运行。
3. 确认前端请求地址是 `http://localhost:8080`。

### 8.6 JWT 过期

现象：

```text
401 Unauthorized
```

原因：登录 token 过期或 token 无效。

处理：

1. 退出登录。
2. 清理浏览器 localStorage。
3. 重新登录。

### 8.7 Feign 调用失败

现象：

```text
服务不可用
course-service is unavailable
teacher-service is unavailable
student-service is unavailable
```

原因：`enrollment-service` 调用其他服务失败，可能是目标服务未启动、未注册到 Eureka、网络连接失败或熔断降级触发。

处理：

1. 确认 Eureka 页面能看到目标服务。
2. 确认 `student-service`、`course-service`、`teacher-service` 都已启动。
3. 查看 `logs/enrollment-service.log`。
4. 等待 Resilience4j 熔断器恢复后重试。

## 9. 停止服务

Windows 自动脚本会在 `logs/` 下生成 PID 文件，推荐使用停止脚本：

```powershell
cd D:\demo\YunSoftwareSystem
.\scripts\win\stop-all.ps1
```

如果想保留前端，只停止后端：

```powershell
.\scripts\win\stop-all.ps1 -KeepFrontend
```

Linux/macOS 自动脚本会在 `logs/` 下生成 PID 文件，推荐使用停止脚本：

```bash
cd /path/to/YunSoftwareSystem
bash scripts/unix/stop-all.sh
```

如果想保留前端，只停止后端：

```bash
KEEP_FRONTEND=1 bash scripts/unix/stop-all.sh
```

## 10. Spring Cloud Config 配置中心

本项目已加入独立 `config-service` 配置中心，端口为 `8888`。除 `config-service` 外，后端微服务启动时都会先读取：

```text
http://localhost:8888
```

本机启动顺序调整为：

```text
MySQL -> config-service -> eureka-service -> student/course/teacher/enrollment -> gateway-service -> frontend
```

集中配置文件位于：

```text
config-repo/
```

本机部署默认使用 `local` profile。可以通过下面命令验证配置中心：

```powershell
curl http://localhost:8888/actuator/health
curl http://localhost:8888/student-service/local
curl http://localhost:8888/gateway-service/local
```

更多说明见 `docs/config-center.md`。
