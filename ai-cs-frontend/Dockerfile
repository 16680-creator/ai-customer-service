# syntax=docker/dockerfile:1
#
# ai-cs-frontend 生产镜像：Node 构建 -> Nginx 静态托管
#
# 为什么必须多阶段：构建阶段需要 node + node_modules，运行阶段一个都不需要。
# 单阶段镜像会把 node_modules（数百 MB）和源码一起塞进产物，既是体积浪费也是源码泄露。
#
# ⚠️ 本机没有安装 Docker，以下命令未实跑验证；文档里的"已验证"字样不适用于本文件。
#    在有 Docker 的机器上应执行：
#      docker build -t aics/frontend:local .
#      docker run --rm -p 8081:80 aics/frontend:local
#      curl -I http://localhost:8081/order/1        # 期望 200 而不是 404（history 深链接）
#      curl -s http://localhost:8081/api/user/ping  # 期望被 nginx 反代到网关

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /app

# 先只拷贝依赖清单：只要 lockfile 没变，这一层就命中缓存，改业务代码时不用重装依赖
COPY package.json package-lock.json ./
# npm ci 严格按 lockfile 安装（比 npm install 快且可复现）
RUN npm ci

COPY . .

# .env.production 里 VITE_GATEWAY 留空 => 产物里所有请求都是同源相对路径 /api，
# 由下面的 nginx.conf 反代到网关。这样镜像本身与环境无关，dev/prod 用同一个包。
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine

# 覆盖默认站点配置（默认配置只有 root 静态托管，没有 try_files，深链接刷新必 404）
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

# 基础健康检查：容器自身活着 != 能服务请求，这里真发一个 HTTP 请求
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://127.0.0.1/index.html || exit 1

CMD ["nginx", "-g", "daemon off;"]
