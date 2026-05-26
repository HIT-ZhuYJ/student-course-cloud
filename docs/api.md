# 学生课程管理系统 API 文档

## 1. 接口说明

本系统采用前后端分离和微服务架构。前端只访问 API Gateway，统一前缀为：

```text
http://localhost:8080/api/**
```

前端禁止直接访问业务服务端口：

```text
http://localhost:8081
http://localhost:8082
http://localhost:8083
http://localhost:8084
```

所有对前端开放的接口都通过 Gateway 转发到后端服务。内部服务接口用于微服务之间调用，不直接给前端使用。

## 2. 统一响应格式

所有业务接口统一返回 `Result<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1710000000000
}
```

分页接口的 `data` 使用 `PageResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 0,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": 1710000000000
}
```

常见错误响应：

```json
{
  "code": 401,
  "message": "Unauthorized",
  "data": null,
  "timestamp": 1710000000000
}
```

## 3. 鉴权规则

除登录、注册和公开课程查询接口外，请求需要携带 JWT：

```text
Authorization: Bearer <token>
```

角色说明：

| 角色 | 说明 |
|---|---|
| `STUDENT` | 学生 |
| `TEACHER` | 教师 |
| `ADMIN` | 管理员 |

Gateway 校验通过后，会向下游服务透传：

```text
X-User-Id
X-Username
X-Role
X-Related-Id
```

## 4. 认证接口

### 4.1 学生注册

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/auth/register` |
| 下游服务 | `student-service` |
| 是否需要登录 | 否 |
| 允许角色 | 未登录用户、`STUDENT`、`TEACHER`、`ADMIN` |

请求体示例：

```json
{
  "studentNo": "S20260001",
  "name": "张三",
  "major": "软件工程",
  "grade": "2026",
  "phone": "13800000001",
  "email": "zhangsan@example.com",
  "username": "student001",
  "password": "student123"
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1,
    "studentNo": "S20260001",
    "name": "张三",
    "major": "软件工程",
    "grade": "2026",
    "phone": "13800000001",
    "email": "zhangsan@example.com",
    "status": "ACTIVE",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400`：必填字段为空、邮箱格式错误、密码长度不符合要求。
- `409`：用户名或学号已存在。
- `500`：服务内部错误。

### 4.2 登录

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/auth/login` |
| 下游服务 | `student-service` |
| 是否需要登录 | 否 |
| 允许角色 | 未登录用户、`STUDENT`、`TEACHER`、`ADMIN` |

请求体示例：

```json
{
  "username": "student001",
  "password": "student123"
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.xxx",
    "tokenType": "Bearer",
    "userId": 1,
    "username": "student001",
    "role": "STUDENT",
    "relatedId": 1,
    "expiresAt": 1710003600000
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400`：用户名或密码为空。
- `401`：用户名不存在、密码错误、账号被禁用。
- `500`：服务内部错误。

## 5. 学生接口

### 5.1 查询学生详情

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/students/{studentId}` |
| 下游服务 | `student-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT` 查询自己、`ADMIN` 查询任意学生 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1,
    "studentNo": "S20260001",
    "name": "张三",
    "major": "软件工程",
    "grade": "2026",
    "phone": "13800000001",
    "email": "zhangsan@example.com",
    "status": "ACTIVE",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：学生访问他人信息。
- `404`：学生不存在。

### 5.2 修改学生信息

| 项目 | 内容 |
|---|---|
| 请求方法 | `PUT` |
| 前端请求路径 | `/api/students/{studentId}` |
| 下游服务 | `student-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT` 修改自己、`ADMIN` 修改任意学生 |

请求体示例：

```json
{
  "name": "张三",
  "major": "软件工程",
  "grade": "2026",
  "phone": "13800000002",
  "email": "zhangsan2@example.com",
  "status": "ACTIVE"
}
```

响应体示例：同“查询学生详情”。

错误情况：

- `400`：必填字段为空或邮箱格式错误。
- `401`：未登录或 token 无效。
- `403`：权限不足。
- `404`：学生不存在。

### 5.3 查询学生列表

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/students?pageNo=1&pageSize=10&keyword=张三` |
| 下游服务 | `student-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "studentId": 1,
        "studentNo": "S20260001",
        "name": "张三",
        "major": "软件工程",
        "grade": "2026",
        "phone": "13800000001",
        "email": "zhangsan@example.com",
        "status": "ACTIVE",
        "createTime": "2026-05-06T10:00:00",
        "updateTime": "2026-05-06T10:00:00"
      }
    ],
    "total": 1,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。

## 6. 课程接口

### 6.1 新增课程

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/courses` |
| 下游服务 | `course-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：

```json
{
  "courseCode": "CS101",
  "courseName": "Java 程序设计",
  "credit": 3.0,
  "capacity": 60,
  "status": "OPEN",
  "description": "Java 基础课程",
  "schedule": {
    "startWeek": 1,
    "endWeek": 16,
    "weekType": "ALL",
    "weekday": 1,
    "startSection": 1,
    "endSection": 2,
    "startTime": "08:00:00",
    "endTime": "09:45:00",
    "classroom": "A101"
  }
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": 1,
    "courseCode": "CS101",
    "courseName": "Java 程序设计",
    "credit": 3.0,
    "capacity": 60,
    "selectedCount": 0,
    "status": "OPEN",
    "description": "Java 基础课程",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400` / `422`：课程编码、课程名称、学分、容量或首个课程时间不合法。新增课程时 `schedule` 必填。
- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `409`：课程编码已存在。

### 6.2 修改课程

| 项目 | 内容 |
|---|---|
| 请求方法 | `PUT` |
| 前端请求路径 | `/api/courses/{courseId}` |
| 下游服务 | `course-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：

```json
{
  "courseName": "Java 程序设计",
  "credit": 3.5,
  "capacity": 80,
  "status": "OPEN",
  "description": "Java 基础与 Web 开发"
}
```

响应体示例：同“新增课程”。

错误情况：

- `400`：容量小于已选人数、学分或容量不合法。
- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：课程不存在。

### 6.3 删除或禁用课程

| 项目 | 内容 |
|---|---|
| 请求方法 | `DELETE` |
| 前端请求路径 | `/api/courses/{courseId}` |
| 下游服务 | `course-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：课程不存在。

### 6.4 查询课程详情

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/courses/{courseId}` |
| 下游服务 | `course-service` |
| 是否需要登录 | 可不登录 |
| 允许角色 | 未登录用户、`STUDENT`、`TEACHER`、`ADMIN` |

请求体示例：无。

响应体示例：同“新增课程”。

错误情况：

- `404`：课程不存在。

### 6.5 查询课程列表

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/courses?pageNo=1&pageSize=10&keyword=Java&status=OPEN` |
| 下游服务 | `course-service` |
| 是否需要登录 | 可不登录 |
| 允许角色 | 未登录用户、`STUDENT`、`TEACHER`、`ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "courseId": 1,
        "courseCode": "CS101",
        "courseName": "Java 程序设计",
        "credit": 3.0,
        "capacity": 60,
        "selectedCount": 12,
        "status": "OPEN",
        "description": "Java 基础课程",
        "createTime": "2026-05-06T10:00:00",
        "updateTime": "2026-05-06T10:00:00"
      }
    ],
    "total": 1,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `500`：课程服务异常。

### 6.6 新增课程时间

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/courses/{courseId}/schedules` |
| 下游服务 | `course-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：

```json
{
  "weekday": 1,
  "startTime": "08:00:00",
  "endTime": "09:40:00",
  "classroom": "A101"
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "scheduleId": 1,
    "courseId": 1,
    "weekday": 1,
    "startTime": "08:00:00",
    "endTime": "09:40:00",
    "classroom": "A101",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400`：星期、开始时间、结束时间或教室不合法。
- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：课程不存在。

### 6.7 查询课程时间

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/courses/{courseId}/schedules` |
| 下游服务 | `course-service` |
| 是否需要登录 | 可不登录 |
| 允许角色 | 未登录用户、`STUDENT`、`TEACHER`、`ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "scheduleId": 1,
      "courseId": 1,
      "weekday": 1,
      "startTime": "08:00:00",
      "endTime": "09:40:00",
      "classroom": "A101",
      "createTime": "2026-05-06T10:00:00",
      "updateTime": "2026-05-06T10:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

错误情况：

- `404`：课程不存在。

## 7. 教师接口

### 7.1 新增教师

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/teachers` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：

```json
{
  "teacherNo": "T20260001",
  "name": "李老师",
  "title": "副教授",
  "phone": "13800000003",
  "email": "teacher@example.com",
  "status": "ACTIVE"
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "teacherId": 1,
    "teacherNo": "T20260001",
    "name": "李老师",
    "title": "副教授",
    "phone": "13800000003",
    "email": "teacher@example.com",
    "status": "ACTIVE",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400`：教师编号或姓名为空、邮箱格式错误。
- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `409`：教师编号已存在。

### 7.2 修改教师

| 项目 | 内容 |
|---|---|
| 请求方法 | `PUT` |
| 前端请求路径 | `/api/teachers/{teacherId}` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：

```json
{
  "name": "李老师",
  "title": "教授",
  "phone": "13800000004",
  "email": "teacher2@example.com",
  "status": "ACTIVE"
}
```

响应体示例：同“新增教师”。

错误情况：

- `400`：姓名为空或邮箱格式错误。
- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：教师不存在。

### 7.3 删除或禁用教师

| 项目 | 内容 |
|---|---|
| 请求方法 | `DELETE` |
| 前端请求路径 | `/api/teachers/{teacherId}` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：教师不存在。

### 7.4 查询教师详情

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/teachers/{teacherId}` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：同“新增教师”。

错误情况：

- `401`：未登录或 token 无效。
- `403`：权限不足。
- `404`：教师不存在。

### 7.5 查询教师列表

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/teachers?pageNo=1&pageSize=10&keyword=李&status=ACTIVE` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "teacherId": 1,
        "teacherNo": "T20260001",
        "name": "李老师",
        "title": "教授",
        "phone": "13800000003",
        "email": "teacher@example.com",
        "status": "ACTIVE",
        "createTime": "2026-05-06T10:00:00",
        "updateTime": "2026-05-06T10:00:00"
      }
    ],
    "total": 1,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。

### 7.6 为课程分配教师

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/teachers/{teacherId}/courses/{courseId}` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "teacherId": 1,
    "courseId": 1,
    "status": "ACTIVE",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：教师不存在。
- `409`：课程已分配教师或重复分配。

### 7.7 取消课程教师分配

| 项目 | 内容 |
|---|---|
| 请求方法 | `DELETE` |
| 前端请求路径 | `/api/teachers/{teacherId}/courses/{courseId}` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。
- `404`：分配记录不存在。

### 7.8 查询教师任课

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/teachers/{teacherId}/courses` |
| 下游服务 | `teacher-service` |
| 是否需要登录 | 是 |
| 允许角色 | `TEACHER` 查询自己、`ADMIN` 查询任意教师 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "teacherId": 1,
      "courseId": 1,
      "status": "ACTIVE",
      "createTime": "2026-05-06T10:00:00",
      "updateTime": "2026-05-06T10:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：权限不足。
- `404`：教师不存在。

## 8. 选课接口

### 8.1 学生选课

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 前端请求路径 | `/api/enrollments` |
| 下游服务 | `enrollment-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT`、`ADMIN` |

请求体示例：

```json
{
  "studentId": 1,
  "courseId": 1
}
```

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enrollmentId": 1,
    "studentId": 1,
    "courseId": 1,
    "status": "ACTIVE",
    "createTime": "2026-05-06T10:00:00",
    "updateTime": "2026-05-06T10:00:00"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `400`：学生 ID 或课程 ID 为空。
- `401`：未登录或 token 无效。
- `403`：学生选择他人课程、教师无权限选课。
- `404`：学生或课程不存在。
- `409`：重复选课、课程已满、课程未分配教师、课程时间冲突。
- `503`：学生服务、课程服务或教师服务不可用。

### 8.2 学生退课

| 项目 | 内容 |
|---|---|
| 请求方法 | `DELETE` |
| 前端请求路径 | `/api/enrollments/{enrollmentId}` |
| 下游服务 | `enrollment-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT`、`ADMIN` |

请求体示例：无。

响应体示例：同“学生选课”。

错误情况：

- `401`：未登录或 token 无效。
- `403`：学生退他人的课程或教师无权限退课。
- `404`：选课记录不存在。
- `409`：选课记录状态不允许退课。
- `503`：课程服务不可用，减少已选人数失败。

### 8.3 查询学生选课记录

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/enrollments/students/{studentId}` |
| 下游服务 | `enrollment-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT` 查询自己、`ADMIN` 查询任意学生 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "enrollmentId": 1,
      "studentId": 1,
      "courseId": 1,
      "status": "ACTIVE",
      "createTime": "2026-05-06T10:00:00",
      "updateTime": "2026-05-06T10:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：学生查询他人记录。

### 8.4 查询学生课表

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/enrollments/students/{studentId}/timetable` |
| 下游服务 | `enrollment-service` |
| 是否需要登录 | 是 |
| 允许角色 | `STUDENT` 查询自己、`ADMIN` 查询任意学生 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "enrollmentId": 1,
      "studentId": 1,
      "courseId": 1,
      "scheduleId": 1,
      "weekday": 1,
      "startTime": "08:00:00",
      "endTime": "09:40:00",
      "classroom": "A101"
    }
  ],
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：学生查询他人课表。
- `503`：课程服务不可用，无法获取课程时间。

### 8.5 管理员查询全部选课记录

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 前端请求路径 | `/api/enrollments?pageNo=1&pageSize=10&studentId=1&courseId=1&status=ACTIVE` |
| 下游服务 | `enrollment-service` |
| 是否需要登录 | 是 |
| 允许角色 | `ADMIN` |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "enrollmentId": 1,
        "studentId": 1,
        "courseId": 1,
        "status": "ACTIVE",
        "createTime": "2026-05-06T10:00:00",
        "updateTime": "2026-05-06T10:00:00"
      }
    ],
    "total": 1,
    "pageNo": 1,
    "pageSize": 10
  },
  "timestamp": 1710000000000
}
```

错误情况：

- `401`：未登录或 token 无效。
- `403`：非管理员访问。

## 9. 内部服务接口

内部服务接口不直接给前端使用。它们由微服务之间通过 OpenFeign 调用，实际路径不带 `/api` 前缀，由服务注册发现和服务名调用完成。

### 9.1 校验学生状态

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 内部路径 | `/internal/students/{studentId}/status` |
| 调用方 | `enrollment-service` |
| 提供方 | `student-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "studentId": 1,
    "exists": true,
    "status": "ACTIVE",
    "active": true
  },
  "timestamp": 1710000000000
}
```

错误情况：

- 学生不存在时 `exists=false`。
- 服务不可用时 Feign fallback 返回服务不可用信息。

### 9.2 校验课程容量

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 内部路径 | `/internal/courses/{courseId}/check-capacity` |
| 调用方 | `enrollment-service` |
| 提供方 | `course-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": 1,
    "exists": true,
    "status": "OPEN",
    "capacity": 60,
    "selectedCount": 12,
    "selectable": true,
    "hasCapacity": true
  },
  "timestamp": 1710000000000
}
```

错误情况：

- 课程不存在时 `exists=false`。
- 课程关闭时 `selectable=false`。
- 容量已满时 `hasCapacity=false`。
- 服务不可用时 Feign fallback 返回服务不可用信息。

### 9.3 查询课程时间

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 内部路径 | `/internal/courses/{courseId}/schedule` |
| 调用方 | `enrollment-service` |
| 提供方 | `course-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "scheduleId": 1,
      "courseId": 1,
      "weekday": 1,
      "startTime": "08:00:00",
      "endTime": "09:40:00",
      "classroom": "A101",
      "createTime": "2026-05-06T10:00:00",
      "updateTime": "2026-05-06T10:00:00"
    }
  ],
  "timestamp": 1710000000000
}
```

错误情况：

- 课程不存在或无时间安排。
- 服务不可用时 Feign fallback 返回服务不可用信息。

### 9.4 增加课程已选人数

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 内部路径 | `/internal/courses/{courseId}/increase-selected-count` |
| 调用方 | `enrollment-service` |
| 提供方 | `course-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：同“校验课程容量”。

错误情况：

- 课程不存在。
- 课程不是 `OPEN` 状态。
- 课程容量已满。
- 服务不可用时 Feign fallback 返回服务不可用信息。

### 9.5 减少课程已选人数

| 项目 | 内容 |
|---|---|
| 请求方法 | `POST` |
| 内部路径 | `/internal/courses/{courseId}/decrease-selected-count` |
| 调用方 | `enrollment-service` |
| 提供方 | `course-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：同“校验课程容量”。

错误情况：

- 课程不存在。
- 已选人数已经为 0。
- 服务不可用时 Feign fallback 返回服务不可用信息。

### 9.6 校验课程是否已分配教师

| 项目 | 内容 |
|---|---|
| 请求方法 | `GET` |
| 内部路径 | `/internal/courses/{courseId}/teacher-assigned` |
| 调用方 | `enrollment-service` |
| 提供方 | `teacher-service` |
| 是否需要登录 | 内部调用，不给前端直接访问 |
| 允许角色 | 内部服务 |

请求体示例：无。

响应体示例：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "courseId": 1,
    "assigned": true,
    "teacherId": 1,
    "teacherName": "李老师"
  },
  "timestamp": 1710000000000
}
```

错误情况：

- 未分配教师时 `assigned=false`。
- 服务不可用时 Feign fallback 返回服务不可用信息。

## 10. 接口清单汇总

| 分类 | 方法 | 前端路径或内部路径 | 说明 |
|---|---|---|---|
| 认证 | `POST` | `/api/auth/register` | 学生注册 |
| 认证 | `POST` | `/api/auth/login` | 登录 |
| 学生 | `GET` | `/api/students/{studentId}` | 查询学生详情 |
| 学生 | `PUT` | `/api/students/{studentId}` | 修改学生信息 |
| 学生 | `GET` | `/api/students` | 管理员查询学生列表 |
| 课程 | `POST` | `/api/courses` | 新增课程 |
| 课程 | `PUT` | `/api/courses/{courseId}` | 修改课程 |
| 课程 | `DELETE` | `/api/courses/{courseId}` | 禁用课程 |
| 课程 | `GET` | `/api/courses/{courseId}` | 查询课程详情 |
| 课程 | `GET` | `/api/courses` | 查询课程列表 |
| 课程 | `POST` | `/api/courses/{courseId}/schedules` | 新增课程时间 |
| 课程 | `GET` | `/api/courses/{courseId}/schedules` | 查询课程时间 |
| 教师 | `POST` | `/api/teachers` | 新增教师 |
| 教师 | `PUT` | `/api/teachers/{teacherId}` | 修改教师 |
| 教师 | `DELETE` | `/api/teachers/{teacherId}` | 禁用教师 |
| 教师 | `GET` | `/api/teachers/{teacherId}` | 查询教师详情 |
| 教师 | `GET` | `/api/teachers` | 查询教师列表 |
| 教师 | `POST` | `/api/teachers/{teacherId}/courses/{courseId}` | 分配教师 |
| 教师 | `DELETE` | `/api/teachers/{teacherId}/courses/{courseId}` | 取消分配 |
| 教师 | `GET` | `/api/teachers/{teacherId}/courses` | 查询教师任课 |
| 选课 | `POST` | `/api/enrollments` | 学生选课 |
| 选课 | `DELETE` | `/api/enrollments/{enrollmentId}` | 学生退课 |
| 选课 | `GET` | `/api/enrollments/students/{studentId}` | 查询学生选课记录 |
| 选课 | `GET` | `/api/enrollments/students/{studentId}/timetable` | 查询学生课表 |
| 选课 | `GET` | `/api/enrollments` | 管理员查询选课记录 |
| 内部 | `GET` | `/internal/students/{studentId}/status` | 校验学生状态 |
| 内部 | `GET` | `/internal/courses/{courseId}/check-capacity` | 校验课程容量 |
| 内部 | `GET` | `/internal/courses/{courseId}/schedule` | 查询课程时间 |
| 内部 | `POST` | `/internal/courses/{courseId}/increase-selected-count` | 增加已选人数 |
| 内部 | `POST` | `/internal/courses/{courseId}/decrease-selected-count` | 减少已选人数 |
| 内部 | `GET` | `/internal/courses/{courseId}/teacher-assigned` | 校验教师分配 |

## 11. 课程周次与课表补充

课程时间现在按学校课表常用格式保存：

```json
{
  "startWeek": 1,
  "endWeek": 16,
  "weekType": "ALL",
  "weekday": 1,
  "startSection": 1,
  "endSection": 2,
  "startTime": "08:00:00",
  "endTime": "09:45:00",
  "classroom": "A101"
}
```

字段说明：

| 字段 | 说明 |
|---|---|
| `startWeek` / `endWeek` | 起止教学周，当前限制为 1-30 |
| `weekType` | `ALL` 每周，`ODD` 单周，`EVEN` 双周 |
| `weekday` | 星期，1-7 分别表示周一到周日 |
| `startSection` / `endSection` | 起止节次，当前限制为 1-12 |
| `startTime` / `endTime` | 实际上课起止时间 |
| `classroom` | 教室 |

学生课表接口仍为：

```text
GET /api/enrollments/students/{studentId}/timetable
```

不传 `weekNo` 时返回学生总课表；传入 `weekNo` 时返回指定教学周课表：

```text
GET /api/enrollments/students/{studentId}/timetable?weekNo=6
```

响应中会包含课程名称、课程编码、周次、单双周、星期、节次、时间和教室，供前端渲染总课表和周课表。
