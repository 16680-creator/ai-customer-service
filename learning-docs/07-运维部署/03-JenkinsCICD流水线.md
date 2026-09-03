# Jenkins CI/CD 流水线：一条命令完成"编译到上线"

> 对应项目文件：根目录 `Jenkinsfile`（主力）、`Jenkinsfile-k8s`（同款流水线，Maven 镜像走国内源）、`deploy/scripts/k8s-*.sh`（流水线调用的部署脚本）
> 前置阅读：[01-Docker容器化](./01-Docker容器化.md)（镜像怎么造）、[02-Kubernetes入门](./02-Kubernetes入门.md)（清单怎么部署）。本篇就是把那两篇里**手工做的事全部自动化**。

---

## 零、先定位：CI/CD 在整套部署逻辑里的位置

回忆 [02 篇第七节](./02-Kubernetes入门.md)手工部署的过程：构建镜像 → 推仓库 → apply 清单 → set image → 验证，8 个服务走一遍至少半小时还容易漏。CI/CD 就是把这串动作固化成一条流水线：

```
【没有 CI/CD】每发一次版
  开发: git push
  运维: 手动 mvn package → 手动 docker build ×8 → push ×8 → apply → 验证
  耗时 30+ 分钟，第 5 个服务忘了 build 是常态

【有 CI/CD】每发一次版
  开发: git push（或在 Jenkins 页面点一下"立即构建"）
  Jenkins: 拉代码 → 测试 → 建镜像 ×8 → 推仓库 → kubectl 滚动更新 → 验证
  全程 10 分钟，人只看结果

本项目部署链路全景（Jenkins 负责虚线框里的全部步骤）：

 git push ──► ┌────────────────────────────────────────────┐ ──► K8s 集群
              │ ①拉代码 ②环境检查 ③Maven测试(可选)           │
              │ ④写Secret ⑤部署基础设施(可选)                │
              │ ⑥构建+推送镜像×8 ⑦kubectl滚动更新 ⑧验证      │
              └────────────────────────────────────────────┘
                  全部定义在根目录 Jenkinsfile 里
```

---

## 一、三个词先分清：CI、CD（交付）、CD（部署）

| 阶段 | 含义 | 本项目对应 stage |
|------|------|-----------------|
| CI 持续集成 | 代码合入后自动编译 + 测试，尽早暴露问题 | `Maven 测试` |
| CD 持续交付 | 产物（镜像）随时可发布，但上线要人点确认 | `构建并推送镜像`（推到仓库后停住） |
| CD 持续部署 | 测试通过自动上线 | `部署业务服务`（直接 set image） |

本项目默认是"持续交付"模式：镜像推到仓库后，部署动作由参数（`DEPLOY_MODE`）控制要不要自动执行。

---

## 二、流水线全貌：8 个 stage 一张图

```
                 ┌──────────────────────────────┐
                 │ ① 拉取代码 checkout scm       │  无条件执行
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ② 环境检查                    │  docker/kubectl 可用？
                 │   docker version             │  registry 可达？
                 │   kubectl cluster-info       │
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ③ Maven 测试（可选）           │  when: !SKIP_TESTS 且非 deploy/infra-only
                 │  在 maven 容器里 mvn test     │
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ④ 写入 Kubernetes Secret      │  when: 非 build-only
                 │  aics-secrets（AI密钥等）      │  ── 幂等创建，重复执行不报错
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ⑤ 部署基础设施（可选）          │  when: DEPLOY_INFRA 或 infra-only
                 │  mysql→nacos→redis→es→...    │  ── 调 k8s-apply-infra.sh
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ⑥ 构建并推送镜像（核心）        │  when: 非 deploy-only / infra-only
                 │  docker build ×7 + push      │  ── 调 k8s-build-push.sh
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ⑦ 部署业务服务                │  when: 非 build-only / infra-only
                 │  set image + rollout status  │  ── 调 k8s-deploy-services.sh
                 └──────────────┬───────────────┘
                 ┌──────────────▼───────────────┐
                 │ ⑧ 部署验证                    │  when: VERIFY_DEPLOY 且非 build-only
                 │  get nodes/pods/svc          │
                 └──────────────────────────────┘

 post 块：成功 → 打印"网关访问 http://任意K8s节点IP:30080"
         失败 → 打印 4 条排查清单（docker/kubectl、registry、containerd、pods 状态）
```

**`when` 条件的意义**：不是每次构建都要做全部事。`DEPLOY_MODE=deploy-only` 时 ③⑥ 直接跳过（用已有镜像只做部署）；`build-only` 时 ④⑤⑦⑧ 跳过。这就是"参数化构建"。

### 全局 options（新手容易忽略但很实用）

```groovy
options {
    timestamps()                                   // 日志带时间戳，算构建耗时
    disableConcurrentBuilds()                      // 禁止并发构建（两次同时部署会互相踩）
    buildDiscarder(logRotator(numToKeepStr: '20')) // 只保留最近 20 次构建历史
}
```

---

## 三、逐个 stage 精讲

### 3.1 参数区：一次构建的所有"旋钮"

```groovy
parameters {
    string(name: 'REGISTRY', defaultValue: '192.168.56.12:5000')   // 私有镜像仓库
    string(name: 'NAMESPACE', defaultValue: 'ai-customer-service') // K8s 命名空间
    string(name: 'VERSION', defaultValue: '')                      // 留空 = 用构建号
    choice(name: 'DEPLOY_MODE', choices: ['full','build-only','deploy-only','infra-only'])
    string(name: 'SERVICES', defaultValue: 'ai-cs-gateway ai-cs-user ...')  // 空格分隔，可只填一个
    booleanParam(name: 'SKIP_TESTS', defaultValue: true)
    booleanParam(name: 'DEPLOY_INFRA', defaultValue: false)        // 基础设施一般不动
    booleanParam(name: 'INIT_DATABASE', defaultValue: false)       // ⚠️ 重复勾会覆盖初始化
    password(name: 'OPENAI_API_KEY', ...)                          // password 类型不回显明文
}
environment {
    IMAGE_VERSION = "${params.VERSION ?: env.BUILD_NUMBER}"       // ?: 空则取构建号
}
```

**版本号设计**：`BUILD_NUMBER` 是 Jenkins 自动递增的构建号（1、2、3...）。每次构建的镜像版本都不同 → K8s 更新时能明确触发滚动更新，出问题也知道回滚到哪一版。这比全用 `latest` 靠谱（`latest` 无法追溯历史）。

### 3.2 写 Secret 的幂等技巧（stage ④）

```bash
kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic aics-secrets -n "$NAMESPACE" \
  --from-literal=openai-api-key="$OPENAI_API_KEY" \
  --from-literal=openai-base-url="$OPENAI_BASE_URL" \
  --from-literal=openai-model="$OPENAI_MODEL" \
  --dry-run=client -o yaml | kubectl apply -f -
```

`create` 命令本身不幂等（资源已存在会报错），这里用 `--dry-run=client -o yaml` 在本地生成 YAML 再交给 `apply`——**"生成式创建 + 声明式应用"**，流水线重复跑多少遍都不炸。这个技巧在写任何 CI 脚本时都通用。

### 3.3 Maven 测试在容器里跑（stage ③）

```bash
docker run --rm \
  -v "$PWD":/workspace \        # 代码挂进容器
  -v "$HOME/.m2":/root/.m2 \    # 本地 Maven 仓库挂进去（依赖缓存，第二次起不用重新下载）
  -w /workspace \
  maven:3.9-eclipse-temurin-17 \
  mvn test -B
```

好处：Jenkins 机器本身不用装 Maven/JDK，测试环境和构建镜像用的基础镜像保持一致（都是 temurin-17）。

### 3.4 post：失败时的"排查清单"比"失败"两个字有用

```groovy
failure {
    echo """
    1. Jenkins 机器是否能执行 docker 和 kubectl
    2. ${params.REGISTRY} 是否可访问
    3. 三台 Kubernetes 节点是否已配置 containerd 拉取 HTTP 私有仓库
    4. kubectl get pods -n ${params.NAMESPACE} 的具体错误
    """
}
```

第 3 条是本项目实战踩出来的坑：K8s 节点用 containerd（不是 Docker）拉镜像时，HTTP 私有仓库必须在 containerd 配置里声明为非 TLS 仓库（参考 `deploy/scripts/setup-containerd-insecure-registry.sh`），否则全部 Pod 卡 `ImagePullBackOff`。

---

## 四、幕后三个脚本：流水线只负责"调"，逻辑都在脚本里

```
Jenkinsfile（编排层）                 deploy/scripts/（执行层）
├── stage⑥ 构建并推送镜像  ──────►  k8s-build-push.sh
├── stage⑤ 部署基础设施    ──────►  k8s-apply-infra.sh
└── stage⑦ 部署业务服务    ──────►  k8s-deploy-services.sh
```

### 4.1 k8s-build-push.sh：模块名 → 镜像名的映射

项目模块叫 `ai-cs-gateway`，K8s 清单里的镜像叫 `api-gateway`，脚本里维护了映射：

| Maven 模块 | 镜像名 | 端口 |
|-----------|--------|------|
| ai-cs-gateway | api-gateway | 8080 |
| ai-cs-user | user-service | 8081 |
| ai-cs-knowledge | knowledge-service | 8082 |
| ai-cs-chat | ai-chat-service | 8083 |
| ai-cs-search | search-service | 8084 |
| ai-cs-message | message-service | 8085 |
| ai-cs-notify | notify-service | 8086 |

每个服务打**两个 tag** 并都推送：

```bash
docker build -f "${service}/Dockerfile" \
  -t "${REGISTRY}/aics/${image}:${VERSION}" \     # 带版本：用于滚动更新和回滚
  -t "${REGISTRY}/aics/${image}:latest" \         # latest：始终指向最新，方便本地拉取调试
  .                                               # 上下文 = 项目根目录（要 COPY ai-cs-common）
```

### 4.2 k8s-deploy-services.sh：滚动更新就两步

```bash
kubectl apply -f deploy/k8s/namespace.yaml
kubectl apply -f deploy/k8s/services/          # 先 apply 保证 Deployment 定义是最新的

for service in ${SERVICES}; do
  kubectl set image "deployment/${deployment}" \
      "${container}=${REGISTRY}/aics/${image}:${VERSION}" -n "${NAMESPACE}"
  kubectl rollout status "deployment/${deployment}" -n "${NAMESPACE}" --timeout=300s
done
```

- `set image` 改的是 Deployment 里的镜像版本 → 触发滚动更新（新 Pod Ready 才杀旧 Pod，原理见 [06-优雅停机](./06-优雅停机与零丢失滚动更新.md)）
- `rollout status --timeout=300s` 是**流水线的质量门禁**：新版本 5 分钟内没起来，这一步直接失败，流水线停下报错，不会带着故障继续跑下一个服务
- 服务按列表顺序逐个更新，天然形成部署顺序

### 4.3 k8s-apply-infra.sh：基础设施按依赖顺序上

```bash
kubectl apply -f deploy/k8s/mysql.yaml
kubectl rollout status statefulset/mysql-master ...   # MySQL 完全就绪才继续
[[ "$INIT_DATABASE" == "true" ]] && bash deploy/scripts/k8s-init-mysql.sh
kubectl apply -f nacos.yaml -f redis.yaml -f elasticsearch.yaml -f rocketmq.yaml ...
kubectl rollout status ...（逐个确认，|| true 容忍首次部署时的等待超时）
```

这就是 [02 篇](./02-Kubernetes入门.md)讲的依赖顺序在脚本里的落地：MySQL 最先（Nacos 要连它的 `nacos_config` 库），初始化 SQL 可选，然后其余中间件批量 apply。

---

## 五、四种部署模式：什么时候用哪个

| DEPLOY_MODE | 执行的 stage | 典型场景 |
|-------------|-------------|---------|
| `full` | ①②③⑥⑦⑧（+④） | 日常发版：代码变了，构建并上线 |
| `build-only` | ①②③⑥ | 只想验证"代码能不能构建出镜像"，不动集群 |
| `deploy-only` | ①②④⑤⑦⑧ | 集群重装后用已有镜像重新拉起；改了 YAML 要重新生效 |
| `infra-only` | ①②④⑤ | 首次搭建环境：只部署 MySQL/Nacos 等中间件 |

三个高频操作组合（在 Jenkins 页面 Build with Parameters 里勾选）：

```
【首次部署】infra-only + DEPLOY_INFRA✓ + INIT_DATABASE✓ → 跑完 → full
【日常发版】直接 full（默认参数）
【修了某个服务急发】full + SERVICES 只填那一个模块（如 ai-cs-chat）
```

---

## 六、搭建自己的流水线：从零跑通要过 4 道坎

```
① 装 Jenkins（Docker 方式最省事）
   docker run -d -p 8081:8080 -v jenkins_home:/var/jenkins_home \
     -v /var/run/docker.sock:/var/run/docker.sock \   # 让 Jenkins 容器能调用宿主机 docker
     jenkins/jenkins:lts
   ⚠️ 流水线里还要用 kubectl → Jenkins 容器里需另装 kubectl，或用含这两样工具的定制镜像

② 配 Pipeline Job
   新建 Item → Pipeline → 定义选 "Pipeline script from SCM" → 填仓库地址
   → Jenkins 会自动读取仓库根目录的 Jenkinsfile

③ 私有仓库打通（本项目 HTTP 仓库 192.168.56.12:5000）
   - Jenkins 机器：docker daemon 配 insecure-registries（setup-jenkins-docker-insecure-registry.sh）
   - K8s 节点：containerd 配非 TLS 仓库（setup-containerd-insecure-registry.sh）
   - 仓库本身：setup-local-registry.sh 一条命令拉起 registry:2 容器

④ 自动触发（可选）
   Jenkins Job → 构建触发器 → 勾 "GitHub hook trigger"
   GitHub 仓库 → Settings → Webhooks → 填 Jenkins 地址（http://jenkins:8081/github-webhook/）
   之后 git push 自动触发构建
```

---

## 七、离生产级流水线还差什么（演进路线）

```
当前（教学级）                      生产级
────────────────────────────────────────────────────────
SKIP_TESTS 默认 true        →    测试门禁：单测/覆盖率不过不许发版
无代码质量检查               →    SonarQube 扫描，阻断严重问题
镜像只推本地私有仓库          →    Harbor（漏洞扫描 + 镜像签名 + 权限）
部署失败靠人发现             →    钉钉/企微/邮件通知 + 自动回滚
一次全量替换（滚动更新）      →    金丝雀发布：先放 10% 流量验证再全量
凭据放在 Jenkins 参数        →    Jenkins Credentials + Vault 管理
```

---

## 动手练习

1. 通读根目录 `Jenkinsfile`，对着第二节的流程图把 8 个 stage 和各自 `when` 条件标注出来
2. 手动模拟 stage⑥：`docker build -f ai-cs-user/Dockerfile -t 192.168.56.12:5000/aics/user-service:v1 .`，再 `docker push`（仓库没搭就先搭 registry:2）
3. 手动模拟 stage⑦：`kubectl set image deployment/ai-chat-service ai-chat-service=...:v1`，然后 `kubectl rollout status` 看滚动更新过程
4. 思考题：如果把 `rollout status --timeout=300s` 这行从脚本里删掉，流水线会出现什么隐患？（提示：新版本起不来，流水线却显示成功，接着更新下一个服务）
5. （可选）用 Docker 起 Jenkins，配一个 Pipeline Job 指向本仓库，跑一次 `build-only`

---

## 学习检查清单

- [ ] 能画出本项目 8 个 stage 的流程图，并说出每个 `when` 条件的作用
- [ ] 理解 CI / 持续交付 / 持续部署的区别，以及本项目默认处于哪一级
- [ ] 理解版本号策略：BUILD_NUMBER 比 latest 好在哪
- [ ] 掌握 `--dry-run=client -o yaml | kubectl apply -f -` 的幂等技巧
- [ ] 知道三个 k8s-*.sh 脚本各自负责什么，`set image` + `rollout status` 就是滚动更新
- [ ] 理解四种 DEPLOY_MODE 的使用场景
- [ ] 知道 K8s 节点拉 HTTP 私有仓库要配 containerd insecure（实战大坑）
- [ ] 说出至少 4 条生产级流水线的演进方向

---

## 下一步

→ [04-Prometheus可观测性](./04-Prometheus可观测性.md)（部署上去之后，怎么知道它运行得好不好）
→ ../08-测试/（把 `SKIP_TESTS=false` 时的测试环节补强）
