#!/bin/bash
# ============================================================
# AI客服平台 - Host1 部署脚本
# 部署: MySQL Master, Redis Node1, Nacos, Seata, MinIO, Gateway, User Service
# 用法: 在目标主机上执行 bash deploy-host1.sh
# ============================================================

set -e

HOST1_IP="${HOST1_IP:-127.0.0.1}"
VERSION="${VERSION:-latest}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MINIO_USER="${MINIO_USER:-minioadmin}"
MINIO_PASSWORD="${MINIO_PASSWORD:-minioadmin}"

echo "=========================================="
echo " AI客服平台 - Host1 部署"
echo " 本机IP: $HOST1_IP"
echo " 版本: $VERSION"
echo "=========================================="

# ---------- 创建目录 ----------
mkdir -p /opt/aics/{mysql,redis,config,logs}

# ---------- 复制配置文件 ----------
# 假设这些文件已经在 /opt/aics/ 目录下
cd /opt/aics

# ---------- 加载镜像 ----------
echo ""
echo "[1/3] 加载业务镜像..."

# 从 tar 文件加载镜像（由 Jenkins 构建并分发）
for svc in ai-cs-gateway ai-cs-user; do
    tar_file="/opt/aics/images/${svc}-${VERSION}.tar"
    if [ -f "$tar_file" ]; then
        echo "  加载 ${svc}:${VERSION} ..."
        docker load -i "$tar_file"
    else
        echo "  WARN: 镜像文件不存在: $tar_file"
    fi
done

# ---------- 启动服务 ----------
echo ""
echo "[2/3] 启动 Host1 服务..."
export HOST1_IP VERSION MYSQL_ROOT_PASSWORD MINIO_USER MINIO_PASSWORD

# 先启动基础设施
docker compose -f /opt/aics/docker-compose-host1.yml up -d mysql-master redis-node1 nacos minio seata-server

# 等待基础设施就绪
echo "  等待 MySQL 就绪..."
while ! docker exec aics-mysql-master mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null; do
    sleep 2
done
echo "  MySQL 就绪"

echo "  等待 Nacos 就绪..."
while ! curl -s http://localhost:8848/nacos/v1/console/health/readiness 2>/dev/null | grep -q ok; do
    sleep 2
done
echo "  Nacos 就绪"

# 启动业务服务
docker compose -f /opt/aics/docker-compose-host1.yml up -d api-gateway user-service

# ---------- 初始化 MinIO ----------
echo ""
echo "[3/3] 初始化 MinIO..."
sleep 5
# 等待 MinIO 客户端就绪，创建 bucket
docker exec aics-minio sh -c "
  mc alias set local http://localhost:9000 $MINIO_USER $MINIO_PASSWORD 2>/dev/null || true
  mc mb local/aics-knowledge --ignore-existing 2>/dev/null || true
" 2>/dev/null || echo "  MinIO 初始化跳过（客户端未安装）"

# ---------- 配置 MySQL 主库 ----------
echo ""
echo "  配置 MySQL 主库复制用户..."
docker exec aics-mysql-master mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  CREATE USER IF NOT EXISTS 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl_password';
  GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
  FLUSH PRIVILEGES;
" 2>/dev/null || echo "  复制用户已存在"

echo ""
echo "=========================================="
echo " Host1 部署完成！"
echo " 服务列表:"
docker compose -f /opt/aics/docker-compose-host1.yml ps
echo "=========================================="