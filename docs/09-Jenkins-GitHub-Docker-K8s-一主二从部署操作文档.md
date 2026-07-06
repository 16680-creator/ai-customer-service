# Jenkins + GitHub + Docker + Kubernetes 一主二从部署操作文档

本文档承接 [08-VMware-Workstation-26H1-一主二从虚拟机创建.md](./08-VMware-Workstation-26H1-一主二从虚拟机创建.md)，用于把本项目部署成下面这条流水线：

```text
GitHub 代码仓库
  -> Jenkins 拉取代码
  -> Docker 构建微服务镜像
  -> 推送到本地 Docker Registry
  -> Kubernetes 一主二从集群拉取镜像
  -> 滚动部署微服务
```

最终结构：

| 节点 | IP | 角色 |
|---|---|---|
| k8s-master | 192.168.56.10 | Kubernetes 主节点、kubectl 管理节点 |
| k8s-worker01 | 192.168.56.11 | Kubernetes 工作节点 |
| k8s-worker02 | 192.168.56.12 | Kubernetes 工作节点、Jenkins、Docker 构建机、本地镜像仓库 |

本项目已按该流程调整：

```text
Jenkinsfile
deploy/k8s/*.yaml
deploy/k8s/services/*.yaml
deploy/scripts/k8s-*.sh
deploy/scripts/setup-*.sh
各微服务 application.yml
各微服务 Dockerfile
```

主要改动说明：

| 文件 | 作用 |
|---|---|
| `Jenkinsfile` | 改为 GitHub 拉代码、Docker 构建推送、本地 Kubernetes 部署的流水线 |
| `.dockerignore` | 减少 Docker 构建上下文，避免把文档、target、临时目录打进构建上下文 |
| `.gitattributes` | 强制脚本、YAML、Jenkinsfile 使用 LF，避免 Linux 执行脚本失败 |
| `deploy/scripts/k8s-build-push.sh` | 构建 7 个微服务镜像并推送到本地 Registry |
| `deploy/scripts/k8s-deploy-services.sh` | 应用业务服务 YAML，并用指定镜像版本滚动更新 |
| `deploy/scripts/k8s-apply-infra.sh` | 部署 MySQL、Nacos、Redis、Elasticsearch、RocketMQ、MinIO |
| `deploy/scripts/k8s-init-mysql.sh` | 把 `deploy/mysql/init.sql` 导入 MySQL 主库 |
| `deploy/scripts/setup-local-registry.sh` | 在 worker02 启动本地 Docker Registry |
| `deploy/scripts/setup-containerd-insecure-registry.sh` | 让 Kubernetes 节点的 containerd 能拉取 HTTP 本地仓库 |
| `deploy/scripts/setup-jenkins-docker-insecure-registry.sh` | 让 Jenkins 所在机器 Docker 能推送 HTTP 本地仓库 |
| `deploy/k8s/services/*.yaml` | 镜像地址改为 `192.168.56.12:5000/aics/...`，网关暴露 `30080` |
| `deploy/k8s/minio.yaml` | 新增 MinIO 对象存储 |
| `deploy/k8s/mysql.yaml`、`redis.yaml`、`elasticsearch.yaml` | 增加 `local-path` 存储类 |
| `ai-cs-*/application.yml` | 改为通过环境变量读取 Nacos、MySQL、Redis、RocketMQ、Elasticsearch、OpenAI 配置 |
| `ai-cs-*/Dockerfile` | 启动命令支持 `JAVA_OPTS` |

---

## 1. 前置条件

你需要先完成：

```text
3 台 Ubuntu Server 虚拟机已创建
三台机器固定 IP
三台机器主机名正确
三台机器互相 ping 通
三台机器可以访问外网
swap 已关闭
containerd 已安装
kubeadm / kubelet / kubectl 已安装
Kubernetes 一主二从集群已初始化
```

如果在 master 执行：

```bash
kubectl get nodes -o wide
```

出现：

```text
Command 'kubectl' not found
```

说明 Kubernetes 命令行工具还没有安装。不要使用提示里的 `sudo snap install kubectl`，按下面 `1.1` 的 apt 仓库方式安装，保证 `kubeadm`、`kubelet`、`kubectl` 版本一致。

### 1.1 三台节点安装 Kubernetes 组件

以下命令在三台机器都执行：

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gpg containerd

sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml >/dev/null
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo sed -i "s#sandbox = 'registry.k8s.io/pause:.*'#sandbox = 'registry.aliyuncs.com/google_containers/pause:3.10'#" /etc/containerd/config.toml
sudo sed -i 's#sandbox_image = "registry.k8s.io/pause:.*"#sandbox_image = "registry.aliyuncs.com/google_containers/pause:3.10"#' /etc/containerd/config.toml
sudo systemctl enable --now containerd
sudo systemctl restart containerd

sudo mkdir -p -m 755 /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.36/deb/Release.key | \
  sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.36/deb/ /' | \
  sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
sudo systemctl enable --now kubelet
```

验证：

```bash
kubeadm version
kubectl version --client
systemctl status containerd --no-pager
```

Kubernetes 官方 apt 仓库现在使用 `pkgs.k8s.io`。旧的 `apt.kubernetes.io` 仓库已经废弃并冻结，不建议继续使用。

### 1.2 初始化 master

以下命令只在 `k8s-master` 执行：

```bash
sudo kubeadm init \
  --apiserver-advertise-address=192.168.56.10 \
  --pod-network-cidr=192.168.0.0/16 \
  --image-repository=registry.aliyuncs.com/google_containers
```

初始化成功后，按输出提示配置当前用户的 kubeconfig：

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

记录 `kubeadm init` 最后输出的 `kubeadm join ...` 命令，后面 worker 节点要用。

如果你所在网络可以直接访问 `registry.k8s.io`，也可以不加 `--image-repository`。国内网络建议保留该参数，否则可能在拉取 `kube-apiserver`、`kube-controller-manager`、`kube-scheduler`、`etcd`、`pause` 等镜像时失败。

### 1.3 worker 节点加入集群

在 `k8s-worker01` 和 `k8s-worker02` 分别执行 master 输出的 join 命令，格式类似：

```bash
sudo kubeadm join 192.168.56.10:6443 \
  --token xxxxxx.xxxxxxxxxxxxxxxx \
  --discovery-token-ca-cert-hash sha256:xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

如果忘记保存 join 命令，可以在 master 重新生成：

```bash
kubeadm token create --print-join-command
```

然后把输出复制到两个 worker 节点执行。

检查集群：

```bash
kubectl get nodes -o wide
```

期望看到：

```text
k8s-master     Ready
k8s-worker01   Ready
k8s-worker02   Ready
```

---

## 2. Kubernetes 集群准备

### 2.1 安装网络插件

如果你使用 kubeadm 初始化时指定了：

```bash
--pod-network-cidr=192.168.0.0/16
```

可以在 master 执行：

```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.30.2/manifests/calico.yaml
```

如果 GitHub raw 下载超时或连接被重置，改用 jsDelivr CDN 下载到本地再 apply：

```bash
curl -L --connect-timeout 30 --retry 5 \
  -o calico.yaml \
  https://cdn.jsdelivr.net/gh/projectcalico/calico@v3.30.2/manifests/calico.yaml

kubectl apply -f calico.yaml
```

确认 Pod 网络正常：

```bash
kubectl get pods -A
```

### 2.2 安装本地存储插件

本项目的 MySQL、Redis、Elasticsearch、MinIO 使用 PVC。kubeadm 默认没有动态存储插件，所以需要安装 `local-path-provisioner`。

在 `k8s-master` 执行：

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
```

设置为默认 StorageClass：

```bash
kubectl patch storageclass local-path \
  -p '{"metadata":{"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'
```

检查：

```bash
kubectl get storageclass
```

期望看到：

```text
local-path   rancher.io/local-path   Delete   WaitForFirstConsumer   true
```

---

## 3. 在 worker02 安装 Docker、Registry、Jenkins

以下操作在 `k8s-worker02` 执行。

### 3.1 安装 Docker

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

重新登录一次，再验证：

```bash
docker version
```

### 3.2 启动本地镜像仓库

本地 Registry 使用：

```text
192.168.56.12:5000
```

如果你已经把项目代码放到 worker02，可以直接执行：

```bash
cd ai-customer-service
bash deploy/scripts/setup-local-registry.sh
```

也可以手动执行：

```bash
sudo mkdir -p /opt/registry/data
docker run -d \
  --name local-registry \
  --restart=always \
  -p 5000:5000 \
  -v /opt/registry/data:/var/lib/registry \
  registry:2
```

验证：

```bash
curl http://192.168.56.12:5000/v2/_catalog
```

返回 `{}` 或包含仓库列表都表示正常。

### 3.3 配置 Docker 允许推送 HTTP 本地仓库

在 `k8s-worker02` 执行：

```bash
sudo tee /etc/docker/daemon.json <<EOF
{
  "insecure-registries": ["192.168.56.12:5000"]
}
EOF

sudo systemctl restart docker
```

如果使用项目脚本：

```bash
sudo REGISTRY=192.168.56.12:5000 bash deploy/scripts/setup-jenkins-docker-insecure-registry.sh
```

### 3.4 安装 Jenkins

```bash
sudo apt update
sudo apt install -y fontconfig openjdk-21-jre wget gpg

sudo mkdir -p /etc/apt/keyrings
sudo wget -O /etc/apt/keyrings/jenkins-keyring.asc \
https://pkg.jenkins.io/debian-stable/jenkins.io-2026.key

echo "deb [signed-by=/etc/apt/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/" | \
sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

sudo apt update
sudo apt install -y jenkins
sudo systemctl enable --now jenkins
```

让 Jenkins 可以调用 Docker：

```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

访问 Jenkins：

```text
http://192.168.56.12:8080
```

查看初始密码：

```bash
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

建议安装插件：

```text
Git
Pipeline
Docker Pipeline
Credentials
Blue Ocean 可选
```

---

## 4. 配置 Kubernetes 节点拉取本地镜像仓库

三台节点都要配置 containerd，否则 Kubernetes 拉取 `192.168.56.12:5000` 的镜像会失败。

在三台机器分别执行：

```bash
sudo mkdir -p /etc/containerd/certs.d/192.168.56.12:5000

sudo tee /etc/containerd/certs.d/192.168.56.12:5000/hosts.toml <<EOF
server = "http://192.168.56.12:5000"

[host."http://192.168.56.12:5000"]
  capabilities = ["pull", "resolve"]
  skip_verify = true
EOF
```

确认 `/etc/containerd/config.toml` 中有：

```text
config_path = "/etc/containerd/certs.d"
SystemdCgroup = true
```

如果没有，可以使用项目脚本：

```bash
sudo REGISTRY=192.168.56.12:5000 bash deploy/scripts/setup-containerd-insecure-registry.sh
```

或者手动修改后重启：

```bash
sudo systemctl restart containerd
```

---

## 5. 给 Jenkins 配置 kubectl

Jenkins 需要能访问 Kubernetes 集群。

### 5.1 在 worker02 安装 kubectl

如果 worker02 本来就是 Kubernetes 节点，通常已经安装了 `kubectl`。检查：

```bash
kubectl version --client
```

如果没有安装，按 Kubernetes 官方 apt 仓库安装 `kubectl`。

### 5.2 复制 kubeconfig 到 Jenkins 用户

在 `k8s-master` 查看配置：

```bash
sudo cat /etc/kubernetes/admin.conf
```

把内容复制到 `k8s-worker02`：

```bash
sudo mkdir -p /var/lib/jenkins/.kube
sudo vim /var/lib/jenkins/.kube/config
```

保存后设置权限：

```bash
sudo chown -R jenkins:jenkins /var/lib/jenkins/.kube
sudo chmod 600 /var/lib/jenkins/.kube/config
```

验证 Jenkins 用户是否能访问集群：

```bash
sudo -u jenkins kubectl get nodes
```

---

## 6. 上传代码到 GitHub

在本地 Windows 项目目录执行：

```bash
git init
git add .
git commit -m "init ai customer service k8s deployment"
git branch -M main
git remote add origin https://github.com/你的账号/ai-customer-service.git
git push -u origin main
```

如果仓库已经存在，只需要：

```bash
git add .
git commit -m "add jenkins k8s deployment"
git push
```

私有仓库需要在 Jenkins 中配置 GitHub 凭据。

---

## 7. 创建 Jenkins Pipeline

在 Jenkins 页面创建：

```text
New Item -> Pipeline
```

推荐配置：

```text
Definition: Pipeline script from SCM
SCM: Git
Repository URL: https://github.com/你的账号/ai-customer-service.git
Branch: */main
Script Path: Jenkinsfile
```

如果是私有仓库：

```text
Credentials -> 添加 GitHub Token
```

---

## 8. 首次部署参数

第一次构建建议参数：

| 参数 | 建议值 |
|---|---|
| REGISTRY | `192.168.56.12:5000` |
| NAMESPACE | `ai-customer-service` |
| VERSION | 留空 |
| DEPLOY_MODE | `full` |
| SERVICES | 默认全部 |
| SKIP_TESTS | 勾选 |
| DEPLOY_INFRA | 勾选 |
| INIT_DATABASE | 勾选 |
| VERIFY_DEPLOY | 勾选 |
| OPENAI_API_KEY | 本地 Ollama 可填 `demo-key` |
| OPENAI_BASE_URL | 本地 Ollama 可填 `http://宿主机IP:11434/v1` |
| OPENAI_MODEL | 例如 `qwen2.5:7b` |

点击：

```text
Build with Parameters
```

---

## 9. Jenkinsfile 做了什么

当前 `Jenkinsfile` 的流水线阶段：

```text
拉取代码
环境检查
Maven 测试，可选
写入 Kubernetes Secret
部署基础设施，可选
构建并推送镜像
部署业务服务
部署验证
```

构建镜像时会执行：

```text
docker build
docker push 192.168.56.12:5000/aics/服务名:版本号
```

部署时会执行：

```text
kubectl apply -f deploy/k8s/services/
kubectl set image deployment/xxx xxx=192.168.56.12:5000/aics/xxx:版本号
kubectl rollout status deployment/xxx
```

---

## 10. Kubernetes 访问方式

网关服务已经配置为 NodePort：

```text
Service: api-gateway
NodePort: 30080
```

访问地址：

```text
http://192.168.56.10:30080
http://192.168.56.11:30080
http://192.168.56.12:30080
```

只要对应节点网络可达，都可以访问。

查看服务：

```bash
kubectl get svc -n ai-customer-service
```

查看 Pod：

```bash
kubectl get pods -n ai-customer-service -o wide
```

查看日志：

```bash
kubectl logs -n ai-customer-service deployment/api-gateway
kubectl logs -n ai-customer-service deployment/user-service
```

---

## 11. 常用部署命令

只部署基础设施：

```bash
DEPLOY_MODE=infra-only
```

只构建镜像，不部署：

```bash
DEPLOY_MODE=build-only
```

只部署已经存在的镜像：

```bash
DEPLOY_MODE=deploy-only
VERSION=指定版本号
```

只部署某几个服务：

```text
SERVICES=ai-cs-gateway ai-cs-user
```

---

## 12. 常见问题

### 12.1 kubectl: command not found

现象：

```bash
kubectl get nodes -o wide
```

返回：

```text
Command 'kubectl' not found, but can be installed with:
sudo snap install kubectl
```

处理方式：

```bash
sudo apt-get update
sudo apt-get install -y apt-transport-https ca-certificates curl gpg

sudo mkdir -p -m 755 /etc/apt/keyrings
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.36/deb/Release.key | \
  sudo gpg --dearmor -o /etc/apt/keyrings/kubernetes-apt-keyring.gpg

echo 'deb [signed-by=/etc/apt/keyrings/kubernetes-apt-keyring.gpg] https://pkgs.k8s.io/core:/stable:/v1.36/deb/ /' | \
  sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update
sudo apt-get install -y kubelet kubeadm kubectl
sudo apt-mark hold kubelet kubeadm kubectl
```

不要直接使用 `sudo snap install kubectl`。集群节点建议通过 Kubernetes 官方 apt 仓库安装，保持 `kubeadm`、`kubelet`、`kubectl` 版本一致。

安装后验证：

```bash
kubectl version --client
```

如果已经执行过 `kubeadm init`，还需要确认 kubeconfig：

```bash
ls ~/.kube/config
```

如果不存在，在 master 执行：

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

### 12.2 kubeadm init 后 kubectl 连接失败

现象：

```text
The connection to the server localhost:8080 was refused
```

通常是没有配置 kubeconfig。在 master 执行：

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
```

然后重试：

```bash
kubectl get nodes -o wide
```

### 12.3 kubeadm init 拉取 registry.k8s.io 镜像失败

现象：

```text
failed to pull and unpack image "registry.k8s.io/kube-apiserver:v1.36.2"
failed to do request: Head "https://asia-east1-docker.pkg.dev/..."
connect: connection refused
```

这是国内网络访问 `registry.k8s.io` 或 Google Artifact Registry 失败。处理方式是在 master 使用国内镜像仓库初始化：

```bash
sudo kubeadm config images pull \
  --kubernetes-version v1.36.2 \
  --image-repository=registry.aliyuncs.com/google_containers

sudo kubeadm init \
  --apiserver-advertise-address=192.168.56.10 \
  --pod-network-cidr=192.168.0.0/16 \
  --image-repository=registry.aliyuncs.com/google_containers
```

如果之前 `kubeadm init` 已经执行到一半并生成了部分集群配置，先重置后再初始化：

```bash
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/manifests /etc/kubernetes/pki
sudo systemctl restart containerd
```

然后重新执行上面的 `kubeadm init`。

### 12.4 kubeadm init 在 wait-control-plane 阶段超时

现象：

```text
error execution phase wait-control-plane:
cannot obtain client without bootstrap:
could not bootstrap the admin user in file admin.conf
unable to create ClusterRoleBinding:
client rate limiter Wait returned an error:
rate: Wait(n=1) would exceed context deadline
```

这通常表示控制面组件启动过慢，或者 `kube-apiserver`、`etcd`、`kubelet` 没有正常起来。先不要马上重装，先检查控制面是否已经在后台启动完成：

```bash
sudo systemctl status kubelet --no-pager
sudo crictl ps -a | grep -E 'kube-apiserver|etcd|kube-controller|kube-scheduler'
sudo ss -lntp | grep 6443 || true
```

如果 `/etc/kubernetes/admin.conf` 已经存在，可以先尝试配置 kubeconfig：

```bash
mkdir -p $HOME/.kube
sudo cp -i /etc/kubernetes/admin.conf $HOME/.kube/config
sudo chown $(id -u):$(id -g) $HOME/.kube/config
kubectl get nodes -o wide
```

如果能正常看到 master 节点，继续安装 Calico 网络插件即可。

如果 `kubectl get nodes` 仍然失败，查看 kubelet 和控制面日志：

```bash
sudo journalctl -u kubelet -n 120 --no-pager
sudo crictl ps -a
```

确认 containerd 使用 systemd cgroup，并且 sandbox/pause 镜像不要指向 `registry.k8s.io`：

```bash
grep -n "SystemdCgroup" /etc/containerd/config.toml
grep -nE "sandbox =|sandbox_image" /etc/containerd/config.toml
```

应该至少看到下面其中一种：

```text
SystemdCgroup = true
sandbox = 'registry.aliyuncs.com/google_containers/pause:3.10'
sandbox_image = "registry.aliyuncs.com/google_containers/pause:3.10"
```

如果不是，修改后重启：

```bash
sudo sed -i 's/SystemdCgroup = false/SystemdCgroup = true/' /etc/containerd/config.toml
sudo sed -i "s#sandbox = 'registry.k8s.io/pause:.*'#sandbox = 'registry.aliyuncs.com/google_containers/pause:3.10'#" /etc/containerd/config.toml
sudo sed -i 's#sandbox_image = "registry.k8s.io/pause:.*"#sandbox_image = "registry.aliyuncs.com/google_containers/pause:3.10"#' /etc/containerd/config.toml
sudo systemctl restart containerd
```

如果配置里没有 `sandbox` 或 `sandbox_image`，可以先预拉阿里云 pause 镜像，并给 `registry.k8s.io/pause:3.10` 打本地 tag，避免 kubelet 再去访问 `registry.k8s.io`：

```bash
sudo ctr -n k8s.io images pull registry.aliyuncs.com/google_containers/pause:3.10
sudo ctr -n k8s.io images tag registry.aliyuncs.com/google_containers/pause:3.10 registry.k8s.io/pause:3.10 || true
```

如果控制面没有启动成功，重置后重新初始化：

```bash
sudo kubeadm reset -f
sudo rm -rf /etc/kubernetes/manifests /etc/kubernetes/pki $HOME/.kube
sudo sed -i "s#sandbox = 'registry.k8s.io/pause:.*'#sandbox = 'registry.aliyuncs.com/google_containers/pause:3.10'#" /etc/containerd/config.toml
sudo sed -i 's#sandbox_image = "registry.k8s.io/pause:.*"#sandbox_image = "registry.aliyuncs.com/google_containers/pause:3.10"#' /etc/containerd/config.toml
sudo systemctl restart containerd
sudo ctr -n k8s.io images pull registry.aliyuncs.com/google_containers/pause:3.10
sudo ctr -n k8s.io images tag registry.aliyuncs.com/google_containers/pause:3.10 registry.k8s.io/pause:3.10 || true

sudo kubeadm init \
  --apiserver-advertise-address=192.168.56.10 \
  --pod-network-cidr=192.168.0.0/16 \
  --image-repository=registry.aliyuncs.com/google_containers
```

如果 master 虚拟机只有 2 核 4GB 且宿主机负载较高，初始化阶段可能超时。建议关闭其他占资源程序，必要时给 master 临时调到 4 核 6GB 后再初始化。

### 12.5 Jenkins 推镜像失败

错误类似：

```text
server gave HTTP response to HTTPS client
```

原因是 Docker 默认按 HTTPS 访问仓库。解决：

```bash
sudo REGISTRY=192.168.56.12:5000 bash deploy/scripts/setup-jenkins-docker-insecure-registry.sh
```

### 12.6 Kubernetes 拉镜像失败

错误类似：

```text
ImagePullBackOff
http: server gave HTTP response to HTTPS client
```

三台节点都执行：

```bash
sudo REGISTRY=192.168.56.12:5000 bash deploy/scripts/setup-containerd-insecure-registry.sh
```

### 12.7 PVC 一直 Pending

检查 StorageClass：

```bash
kubectl get storageclass
```

如果没有 `local-path`，执行：

```bash
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
```

### 12.8 Pod 一直 CrashLoopBackOff

查看日志：

```bash
kubectl logs -n ai-customer-service pod/Pod名称
```

常见原因：

```text
MySQL 未初始化
Nacos 未启动完成
RocketMQ 未启动完成
环境变量配置错误
镜像版本不存在
```

### 12.9 Jenkins 无法执行 kubectl

验证：

```bash
sudo -u jenkins kubectl get nodes
```

如果失败，检查：

```text
/var/lib/jenkins/.kube/config 是否存在
文件权限是否属于 jenkins 用户
master 的 6443 端口是否可访问
```

---

## 13. 推荐首次执行顺序

按下面顺序做，问题最少：

```text
1. 按 08 文档创建三台虚拟机
2. 搭好 Kubernetes 一主二从
3. 安装 local-path-provisioner
4. 在 worker02 安装 Docker、Registry、Jenkins
5. 三台节点配置 containerd 允许拉取 192.168.56.12:5000
6. Jenkins 配好 kubectl
7. 代码推送到 GitHub
8. Jenkins 创建 Pipeline
9. 第一次构建勾选 DEPLOY_INFRA 和 INIT_DATABASE
10. 访问 http://192.168.56.10:30080
```
