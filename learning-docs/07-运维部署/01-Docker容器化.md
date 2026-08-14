# Docker 容器化

> 本项目所有微服务都通过 Docker 部署，基础设施用 Docker Compose 编排。
> 对应项目文件：各服务的 `Dockerfile`、根目录 `docker-compose.yml`、`deploy/` 目录

---

## 一、Docker 核心概念

```
【传统部署】                    【Docker 部署】
  应用 + 依赖 + OS               ┌─────────────────┐
  直接装在服务器上                │  Container      │
  → 环境不一致、部署慢            │  App + 依赖     │
                                ├─────────────────┤
                                │  Docker Engine  │
                                ├─────────────────┤
                                │  OS (Linux)     │
                                └─────────────────┘
                                → 环境一致、秒级部署
```

| 概念 | 类比 | 说明 |
|------|------|------|
| Image（镜像） | 安装包 | 只读模板，包含运行所需的一切 |
| Container（容器） | 运行中的程序 | 镜像的运行实例 |
| Dockerfile | 安装说明书 | 描述如何构建镜像 |
| Registry（仓库） | 应用商店 | 存放镜像的地方（Docker Hub） |
| Volume（数据卷） | 外接硬盘 | 容器数据持久化 |

---

## 二、本项目的 Dockerfile

```dockerfile
# ai-cs-chat/Dockerfile（每个微服务都有类似的）

# ===== 阶段一：运行环境 =====
FROM eclipse-temurin:17-jre-alpine

# 设置工作目录
WORKDIR /app

# 复制编译好的 JAR 包
COPY target/ai-cs-chat-1.0.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8083

# JVM 参数优化（容器环境）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 逐行解释

| 指令 | 作用 |
|------|------|
| `FROM eclipse-temurin:17-jre-alpine` | 基于轻量 JRE 17 镜像（约 100MB） |
| `WORKDIR /app` | 设置工作目录 |
| `COPY target/xxx.jar app.jar` | 把编译产物复制进去 |
| `EXPOSE 8083` | 声明端口（文档作用） |
| `ENV JAVA_OPTS=...` | 设置 JVM 参数 |
| `ENTRYPOINT [...]` | 容器启动时执行的命令 |

### 为什么用 alpine？

```
eclipse-temurin:17-jdk        → 约 450MB（包含编译器）
eclipse-temurin:17-jre-alpine → 约 100MB（只有运行时，更小更快）
```

---

## 三、Docker Compose 编排

### 3.1 什么是 Compose？

一个 YAML 文件定义多个容器，一条命令全部启动。

### 3.2 本项目的 docker-compose.yml 结构

```yaml
version: '3.8'

services:
  mysql:          # 服务名
    image: mysql:8.0
    container_name: aics-mysql
    ports:
      - "3306:3306"       # 宿主机端口:容器端口
    environment:
      MYSQL_ROOT_PASSWORD: root
    volumes:
      - mysql-data:/var/lib/mysql           # 数据持久化
      - ./deploy/mysql/init.sql:/docker-entrypoint-initdb.d/init.sql  # 初始化脚本
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 10
    networks:
      - aics-network

  redis:
    image: redis:7
    # ...

  nacos:
    image: nacos/nacos-server:v2.3.2
    depends_on:
      mysql:
        condition: service_healthy   # 等 MySQL 健康后再启动
    # ...

volumes:
  mysql-data:     # 命名数据卷（容器删除后数据还在）
  redis-data:
  es-data:

networks:
  aics-network:
    driver: bridge
```

### 3.3 关键配置解释

| 配置 | 作用 |
|------|------|
| `ports: "3306:3306"` | 端口映射（宿主机:容器） |
| `volumes` | 数据持久化 + 配置文件挂载 |
| `depends_on + condition` | 启动顺序控制 |
| `healthcheck` | 健康检查（判断服务是否就绪） |
| `networks` | 容器间通过服务名互访 |
| `environment` | 环境变量 |

---

## 四、常用 Docker 命令

### 4.1 容器管理

```bash
# 启动所有服务（后台运行）
docker-compose up -d

# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f mysql        # 跟踪 MySQL 日志
docker-compose logs --tail=100 nacos # 最近 100 行

# 停止所有服务
docker-compose down

# 停止并删除数据卷（⚠️ 数据会丢失）
docker-compose down -v

# 重启某个服务
docker-compose restart redis
```

### 4.2 进入容器

```bash
# 进入 MySQL 命令行
docker exec -it aics-mysql mysql -uroot -proot

# 进入 Redis CLI
docker exec -it aics-redis redis-cli

# 进入容器 Shell
docker exec -it aics-elasticsearch /bin/bash
```

### 4.3 镜像管理

```bash
# 构建镜像
docker build -f ai-cs-chat/Dockerfile -t ai-cs-chat:1.0 .

# 查看本地镜像
docker images

# 运行单个容器
docker run -d -p 8083:8083 --name chat ai-cs-chat:1.0

# 删除镜像
docker rmi ai-cs-chat:1.0
```

---

## 五、容器间网络通信

```
同一个 docker-compose 网络中的容器可以用服务名互访：

ai-cs-chat 容器内：
  连接 MySQL  → jdbc:mysql://mysql:3306/...     （不是 localhost！）
  连接 Redis  → redis:6379
  连接 Nacos  → nacos:8848
  连接 ES     → elasticsearch:9200

宿主机访问：
  MySQL  → localhost:3306
  Nacos  → localhost:8848
```

---

## 六、多阶段构建（进阶）

```dockerfile
# 更完整的 Dockerfile（编译 + 运行分离）

# 阶段一：编译
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-chat/pom.xml ai-cs-chat/
RUN mvn dependency:go-offline -B          # 先下载依赖（利用缓存）
COPY . .
RUN mvn clean package -DskipTests -pl ai-cs-chat -am -B

# 阶段二：运行（最终镜像很小）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/ai-cs-chat/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**好处**：最终镜像不包含 Maven、源码，只有 JRE + JAR，更小更安全。

---

## 七、动手练习

1. `docker-compose up -d` 启动所有基础设施
2. `docker-compose ps` 查看所有容器状态
3. `docker exec -it aics-mysql mysql -uroot -proot` 进入 MySQL
4. 修改 docker-compose.yml 中 Redis 端口为 6380，重启验证
5. 构建一个微服务镜像并运行

---

## 学习检查清单

- [ ] 理解镜像、容器、数据卷的概念
- [ ] 会写基本的 Dockerfile
- [ ] 理解 docker-compose.yml 的结构
- [ ] 掌握常用 docker 命令
- [ ] 理解容器间网络通信（服务名访问）
- [ ] 理解 healthcheck 和 depends_on 的作用
- [ ] 了解多阶段构建优化镜像大小

---

## 下一步

→ [02-Kubernetes入门](./02-Kubernetes入门.md)
