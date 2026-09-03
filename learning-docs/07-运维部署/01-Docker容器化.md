# Docker 容器化：搞懂本项目的镜像与 Compose 编排

> 对应项目文件：各微服务的 `Dockerfile`（9 个）、`deploy/docker-compose/` 目录（5 套编排）、`deploy/mysql/`、`deploy/redis/` 等配置
> 下一篇 [02-Kubernetes入门](./02-Kubernetes入门.md) 会把"单机 Compose"升级为"集群 K8s"，这篇先打牢 Docker 地基。

---

## 零、先定位：Docker 在整套部署逻辑里的位置

回忆 [02](./02-Kubernetes入门.md) 第零节那条 6 步链路，Docker 负责其中的 ②③④：

```
① git push
② mvn package → jar
③ docker build → 镜像          ←── 本篇第 2 节：Dockerfile 怎么写、怎么跑
④ docker push → 镜像仓库        ←── 镜像造好了，供 K8s/其他机器拉取
⑤ kubectl apply / docker compose up
⑥ 服务可访问

【本篇另外两块内容】
  - 一台机器上怎么把十几个容器一起管起来 → docker compose（第 3 节）
  - 容器之间怎么互访、数据怎么不丢       → network / volume（第 4 节）
```

一句话：**Docker 解决"把我这段代码 + 它依赖的环境打包成一个标准件"，Compose 解决"单机上把一堆标准件编排起来"。**

---

## 一、核心概念：5 个词说清楚

| 概念 | 类比 | 本项目实体 |
|------|------|-----------|
| Image 镜像 | 软件安装包（只读模板） | `ai-cs-chat:latest`、`mysql:8.0` |
| Container 容器 | 运行中的程序（镜像的实例） | `docker ps` 里的 `aics-mysql`、`aics-chat-service` |
| Dockerfile | 安装说明书（怎么造镜像） | `ai-cs-chat/Dockerfile` 等 9 个 |
| Registry 仓库 | 应用商店（存镜像） | Docker Hub（中间件镜像）+ `192.168.56.12:5000`（本项目私有仓库，存自建镜像） |
| Volume 数据卷 | 外接硬盘 | `mysql-data` 卷，容器删了数据还在 |

另外两个高频词，用本项目的例子记：

```
构建上下文（build context）：docker build 最后那个 "." 
  docker build -f ai-cs-chat/Dockerfile -t xxx .   ← 这个 "." 就是上下文 = 项目根目录
  Dockerfile 里 COPY 的文件，都从这个目录出发找（所以能 COPY 到 ai-cs-common/）

Docker 网络：同一个 compose 网络里的容器可以用"服务名"当域名互访
  ai-chat-service 容器里连数据库写 jdbc:mysql://mysql:3306/...（不是 localhost！）
```

---

## 二、精读本项目的 Dockerfile（多阶段构建）

9 个微服务的 Dockerfile 结构完全一样，以 `ai-cs-chat/Dockerfile` 为例：

```dockerfile
# ========== 阶段一：builder（编译，最终会被丢弃） ==========
FROM maven:3.9-eclipse-temurin-17 AS builder    # 带 Maven + JDK 17 的"重型"环境
WORKDIR /build

# 先只复制 pom 文件，预下载依赖 —— 这一步是为了利用 Docker 层缓存
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-chat/pom.xml ai-cs-chat/
RUN mvn dependency:go-offline -B -pl ai-cs-chat -am

# 再复制源码，真正编译
COPY ai-cs-common/ ai-cs-common/
COPY ai-cs-chat/ ai-cs-chat/
RUN mvn package -pl ai-cs-chat -am -DskipTests -B

# ========== 阶段二：运行时（最终镜像 = 这一层） ==========
FROM eclipse-temurin:17-jre-jammy               # 只有 JRE，没有 Maven/源码
WORKDIR /app
COPY --from=builder /build/ai-cs-chat/target/*.jar app.jar   # 只把 jar 搬过来
EXPOSE 8083                                     # 声明端口（文档作用 + K8s 参考）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]  # 启动命令，JVM 参数从环境变量注入
```

### 2.1 构建过程流程图

```
docker build -f ai-cs-chat/Dockerfile -t 192.168.56.12:5000/aics/ai-chat-service:latest .
                                          │
   ┌──────────────────────────────────────┘
   ▼
[阶段1 builder]  maven:3.9-temurin-17（约 500MB 的"工地"）
   ├─ 第1层 COPY 3 个 pom            ← 代码没变时这层缓存命中，跳过下载
   ├─ 第2层 mvn dependency:go-offline ← 下载全部依赖（最耗时，但只在 pom 变了才重跑）
   ├─ 第3层 COPY 源码                 ← 每次改代码这层失效
   └─ 第4层 mvn package               ← 产出 ai-cs-chat/target/*.jar
   │
   ▼  只搬走 jar，"工地"整个丢弃
[阶段2 运行时]  eclipse-temurin:17-jre-jammy（约 200MB）
   └─ COPY --from=builder .../*.jar
   = 最终镜像：JRE + 一个 jar，干净且小
```

### 2.2 三个关键设计，每个都是面试点

**① 为什么多阶段？**
编译需要 Maven + JDK（约 500MB+），运行只需要 JRE。多阶段让最终镜像不含编译器和源码：更小（拉取快）、更安全（攻击面小）。

**② 为什么先 COPY pom 再 COPY 源码？（层缓存优化）**
Docker 每条指令是一层，某层没变就直接用缓存。日常改的是代码、很少动 pom——把"下载依赖"放在"复制源码"之前，改代码时第 1、2 层缓存命中，构建从十几分钟降到几十秒。

**③ 为什么 ENTRYPOINT 里是 `${JAVA_OPTS:-}`？**
容器参数和镜像解耦：镜像里不写死 JVM 参数，运行时通过环境变量注入（K8s 清单里 `JAVA_OPTS: "-Xms512m -Xmx512m"` 就是这样传进去的）。`${JAVA_OPTS:-}` 表示没设置就用空串。

### 2.3 动手构建（注意上下文）

```bash
# 必须在项目根目录执行！因为 Dockerfile 要 COPY ai-cs-common/
docker build -f ai-cs-chat/Dockerfile -t ai-cs-chat:local .

# 验证镜像能跑起来
docker run -d -p 8083:8083 \
  -e NACOS_ADDR=127.0.0.1:8848 \
  -e JAVA_OPTS="-Xms256m -Xmx256m" \
  --name chat-test ai-cs-chat:local

docker logs -f chat-test        # 看启动日志
docker stop chat-test && docker rm chat-test
```

---

## 三、docker compose：一台机器上编排整套系统

### 3.1 本项目有 5 套 Compose 文件，各有用途

```
deploy/docker-compose/
├── docker-compose-all.yml            ★ 单机全量：5 个中间件 + 7 个微服务一锅端（入门用它）
├── docker-compose-host1/2/3.yml      分布式：拆到 3 台虚拟机（对应 Jenkins 多主机部署）
├── docker-compose-master-slave.yml   MySQL 一主两从版（配合 deploy/mysql/slave.cnf）
└── docker-compose-observability.yml  观测栈：Prometheus + Grafana + Tempo + OTel Collector
```

### 3.2 精读 docker-compose-all.yml 的 MySQL 段（每行都有用）

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0                    # 直接用官方镜像，不用自己 build
    container_name: aics-mysql          # 容器名（docker exec 时用）
    restart: always                     # 容器挂了自动重启（单机版的自愈）
    ports:
      - "3306:3306"                     # 宿主机:容器 —— 外部工具（Navicat）连 localhost:3306
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root}   # 从 .env 或环境变量取，默认 root
      TZ: Asia/Shanghai
    volumes:
      - mysql-data:/var/lib/mysql       # ① 数据卷：数据落盘，容器删了不丢
      - ../mysql/mysql.cnf:/etc/mysql/conf.d/mysql.cnf     # ② 配置挂载：改配置不用重打镜像
      - ../mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # ③ 首次启动自动建库建表
    healthcheck:                        # ④ 健康检查：决定 mysql 服务算不算"就绪"
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-uroot", "-p..."]
      interval: 10s                     # 每 10s 检查一次
      retries: 10                       # 连续失败 10 次才算不健康
      start_period: 30s                 # 启动宽限期（MySQL 初始化慢，先不计时）
    networks:
      - aics-network

volumes:
  mysql-data:                           # 命名卷声明
networks:
  aics-network:
    driver: bridge
```

### 3.3 启动顺序：depends_on + condition 是编排的灵魂

微服务依赖十几个中间件，启动顺序错了全是连接报错。本项目用 `depends_on` 声明依赖：

```
mysql (healthcheck 通过)
  └─→ nacos (等 condition: service_healthy)
        └─→ rocketmq-broker (等 namesrv)
              └─→ 7 个微服务（等 mysql + nacos + redis + ... 都健康）

docker compose up -d 会自动按这个拓扑图排序启动：
  先起没有依赖的（mysql、redis、namesrv、es、minio）
  → 等健康检查通过 → 再起依赖它们的（nacos → broker → 微服务）
```

对比 K8s：K8s **没有** `depends_on`，靠探针 + 重试达到同样效果（Pod 起来连不上就重启重试）——这是两套体系的一个典型思维差异，下一篇会再对照。

---

## 四、容器间怎么互访？（本项目最常用的通信图）

同一个 compose 网络（`aics-network`）内，**服务名就是 DNS**：

```
【容器之间】用服务名，绝不用 localhost
┌──────────────────────── aics-network ────────────────────────┐
│                                                               │
│  aics-chat-service ──── mysql:3306    ────→  aics-mysql       │
│       │                 redis:6379    ────→  aics-redis       │
│       │                 nacos:8848    ────→  aics-nacos       │
│       └── elasticsearch:9200 ────→  aics-elasticsearch        │
│                                                               │
│  （配置里写的 NACOS_ADDR=nacos:8848，nacos 就是服务名）          │
└───────────────────────────────────────────────────────────────┘

【宿主机访问容器】走 ports 端口映射
  Navicat 连 MySQL  → localhost:3306
  浏览器开 Nacos    → localhost:8848/nacos
  压测网关          → localhost:8080

【容器访问宿主机】（服务跑在宿主机、Docker 里起中间件时用）
  host.docker.internal（Linux 上 compose 里要加 extra_hosts 映射）
  ── observability 那套 compose 抓宿主机服务的指标就是这么干的
```

---

## 五、常用命令手册（本项目实景版）

```bash
cd deploy/docker-compose

# ===== 生命周期 =====
docker compose -f docker-compose-all.yml up -d      # 全量启动（后台）
docker compose -f docker-compose-all.yml ps         # 状态 + 健康情况
docker compose -f docker-compose-all.yml down       # 全部停止并删容器（卷保留）
docker compose -f docker-compose-all.yml down -v    # ⚠️ 连数据卷一起删，数据库清空
docker compose -f docker-compose-all.yml restart redis

# ===== 排障 =====
docker compose -f docker-compose-all.yml logs -f nacos       # 跟某个服务日志
docker compose -f docker-compose-all.yml logs --tail=100 ai-cs-chat
docker exec -it aics-mysql mysql -uroot -proot               # 进 MySQL 命令行
docker exec -it aics-redis redis-cli                          # 进 Redis
docker inspect aics-chat-service | grep -A5 Health           # 看健康检查结果

# ===== 镜像 =====
docker images                          # 本地镜像列表
docker rmi ai-cs-chat:local            # 删镜像
docker save -o chat.tar ai-cs-chat:latest   # 导出镜像文件（离线分发用）
docker load -i chat.tar
```

---

## 六、Docker/Compose 概念 → K8s 概念对照（为下一篇铺路）

| Compose 写法 | K8s 对应 | 说明 |
|--------------|----------|------|
| `services:` 下的一项 | Deployment + Service | K8s 把"运行"和"访问入口"拆成两个对象 |
| `depends_on + healthcheck` | readinessProbe + 重试 | K8s 无依赖声明，靠探针+自愈达成同样效果 |
| `restart: always` | 控制器自动重建 | Deployment 挂了拉起新 Pod（不是重启旧容器） |
| named volume | PVC（volumeClaimTemplates） | K8s 有存储供应商（StorageClass）动态供盘 |
| `.env` / environment | ConfigMap / Secret | 敏感值进 Secret |
| `ports: "3306:3306"` | NodePort（集群内则是 ClusterIP） | K8s 网络分段更严格 |
| 单机 | 多机集群 + 调度 | K8s 决定容器跑在哪台机器上 |

看懂这张表，02 篇的 YAML 就不再陌生。

---

## 动手练习

1. `docker compose -f docker-compose-all.yml up -d` 全量启动，用 `ps` 数一数起了几个容器、哪些在等健康检查
2. 观察启动顺序：`up -d` 后立刻连续执行几次 `docker compose ps`，对照 3.3 的依赖图
3. 断容器自愈：`docker stop aics-redis && docker start aics-redis`；再试 `docker rm -f aics-redis`（restart 策略管不管被删的容器？想清楚再看答案——不管，`restart` 只管容器进程挂掉，删了就是删了。K8s 的自愈更强大，见下一篇）
4. 修改 `ai-cs-chat/Dockerfile` 里某行，重新 build，体会层缓存（对比改 pom 前后的构建耗时）
5. `docker exec` 进入 MySQL，`show databases;` 验证 init.sql 建了哪些库
6. 给 compose 里的 Redis 加一个健康检查（参考 MySQL 的写法）

---

## 学习检查清单

- [ ] 能画出"代码 → jar → 镜像 → 仓库 → 容器"的链路，说清每步的命令
- [ ] 理解多阶段构建的两个目的（镜像小 + 层缓存快），知道为什么先 COPY pom
- [ ] 知道构建上下文（那个 `.`）是什么，为什么 build 必须在项目根目录
- [ ] 会读 compose 的 healthcheck / depends_on / volumes / ports 四件套
- [ ] 理解容器互访用服务名、宿主机访问走端口映射、容器回宿主机用 host.docker.internal
- [ ] 记住了 `down -v` 会删数据，和生产误操作后果
- [ ] 能说出 Compose 和 K8s 的分工边界（单机编排 vs 集群编排）

---

## 下一步

→ [02-Kubernetes入门](./02-Kubernetes入门.md)（同样的镜像，从单机升级到集群编排）
