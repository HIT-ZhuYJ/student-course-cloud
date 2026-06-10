# K8s Worker 调度与 Master Jenkins 部署

## 1. 调度目标

当前集群包含：

| 节点 | IP | 角色 |
|---|---|---|
| k8s-master | 192.168.40.129 | 控制平面、Jenkins、镜像仓库、kubectl 管理 |
| k8s-worker1 | 192.168.40.130 | 工作节点 |
| k8s-worker2 | 192.168.40.131 | 工作节点 |

业务和无状态组件不再固定到 `scms-role=app` 或 `scms-role=data`。这些 Pod 由 Kubernetes scheduler 在两个 worker 节点之间按资源请求、节点可用资源和调度策略自行选择。

## 2. 自由调度范围

以下无状态 Deployment 已取消 `nodeSelector`：

- `config-service`
- `eureka-service`
- `student-service`
- `course-service`
- `teacher-service`
- `enrollment-service`
- `gateway-service`
- `frontend`
- `nginx`
- `prometheus`
- `grafana`
- `elasticsearch`
- `kibana`

说明：

- Kubernetes 默认不会按实时 CPU/内存自动迁移已经运行的 Pod。
- “根据负载调度”主要发生在新 Pod 创建或滚动更新时，scheduler 会根据资源请求和节点状态做调度决策。
- 当前方案已增加 `metrics-server` 和 HPA。负载升高时，HPA 会扩容新的 Pod；新增 Pod 再由 Kubernetes scheduler 根据资源请求、节点状态和软反亲和规则调度到 worker1 或 worker2。
- 为避免课堂实验环境资源紧张导致服务反复重启，Java 服务已增加 `startupProbe`，liveness/readiness 探针也增加了超时时间。

已配置 HPA 的服务：

- `gateway-service`
- `student-service`
- `course-service`
- `teacher-service`
- `enrollment-service`
- `frontend`
- `nginx`

未配置 HPA 的组件：

- `config-service`、`eureka-service`：基础注册/配置组件，单副本更适合当前课堂实验配置。
- `mysql`：StatefulSet + 本地 PV/PVC，不参与自由漂移。
- `prometheus`、`grafana`、`elasticsearch`、`kibana`：实验观测组件，保持单副本，避免占用过多资源。

## 3. 保留固定约束的组件

`mysql` 仍保留数据相关约束，因为它使用 StatefulSet 和本地 PV/PVC，数据卷不能像无状态 Pod 一样任意漂移。

`logstash` 是 DaemonSet，继续通过 `scms-log=enabled` 控制部署到需要采集日志的节点。

## 4. Calico 节点 IP 识别

Calico 已调整为只从实验网段识别节点地址：

```yaml
nodeAddressAutodetectionV4:
  cidrs:
    - 192.168.40.0/24
```

这避免 master 节点误识别到非 Kubernetes 通信网卡，例如 `10.4.0.1`。

验证：

```bash
kubectl get pods -n calico-system -o wide
kubectl get node k8s-master k8s-worker1 k8s-worker2 -o yaml | grep projectcalico.org/IPv4Address
```

## 5. Jenkins 部署方案

Jenkins 不部署在 Windows 本机，也不作为业务 Pod 部署到 worker。当前方案是在 `k8s-master` 上使用 systemd 常驻运行：

- Jenkins Home：`/home/zyj/jenkins-home`
- Jenkins war：`/home/zyj/jenkins.war`
- systemd unit：`/etc/systemd/system/jenkins-master.service`
- 访问地址：`http://192.168.40.129:8080`
- 流水线任务：`student-course-cloud-main-release-cicd`

Jenkins 使用仓库根目录的 `Jenkinsfile` 作为流水线定义。

## 6. Jenkins 部署脚本

从 Windows 本机执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\win\setup-master-jenkins.ps1 `
  -MasterHost "192.168.40.129" `
  -SshUser "zyj" `
  -SshPassword "123456"
```

脚本会完成：

1. 上传 `jenkins.war` 到 master。
2. 可选上传本机 Jenkins 插件。
3. 在 master 上创建/更新 `jenkins-master.service`。
4. 创建/更新 Jenkins 任务 `student-course-cloud-main-release-cicd`。
5. 从当前仓库 `Jenkinsfile` 写入流水线定义。
6. 重启 Jenkins。

如果只更新 Jenkinsfile 或任务配置，不想重复上传插件：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\win\setup-master-jenkins.ps1 `
  -SshPassword "123456" `
  -SkipPluginUpload
```

## 7. Jenkins 凭据要求

Jenkins 中需要以下凭据：

| Credentials ID | 类型 | 用途 |
|---|---|---|
| `k8s-node-ssh` | username/password | Jenkins 执行 sudo、访问节点 |
| `harbor-credentials` | username/password | 登录本地镜像仓库 `192.168.40.129:5000` |
| `github-release-credentials` | username/password | 推送固定发布分支 |

固定发布分支：

```text
release/k8s-deployed
```

## 8. 验证命令

```bash
kubectl get nodes -o wide
kubectl get pods -n student-course -o wide
kubectl get deploy -n student-course
kubectl get hpa -n student-course
kubectl top nodes
kubectl top pods -n student-course
kubectl get pods -n calico-system -o wide
systemctl status jenkins-master.service --no-pager
```

外部访问：

```text
http://192.168.40.129:30080/
http://192.168.40.129:30080/api/courses
http://192.168.40.129:30081/actuator/health
http://192.168.40.129:30061/
http://192.168.40.129:8080/
```
