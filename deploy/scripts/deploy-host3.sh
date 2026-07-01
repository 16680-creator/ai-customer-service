#!/bin/bash
# ============================================================
# AI客服平台 - Host3 部署脚本
# 部署: MySQL Slave2, Redis Node3, Elasticsearch, RocketMQ, Message Service, Notify Service
# 用法: 在目标主机上执行 bash deploy-host3.sh
# ============================================================

set -e

HOST1_IP="${HOST1_IP:-127.0.0.1}"
HOST3_IP="${HOST3_IP:-127.0.0.1}"
VERSION="${VERSION:-latest}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"

echo "=========================================="
echo " AI客服平台 - Host3 部署"
echo " Host1 IP: $HOST1_IP"
echo " 本机IP: $HOST3_IP"
echo " 版本: $VERSION"
echo "=========================================="

# ---------- 创建目录 ----------
mkdir -p /opt/aics/{mysql,redis,config,logs}

cd /opt/aics

# ---------- 加载镜像 ----------
echo ""
echo "[1/3] 加载业务镜像..."

# 从 tar 文件加载镜像（由 Jenkins 构建并分发）
for svc in ai-cs-message ai-cs-notify; do
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
echo "[2/3] 启动 Host3 服务..."
export HOST1_IP HOST3_IP VERSION MYSQL_ROOT_PASSWORD

# 先启动基础设施
docker compose -f /opt/aics/docker-compose-host3.yml up -d mysql-slave2 redis-node3 elasticsearch rocketmq-namesrv

# 等待 MySQL 就绪
echo "  等待 MySQL 从库就绪..."
while ! docker exec aics-mysql-slave2 mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null; do
    sleep 2
done
echo "  MySQL 从库就绪"

# 配置主从复制
echo "  配置 MySQL 主从复制..."
docker exec aics-mysql-slave2 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  STOP SLAVE;
  CHANGE MASTER TO
    MASTER_HOST='$HOST1_IP',
    MASTER_PORT=3306,
    MASTER_USER='repl',
    MASTER_PASSWORD='repl_password',
    MASTER_AUTO_POSITION=1;
  START SLAVE;
" 2>/dev/null

# 等待 Elasticsearch 就绪
echo "  等待 Elasticsearch 就绪..."
while ! curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -q '"status":"green"'; do
    sleep 3
done
echo "  Elasticsearch 就绪"

# 启动 RocketMQ Broker
docker compose -f /opt/aics/docker-compose-host3.yml up -d rocketmq-broker

# 等待 RocketMQ Broker 就绪
echo "  等待 RocketMQ Broker 就绪..."
sleep 15

# 启动业务服务
docker compose -f /opt/aics/docker-compose-host3.yml up -d message-service notify-service

# ---------- 验证 ----------
echo ""
echo "[3/3] 验证服务..."
sleep 10

echo "  Elasticsearch: $(curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -o '"status":"[^"]*"')"
echo "  消息服务: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8085/actuator/health 2>/dev/null || echo 'N/A')"
echo "  通知服务: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8086/actuator/health 2>/dev/null || echo 'N/A')"

echo ""
echo "=========================================="
echo " Host3 部署完成！"
echo " 服务列表:"
docker compose -f /opt/aics/docker-compose-host3.yml ps
echo "=========================================="