# AI 客服平台 - 本地启动指南（tools）

本目录提供**一键启动 / 一键停止**所有服务（基础设施 + 11 个微服务 + 前端）的脚本与配置。

## 架构总览

```
tools\start-all.ps1
   ├─ 1. Nacos 2.3.2        (JDK8, 端口 8848/9848)   tools\nacos
   ├─ 2. RocketMQ 5.1.4     (JDK17, 9876/10911)       tools\rocketmq
   ├─ 3. 发布 Nacos 配置     (tenant=aics)             tools\nacos-config\*.yml
   ├─ 4. Maven 构建后端      (mvn clean install -DskipTests)
   ├─ 5. 启动 11 个微服务    (8081~8090 + 8080 gateway)
   └─ 6. 启动前端 Vite      (5173)                    ai-cs-frontend
```

| 组件 | 端口 | 说明 |
| --- | --- | --- |
| Nacos | 8848 / 9848 | 配置中心 / 注册中心（控制台 `http://localhost:8848/nacos`，账号 `nacos/nacos`） |
| RocketMQ NameServer | 9876 | 消息队列 |
| RocketMQ Broker | 10911 | 消息队列 |
| ai-cs-user | 8081 | 用户 |
| ai-cs-knowledge | 8082 | 知识库 |
| ai-cs-chat | 8083 | 对话 / 图片对话 |
| ai-cs-search | 8084 | 搜索 |
| ai-cs-message | 8085 | 消息 |
| ai-cs-notify | 8086 | 通知 |
| ai-cs-order | 8087 | 订单 |
| ai-cs-product | 8088 | 商品 |
| ai-cs-pay | 8089 | 支付 |
| ai-cs-mq | 8090 | MQ 消费者 |
| ai-cs-gateway | 8080 | 网关（`http://localhost:8080/api/...`） |
| 前端 Vite | 5173 | `http://localhost:5173` |

## 快速开始

```powershell
# 1. 换机后先改机器相关路径（JDK8/JDK17/Maven/数据库密码）
notepad .\tools\env.ps1

# 2. 一键启动全部（首次会自动 mvn 构建，耗时较长）
powershell -ExecutionPolicy Bypass -File .\tools\start-all.ps1

# 3. 停止全部
powershell -ExecutionPolicy Bypass -File .\tools\stop-all.ps1
```

也可以双击 `tools\start-all.bat` 启动（保留命令行窗口，按任意键退出）。

## 常用参数（start-all.ps1）

| 参数 | 作用 |
| --- | --- |
| `-SkipBuild` | 跳过 `mvn clean install`（二次启动更快） |
| `-InfraOnly` | 只启动 Nacos/RocketMQ 并发布配置，不启服务 |
| `-SkipInfra` | 假设基础设施已启动，只启后端+前端 |
| `-Service ai-cs-user` | 只启动单个后端服务（配合 `-SkipInfra -SkipBuild` 用于重启单个服务） |
| `-NoFrontend` | 不启动前端 |

脚本是**幂等**的：已在监听的端口会自动跳过，重复执行安全。

## 换机部署步骤（另一台电脑）

1. 把整个仓库（含 `tools\`）拷贝到新电脑任意目录。
2. 安装 JDK8、JDK17、Maven，然后编辑 `tools\env.ps1` 三个路径：
   ```powershell
   $JAVA8_HOME  = "你的 JDK8 目录"
   $JAVA17_HOME = "你的 JDK17 目录"
   $MAVEN_HOME  = "你的 Maven 目录"
   $DB_PASSWORD = "远程 MySQL 密码"
   ```
   三个路径也可以不填，脚本会自动从常见安装目录 / 环境变量 `JAVA8_HOME`/`JAVA17_HOME`/`MAVEN_HOME`/`JAVA_HOME` 探测。
3. 保证远程 MySQL（123.60.31.79）可达。
4. 运行 `tools\start-all.ps1`。

## 日志位置

| 内容 | 位置 |
| --- | --- |
| 后端 / 前端进程日志 | `tools\logs\<服务名>.out.log` / `.err.log` |
| 启动 PID 记录 | `tools\logs\<服务名>.pid` |
| Nacos 日志 | `tools\nacos\logs\` |
| RocketMQ 日志 | `tools\rocketmq\logs\` |

## Nacos 配置发布说明

配置源文件在 `tools\nacos-config\*.yml`，启动脚本会用：

```
curl.exe --data-urlencode "content@<文件>"
```

把文件**原样字节**发布到 Nacos（tenant=`aics`），避免 PowerShell 5.1 的 GBK 读取导致中文注释乱码。
如果手动改过配置想单独重发：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\publish-nacos-config.ps1
```

> 注意：不要用 `Invoke-RestMethod -Body @{content=...}` 发含中文的配置，PS5.1 会按 GBK 编码入库导致乱码。

## 常见问题

- **JDK 8 not found / JDK 17 not found / Maven not found**：编辑 `tools\env.ps1` 填写正确路径。
- **服务启动但一直 DOWN**：看 `tools\logs\<服务名>.err.log`，常见原因是远程 MySQL/Redis 不可达、Nacos 配置未发布。
- **chat 图片对话报“图片地址无效”**：`ai-cs-chat.yml` 里 `aics.vision.allowed-image-host` 必须包含图片 URL 的主机（MinIO 地址）。
- **配置修改后没生效**：改 `tools\nacos-config\*.yml` 后重跑 `start-all.ps1`（或手动 `publish-nacos-config.ps1`），并重启对应服务。
- **端口被占用**：先 `stop-all.ps1`，或用 `netstat -ano | findstr <端口>` 查占用进程。

## 测试账号

- Nacos 控制台：`nacos / nacos`
- 前端登录：`admin / admin123`（接口 `POST http://localhost:8080/api/user/login`）