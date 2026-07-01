#!/bin/bash
# ============================================================
# AI客服平台 - Host2 部署脚本
# 部署: MySQL Slave1, Redis Node2, Knowledge Service, Chat Service, Search Service
# 用法: 在目标主机上执行 bash deploy-host2.sh
# ============================================================

set -e

HOST1_IP="${HOST1_IP:-127.0.0.1}"
HOST2_IP="${HOST2_IP:-127.0.0.1}"
HOST3_IP="${HOST3_IP:-127.0.0.1}"
REGISTRY="${REGISTRY:-aics}"
VERSION="${VERSION:-latest}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
OPENAI_API_KEY="${OPENAI_API_KEY:-demo-key}"
OPENAI_BASE_URL="${OPENAI_BASE_URL:-http://host.docker.internal:11434/v1}"
OPENAI_MODEL="${OPENAI_MODEL:-qwen2.5:7b}"

echo "=========================================="
echo " AI客服平台 - Host2 部署"
echo " Host1 IP: $HOST1_IP"
echo " Host3 IP: $HOST3_IP"
echo " 版本: $VERSION"
echo "=========================================="

# ---------- 创建目录 ----------
mkdir -p /opt/aics/{mysql,redis,config,logs}

cd /opt/aics

# ---------- 拉取镜像 ----------
echo ""
echo "[1/3] 准备镜像..."
if [ -n "$REGISTRY_USER" ]; then
    echo "$REGISTRY_PASSWORD" | docker login "$REGISTRY" -u "$REGISTRY_USER" --password-stdin 2>/dev/null || true
fi

for svc in ai-cs-knowledge ai-cs-chat ai-cs-search; do
    echo "  拉取 $REGISTRY/$svc:$VERSION ..."
    docker pull "$REGISTRY/$svc:$VERSION" 2>/dev/null || {
        echo "  WARN: 无法拉取 $svc，尝试使用本地镜像..."
    }
done

# ---------- 启动服务 ----------
echo ""
echo "[2/3] 启动 Host2 服务..."
export HOST1_IP HOST2_IP HOST3_IP REGISTRY VERSION MYSQL_ROOT_PASSWORD
export OPENAI_API_KEY OPENAI_BASE_URL OPENAI_MODEL

# 先启动基础设施
docker compose -f /opt/aics/docker-compose-host2.yml up -d mysql-slave1 redis-node2

# 等待 MySQL 就绪
echo "  等待 MySQL 从库就绪..."
while ! docker exec aics-mysql-slave1 mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null; do
    sleep 2
done
echo "  MySQL 从库就绪"

# 配置主从复制
echo "  配置 MySQL 主从复制..."
docker exec aics-mysql-slave1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  STOP SLAVE;
  CHANGE MASTER TO
    MASTER_HOST='$HOST1_IP',
    MASTER_PORT=3306,
    MASTER_USER='repl',
    MASTER_PASSWORD='repl_password',
    MASTER_AUTO_POSITION=1;
  START SLAVE;
" 2>/dev/null

# 验证复制状态
SLAVE_STATUS=$(docker exec aics-mysql-slave1 mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW SLAVE STATUS\G" 2>/dev/null | grep -E "Slave_IO_Running|Slave_SQL_Running")
echo "  复制状态: $SLAVE_STATUS"

# 启动业务服务
docker compose -f /opt/aics/docker-compose-host2.yml up -d knowledge-service ai-chat-service search-service

# ---------- 验证 ----------
echo ""
echo "[3/3] 验证服务..."
sleep 10

echo "  知识库服务: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/actuator/health 2>/dev/null || echo 'N/A')"
echo "  AI对话服务: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/actuator/health 2>/dev/null || echo 'N/A')"
echo "  搜索服务: $(curl -s -o /dev/null -w '%{http_code}' http://localhost:8084/actuator/health 2>/dev/null || echo 'N/A')"

echo ""
echo "=========================================="
echo " Host2 部署完成！"
echo " 服务列表:"
docker compose -f /opt/aics/docker-compose-host2.yml ps
echo "=========================================="