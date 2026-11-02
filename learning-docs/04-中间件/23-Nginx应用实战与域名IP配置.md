# 23-Nginx 应用实战与域名 IP 配置：从配置到上线

> **定位**：[13-Nginx与OpenResty反向代理](13-Nginx与OpenResty反向代理.md) 讲的是"认知坐标系"（是什么、为什么快），本篇是**动手配置篇**：Nginx 到底怎么用、负载均衡到底怎么配、域名和 IP 到底怎么关联起来——每一步给出可直接复制的配置和验证命令。内容不绑定本仓，通用性优先。
> 前置阅读：[13-Nginx与OpenResty反向代理](13-Nginx与OpenResty反向代理.md)（master-worker / epoll / 四个核心词）。

---

## ⚡ 30 秒速记卡

```text
① Nginx 使用三件事：装好 → 写 server/location → nginx -s reload 热生效（不掉连接）
② 负载均衡 = upstream 定义后端池 + location 里 proxy_pass 指向池名；策略默认轮询，常用 weight / ip_hash / least_conn
③ 域名→IP 靠 DNS 解析（本地开发先用 hosts 文件）；IP→Nginx 靠 server_name 匹配请求的 Host 头
④ 一个公网 IP 挂多个域名 = 多个 server 块，按 server_name 区分；匹配不上就落到 default_server
⑤ 上线五步：买域名 → DNS 解析 A 记录到服务器公网 IP → 备案（国内 80/443 必须）→ 安全组放行端口 → Nginx 绑域名
```

---

## 一、Nginx 怎么用：从安装到第一个可用配置

### 1.1 安装（三种方式选一种）

| 方式 | 命令 | 适合 |
|---|---|---|
| Docker（推荐练手） | `docker run -d --name nginx -p 80:80 nginx:1.27` | 本地实验、不污染环境 |
| Linux 包管理 | `apt install nginx` / `yum install nginx` | 云服务器直接部署 |
| Windows | 官网下载 zip，解压后双击 `nginx.exe` | 只适合开发调试 |

### 1.2 认识配置文件结构

```nginx
# /etc/nginx/nginx.conf —— 全部配置就五层壳
...全局块...              # worker 进程数、日志路径、user
events { ... }            # 每个 worker 的连接数上限
http {
    ...http 全局...       # 日志格式、gzip、mime 类型
    upstream 后端池 { ... }   # ← 负载均衡在这定义

    server {              # ← 一个虚拟主机（一个域名/端口）
        listen 80;
        server_name example.com;

        location /api/ {  # ← URL 路由规则
            proxy_pass http://后端池;
        }
        location /static/ {
            root /var/www;
        }
    }

    server { ... }        # 第二个域名就再写一个 server
}
```

实际项目中全局的 `http` 块一般保持不动，自己的配置写在 `/etc/nginx/conf.d/*.conf`（会被 `include` 进来），一域名一文件，改坏了也好回滚。

### 1.3 日常命令（背下来）

| 命令 | 作用 |
|---|---|
| `nginx` | 启动 |
| `nginx -t` | **改完配置先跑它**，只检查语法不生效 |
| `nginx -s reload` | 热加载：新 worker 起来、旧 worker 处理完存量连接再退（**不断服务**） |
| `nginx -s stop` | 快速停止（强行掐连接） |
| `nginx -s quit` | 优雅停止（等请求处理完） |
| `nginx -T` | 打印最终生效的完整配置（排查 include 问题神器） |

### 1.4 第一个实战：静态资源托管

```nginx
server {
    listen 80;
    server_name static.example.com;

    location / {
        root /var/www/static;   # 拼出路径 = /var/www/static/xxx
        index index.html;
        expires 7d;             # 浏览器缓存 7 天
    }
}
```

验证：把一个 `index.html` 放进 `/var/www/static/`，`curl http://static.example.com/`（配好 hosts 后）能看到内容即成功。

### 1.5 第二个实战：反向代理到后端服务

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;   # 转给本机 8080 的 Java 服务
        proxy_set_header Host             $host;
        proxy_set_header X-Real-IP        $remote_addr;
        proxy_set_header X-Forwarded-For  $proxy_add_x_forwarded_for;
    }
}
```

**这三个 `proxy_set_header` 是必配项**：不配的话，后端拿到的 Host 是 `127.0.0.1:8080`（多域名路由直接失效），客户端 IP 全变成 Nginx 的 IP（限流、审计全瞎）。

### 1.6 location 匹配规则（请求来了走哪个块）

| 优先级 | 写法 | 含义 |
|---|---|---|
| 1（最高） | `location = /login` | 精确匹配，只认 `/login` |
| 2 | `location ^~ /api/` | 前缀匹配，命中后**不再看正则** |
| 3 | `location ~ \.php$` | 正则匹配（按书写顺序，第一个命中生效） |
| 4（最低） | `location /` | 普通前缀匹配，最长者胜 |

坑位：`location /api/` 与 `location /apiv2/` 同时存在时，访问 `/apiv2/x` 命中的是 `/apiv2/`（最长前缀），不是 `/api/`。

---

## 二、负载均衡怎么做

### 2.1 核心模型：upstream 后端池 + proxy_pass

```nginx
# 1. 定义后端池（写在 http 层，server 外面）
upstream app_servers {
    server 192.168.1.11:8080;
    server 192.168.1.12:8080;
    server 192.168.1.13:8080;
}

# 2. location 里指向池名（注意：不带 http:// 以外的任何路径尾巴）
server {
    listen 80;
    server_name app.example.com;

    location / {
        proxy_pass http://app_servers;
    }
}
```

请求进来后，Nginx 按策略从池里挑一台转发——**这就是负载均衡的全部骨架**。下面所有内容都是往这个骨架上加参数。

### 2.2 五种调度策略：怎么选

| 策略 | 配置写法 | 原理 | 适用场景 |
|---|---|---|---|
| 轮询（默认） | 不写 | 挨个发 | 后端机器同质、无状态服务（**首选**） |
| 加权轮询 | `server x weight=3;` | 按权重比例分 | 机器配置不均（8核多分、4核少分） |
| ip_hash | `ip_hash;` | 同一客户端 IP 永远打同一台 | 老应用 Session 存本地磁盘/内存，不能共享 |
| least_conn | `least_conn;` | 谁当前连接少给谁 | 请求耗时差异大（有的 10ms 有的 10s） |
| 一致性哈希 | `hash $arg_userId consistent;` | 同一参数值固定打同一台 | 各节点有本地缓存、要求同 key 命中同节点 |

配置示例（加权 + 健康剔除）：

```nginx
upstream app_servers {
    least_conn;
    server 192.168.1.11:8080 weight=3 max_fails=3 fail_timeout=15s;
    server 192.168.1.12:8080 weight=1 max_fails=3 fail_timeout=15s;
    server 192.168.1.13:8080 backup;   # 备机：只有前两台全挂才用
}
```

**健康检查两条路**：
- **被动检查（免费自带）**：`max_fails=3 fail_timeout=15s`——连续失败 3 次就把它踢出 15 秒。缺点是"失败一次"要真实用户来踩；
- **主动检查**：开源版没有，要么用阿里 Tengine 的 `check` 指令，要么上商业版/云 SLB。规模小的时候被动检查够用。

### 2.3 连接调优三件套（生产必配）

```nginx
upstream app_servers {
    server 192.168.1.11:8080;
    server 192.168.1.12:8080;
    keepalive 64;          # 与后端保持 64 个长连接，省去每次 TCP 握手
}

server {
    location / {
        proxy_pass http://app_servers;
        proxy_http_version 1.1;              # keepalive 必须 1.1
        proxy_set_header Connection "";      # 清掉客户端的 Connection 头，启用池的长连接
    }
}
```

### 2.4 故障演练：眼见为实的负载均衡

```bash
# 1. 起两个"后端"（用 nginx 网页当假后端，区分内容即可）
docker run -d --name srv1 -p 8081:80 nginx:1.27
docker run -d --name srv2 -p 8082:80 nginx:1.27
docker exec srv1 sh -c 'echo "I am server-1" > /usr/share/nginx/html/index.html'
docker exec srv2 sh -c 'echo "I am server-2" > /usr/share/nginx/html/index.html'

# 2. 起负载均衡器，conf.d/default.conf 写：
#    upstream pool { server host.docker.internal:8081; server host.docker.internal:8082; }
#    server { listen 80; location / { proxy_pass http://pool; } }
docker run -d --name lb -p 8090:80 nginx:1.27

# 3. 打 6 次，看分布（应大致 3:3）
for i in $(seq 6); do curl -s http://localhost:8090/; echo; done

# 4. 故障演练：干掉一台
docker stop srv2
# 再 curl 全部输出 I am server-1 —— 被动健康检查生效
```

---

## 三、域名和 IP 怎么配（手把手）

### 3.1 先理解一张图：一次访问到底发生了什么

```text
浏览器输入 www.example.com
   ① 查 hosts 文件（本地开发常客）→ 没有再查 DNS（域名服务商）→ 得到 IP：1.2.3.4
   ② 浏览器向 1.2.3.4 的 80/443 端口发起 TCP 连接
   ③ HTTP 请求头里带着 Host: www.example.com
   ④ Nginx 收到连接，拿 Host 头去匹配各个 server 的 server_name
   ⑤ 匹配中 → 进该 server 的 location 处理；全没中 → 落到 default_server
```

**一句话总结：DNS 把域名翻译成 IP（找到机器），server_name 把请求分给正确的 server（找到网站）。两件事配对了，访问才通。**

### 3.2 本地开发：hosts 文件（不用买域名就能练）

域名解析先查本机 hosts，命中就不问 DNS——利用这一点，本地开发随便编一个域名：

| 系统 | hosts 文件位置 | 写法 |
|---|---|---|
| Windows | `C:\Windows\System32\drivers\etc\hosts`（管理员权限改） | `127.0.0.1  www.example.com` |
| Linux/Mac | `/etc/hosts`（sudo 改） | 同上 |

```text
# hosts 文件示例：一行一个"域名 → IP"的假解析
127.0.0.1   www.example.com
127.0.0.1   api.example.com
192.168.1.11  app.internal        # 也能指到局域网其他机器
```

改完刷新缓存：
- Windows：`ipconfig /flushdns`
- Linux：一般立即生效；不生效就 `nscd -i hosts` 或重启

验证三连：

```bash
ping www.example.com        # 看解析出的 IP 是不是你写的
curl -H "Host: www.example.com" http://127.0.0.1   # 绕过 DNS 直接指定 Host 测 Nginx
curl http://www.example.com                        # 完整链路
```

> **排障技巧**：`curl -H "Host: xxx"` 是神器——域名解析和 Nginx 配置哪个坏了，用它一测便知：能通说明 Nginx 没问题、是 DNS/hosts 的锅。

### 3.3 server_name：一个 IP 怎么挂多个域名

一台服务器只有一个公网 IP，但可以同时服务 N 个域名——靠 `server_name` 区分：

```nginx
# server 块 1：官网
server {
    listen 80;
    server_name www.example.com example.com;   # 两个域名写一个 server
    location / { root /var/www/www; }
}

# server 块 2：API
server {
    listen 80;
    server_name api.example.com;
    location / { proxy_pass http://127.0.0.1:8080; }
}

# server 块 3：兜底（匹配不中的全进这里）
server {
    listen 80 default_server;
    server_name _;
    return 444;    # 直接断开，扫域名的爬虫全吃闭门羹
}
```

`server_name` 的匹配顺序：**精确名 > `*.example.com` 通配 > `www.example.*` 尾通配 > 正则（按书写顺序）> default_server**。

### 3.4 生产上线：域名 + IP 完整配置五步

**第 1 步：准备两个东西**
- **域名**：阿里云/腾讯云/Cloudflare 等注册商购买（年费几十元）；
- **公网 IP**：云服务器（阿里云 ECS / 腾讯云 CVM）自带，或家里宽带 + 内网穿透（学习用）。

**第 2 步：DNS 解析（在域名控制台配置）**

| 记录类型 | 填什么 | 作用 |
|---|---|---|
| **A 记录** | 主机记录 `www`，值 `1.2.3.4`（你的公网 IP） | `www.example.com` → `1.2.3.4` |
| **A 记录** | 主机记录 `@`，值 `1.2.3.4` | 裸域 `example.com` 也指向服务器 |
| **A 记录** | 主机记录 `api`，值 `1.2.3.4` | `api.example.com` 同机不同 server |
| CNAME | 主机记录 `cdn`，值 `xxx.cdn.com` | 域名指向另一个域名（CDN 常用） |

配置后等生效（TTL 一般 10 分钟），验证：`nslookup www.example.com` 或 `ping` 看到 IP 是你的服务器即成功。

**第 3 步：备案（国内服务器绕不开）**
- 服务器在中国大陆 → 80/443 端口对外提供网页服务**必须 ICP 备案**，否则云厂商直接拦截；
- 流程：云厂商备案入口提交资料 → 人脸核验 → 管局审核（约 1~3 周）；
- 服务器在香港/海外 → 不用备案，但国内访问慢、且随时有被阻断风险。**正经业务，老实备案**。

**第 4 步：放行端口（两层防火墙都要开）**

```bash
# 云控制台安全组：放行 TCP 80、443（最容易忘的一层！）
# 操作系统防火墙（第二层）：
# Ubuntu
ufw allow 80,443/tcp
# CentOS
firewall-cmd --permanent --add-service=http --add-service=https && firewall-cmd --reload
```

**第 5 步：Nginx 绑定域名 + 证书**

```nginx
# /etc/nginx/conf.d/www.example.com.conf
server {
    listen 80;
    server_name www.example.com example.com;
    return 301 https://$host$request_uri;     # http 强制跳 https
}

server {
    listen 443 ssl;
    server_name www.example.com example.com;

    ssl_certificate     /etc/nginx/certs/www.example.com.pem;
    ssl_certificate_key /etc/nginx/certs/www.example.com.key;

    location / {
        root /var/www/www;
        index index.html;
    }
}
```

证书从哪来：
- **Let's Encrypt 免费**：`certbot --nginx -d www.example.com` 一条命令自动签发 + 写配置 + 续期；
- 云厂商免费证书：控制台申请 DV 证书，下载 nginx 格式放到 `certs/` 目录。

### 3.5 上线检查清单（每一步都有命令可验）

```text
□ nslookup www.example.com        → 解析到服务器公网 IP
□ curl -I http://公网IP            → 通（安全组/防火墙 OK）
□ curl -H "Host: www.example.com" http://公网IP  → 通（Nginx server_name OK）
□ curl http://www.example.com      → 通（DNS + 全链路 OK）
□ curl -I https://www.example.com  → 200/301（证书 OK）
□ tail -f /var/log/nginx/access.log → 能看到自己刚才的请求
```

---

## 四、常见问题速查表

| 现象 | 一般原因 | 定位命令 |
|---|---|---|
| ping 域名不通 | DNS 没生效 / hosts 写错 | `nslookup 域名` |
| ping 通、curl 不通 | 端口没放行（安全组/防火墙） | `telnet IP 80` 或 `nc -zv IP 80` |
| 访问出的是默认欢迎页 | server_name 没匹配上，落进 default_server | `nginx -T \| grep server_name` |
| 502 Bad Gateway | 后端挂了 / proxy_pass 地址错 | `curl 后端地址:端口` 直接测 |
| 后端拿不到真实 IP | 三个 proxy_set_header 没配 | 后端打印 request headers |
| 改了配置不生效 | 没 reload / 语法错被拒 | `nginx -t` 再 `nginx -s reload` |
| 504 Gateway Timeout | 后端处理太慢，默认 60s 超时 | 调大 `proxy_read_timeout` |

---

## 五、动手实验（20 分钟，把两大块串起来）

1. hosts 文件加 `127.0.0.1 demo.local`，Windows 记得 `ipconfig /flushdns`；
2. 写一个 Nginx 配置：`server_name demo.local`，两个 location——`/` 托管静态页、`/api/` 反代到本机随便一个端口的服务；
3. 用 [2.4 节](#24-故障演练眼见为实的负载均衡) 的双后端方法，把 `/api/` 反代改成 upstream 池，`for i in $(seq 6); do curl ...` 看流量分布；
4. `docker stop` 其中一台，再 curl 观察自动切换；
5. 用 `curl -H "Host: demo.local"` 和 `curl http://demo.local/` 各测一遍，确认理解"DNS/hosts 负责找机器、server_name 负责找网站"。

---

## 六、面试要点总结

```text
关键词：
nginx -t / -s reload（热加载不掉连接） · location 优先级（= > ^~ > 正则 > 前缀）
upstream 五策略：轮询/weight/ip_hash/least_conn/hash consistent
被动健康检查 max_fails/fail_timeout · keepalive 长连接三件套
DNS：hosts 优先 → A 记录域名→IP · server_name 按 Host 头选 server
一 IP 多域名 = 多 server 块 · default_server 兜底 · 上线五步：域名→解析→备案→端口→绑定
必配三头：Host / X-Real-IP / X-Forwarded-For
```

## 学习检查清单

- [ ] 能默写"静态托管 + 反代 + upstream 负载均衡"三合一的最小配置
- [ ] 能说出五种负载均衡策略各自的适用场景
- [ ] 能解释 DNS 和 server_name 分别解决什么问题
- [ ] 能在本地用 hosts 文件配出一个假域名并访问通
- [ ] 能列出生产上线五步，并说出每一步的验证命令

## 下一步

- [13-Nginx与OpenResty反向代理](13-Nginx与OpenResty反向代理.md)：回到概念层——epoll 为什么快、OpenResty 把配置变成代码；
- [02-Spring微服务/04-SpringCloudGateway网关](../02-Spring微服务/04-SpringCloudGateway网关.md)：Nginx 后面的 Java 网关层，路由/限流/鉴权在那做；
- [07-运维部署/03-JenkinsCICD流水线](../07-运维部署/03-JenkinsCICD流水线.md)：配置怎么随发布流程自动化上线。
