# 13-Nginx 与 OpenResty：从零开始理解反向代理（认知拓展，工程未部署）

> **定位**：本仓 `deploy/` 里没有任何 Nginx/Ingress 清单（grep 实证），流量路径是"客户端 → K8s Service → Spring Cloud Gateway(8080)"。但 **Nginx 是所有部署岗 JD 的默认词**——Ingress-Nginx、OpenResty 系网关（Kong/APISIX）底层全是它。本篇按仓库纪律写成**认知拓展**：不引入、只建坐标系，并给出"如果要引入，接在哪"的评估。
> 前置阅读：[02-Spring微服务/04-SpringCloudGateway网关](../02-Spring微服务/04-SpringCloudGateway网关.md)（本仓网关）、[15-计算机基础/03-文件系统与IO多路复用](../15-计算机基础/03-文件系统与IO多路复用.md)（epoll——Nginx 高并发的底座）、[15-计算机基础/05-HTTP与TLS](../15-计算机基础/05-HTTP与TLS.md)。

---

## ⚡ 30 秒速记卡

```text
① 反向代理站在"服务端这一侧"替一群服务器接客；正向代理站在"客户端这一侧"替你出门办事
② Nginx = master-worker 进程模型 + epoll 事件驱动：少量 worker 吃下几万并发连接
③ 四个核心词：server(虚拟主机) / location(路由规则) / upstream(上游池) / proxy_pass(转发)
④ 限流三层分工：Nginx 管入口粗粒度 / Gateway 管路由级 / Sentinel 管接口级——本项目后两层已有
⑤ OpenResty = Nginx + LuaJIT：把"配置"升级成"可编程"，Kong/APISIX 皆基于此
```

---

## 一、正向代理 vs 反向代理：先分清"谁站在谁那边"

| | 正向代理 | 反向代理 |
|---|---|---|
| 站在哪 | **客户端**一侧 | **服务端**一侧 |
| 谁知道它的存在 | 客户端配置它，服务端不知道真实客户端 | 客户端只知道它，不知道后面有哪些服务器 |
| 典型用途 | 科学上网、公司出口审计 | 负载均衡、TLS 终结、静态资源、防护 |
| 类比 | 你雇的跑腿（替你出门） | 公司前台（替一群员工接客） |

**与本项目衔接**：你的九站旅程（[02-Spring微服务/17](../02-Spring微服务/17-SpringMVC请求全链路与参数校验.md)）从"Tomcat 接连接"开始；如果前面加了 Nginx，旅程就变成 **Nginx → Gateway → Tomcat**，且 Nginx 这站不解析业务，只做转发/缓存/卸载。

---

## 二、进程模型：为什么一台 Nginx 能扛几万并发

```text
master 进程（1 个）
 ├── 读配置、绑定 80/443 端口
 ├── fork 出 N 个 worker（一般 = CPU 核数）
 └── 平滑重启：新旧 worker 并存，旧的处理完存量连接再退出

worker 进程（N 个）
 └── 每个 worker 单线程 + epoll 事件循环：
     一个连接"事件就绪才处理"，不等 I/O
     → 线程数不随连接数增长（对比：Tomcat 一请求一线程，200 线程就是上限）
```

对比记忆：**Tomcat 是"一客一服务员"（阻塞线程池），Nginx 是"一个服务员看全场，谁举手服务谁"（事件驱动）**。这也解释了为什么反向代理、静态资源这类"轻 I/O"活归 Nginx，重业务归 Tomcat。

---

## 三、四个核心词 + 一份可用的最小配置

| 词 | 类比 | 作用 |
|---|---|---|
| `server` | 一家分店 | 一个虚拟主机（按域名/端口区分） |
| `location` | 分店里的柜台指引 | 按 URL 前缀/正则分流 |
| `upstream` | 后厨列表 | 定义一组上游服务器 + 负载均衡策略 |
| `proxy_pass` | 转单动作 | 把请求转发给 upstream/指定地址 |

```nginx
# 最小可用：反代本项目的网关（假设部署在网关前面）
upstream aics_gateway {
    server 10.0.0.11:8080 max_fails=3 fail_timeout=10s;   # 节点健康剔除
    server 10.0.0.12:8080;
    keepalive 64;                                          # 与上游保持长连接
}

server {
    listen 443 ssl;
    server_name aics.example.com;

    # TLS 终结：https 在这里解掉，内网走 http（证书只在这里管）
    ssl_certificate     /etc/nginx/certs/aics.pem;
    ssl_certificate_key /etc/nginx/certs/aics.key;

    # 静态资源直接由 Nginx 服（不进 Java）
    location /static/ {
        root /var/www/aics;
        expires 7d;
    }

    # 业务流量 → 网关
    location / {
        proxy_pass http://aics_gateway;
        proxy_http_version 1.1;
        proxy_set_header Connection "";                    # keepalive 必配
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
    }

    # WebSocket/SSE 透传（升级头必须透传，对应你 notify 的 /ws/notify 与 chat 的 SSE）
    location /ws/ {
        proxy_pass http://aics_gateway;
        proxy_http_version 1.1;
        proxy_set_header Upgrade    $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 300s;                           # 长连接别被 60s 默认值掐断
    }
}
```

> **必背坑位**：SSE/WebSocket 过 Nginx 必须透传 `Upgrade/Connection` 头并调大 `proxy_read_timeout`——
> 这与你 [04-中间件/05-SSE与WebSocket实时通信](05-SSE与WebSocket实时通信.md) 里"网关要透传 upgrade"是同一件事在不同层的重演。

---

## 四、负载均衡与限流

### 4.1 上游调度策略

| 策略 | 配置 | 适用 |
|---|---|---|
| 轮询（默认） | 无 | 节点同质 |
| 权重 | `server x weight=3` | 机器配置不均 |
| ip_hash | `ip_hash;` | 无共享 Session 的老应用粘性 |
| least_conn | `least_conn;` | 请求耗时不均 |
| 一致性哈希 | `hash $arg_userId consistent;` | 本地缓存命中敏感的场景 |

### 4.2 限流：Nginx 的 `limit_req`（漏桶）

```nginx
limit_req_zone $binary_remote_addr zone=api:10m rate=20r/s;

location /api/ {
    limit_req zone=api burst=40 nodelay;   # 稳态 20r/s，桶容量 40，不排队直接拒
}
```

**三层限流分工（面试可以直接背）**：

| 层 | 组件 | 粒度 | 本项目现状 |
|---|---|---|---|
| 入口 | Nginx `limit_req` | IP/全局 | ❌ 未部署 |
| 路由 | Spring Cloud Gateway `RequestRateLimiter`（Redis Lua 令牌桶） | 路由级 | ✅ `replenish-rate=5/burst=10` |
| 接口 | Sentinel 规则 | 资源级（`chat_send` 等） | ✅ `SentinelFlowConfig` |

---

## 五、OpenResty：当配置不够用，就把代码嵌进 Nginx

`OpenResty = Nginx + LuaJIT`。请求生命周期的各阶段都能插 Lua：

```lua
-- access_by_lua：在"进上游之前"执行任意逻辑（鉴权/灰度/限流）
access_by_lua_block {
    local gray = ngx.var.cookie_gray or "0"
    if gray == "1" then
        ngx.var.upstream = "aics_gateway_gray"   -- 灰度用户转新集群
    end
}
```

- 你学过的 API 网关 **Kong、APISIX 都是 OpenResty 系**——理解了"Lua 挂在 Nginx 生命周期上"，就理解了它们的插件模型；
- 与 Spring Cloud Gateway 的对照：SCG 是 **Java/Reactive**（你的技术栈内、易写业务过滤），OpenResty 系是 **C+Lua**（性能高、生态插件多）。选型口径：**Java 团队自维护逻辑 → SCG；平台化/超高性能入口 → OpenResty 系**。

---

## 六、如果要引入本项目：接入点评估（未落地，仅方案）

| 方案 | 做法 | 收益 | 代价 |
|---|---|---|---|
| K8s Ingress-Nginx | `deploy/k8s/` 加 Ingress 资源指向 gateway Service | TLS 终结收口、域名/路径路由声明化 | 需集群装 ingress-nginx 控制器；多一层排障面 |
| 裸 Nginx（虚拟机部署） | 前置 443 → 反代 8080 | 静态资源卸载、入口限流 | 脱离 K8s 生命周期，配置漂移风险 |

诚实结论：**当前规模（单集群、Service 直出）收益有限，属于"部署成熟度"项而非"必需品"**——先学再用。

---

## 七、动手实验（10 分钟）

```bash
docker run -d --name ngx -p 8081:80 nginx:1.27
# 容器内替换 /etc/nginx/conf.d/default.conf：proxy_pass http://host.docker.internal:8080;
# 浏览器访问 http://localhost:8081 → 应看到网关 401（说明已转发到 Spring Cloud Gateway）
docker exec ngx nginx -s reload    # 改配置后热加载：master 收信号，新 worker 起来旧的退
```

观察点：`curl -v` 看响应头 `Server: nginx`；`tail -f /var/log/nginx/access.log` 认识日志格式。

---

## 八、面试要点总结

```text
关键词：
正向/反向代理之别 · master-worker + epoll（一请求一线程 vs 事件驱动）
server/location/upstream/proxy_pass · TLS 终结 · 平滑重启（新 worker 并存）
limit_req 漏桶 burst/nodelay · WebSocket/SSE 必透传 Upgrade 头 + read_timeout
OpenResty= Nginx+LuaJIT（Kong/APISIX 底座）· 三层限流：入口/网关/接口
项目锚点：deploy 无 Nginx/Ingress（诚实标注）· SCG replenish-rate=5/burst=10 · SentinelFlowConfig
```

## 学习检查清单

- [ ] 能一句话区分正向/反向代理，并说出各一个用途
- [ ] 能解释 master-worker 模型与平滑重启的关系
- [ ] 能写出"反代 + WebSocket 透传"的最小配置
- [ ] 能说清三层限流分工与本项目的现状
- [ ] 能讲 OpenResty 与 SCG 的选型边界

## 下一步

- [14-SkyWalking 链路追踪](14-SkyWalking链路追踪.md)：请求穿过 Nginx/网关之后，链路怎么被"自动"记录；
- [04-中间件/09-AI与治理中间件部署](09-AI与治理中间件部署.md)：本仓真实部署的治理组件全景。
