#!/bin/bash
# ============================================================
# AI客服平台 - 单机一键部署脚本
# 部署: 全部基础设施 + 全部微服务
# 用法: bash deploy.sh
# ============================================================

set -e

# 加载配置
if [ -f "$(dirname "$0")/deploy-vars.sh" ]; then
    source "$(dirname "$0")/deploy-vars.sh"
fi

VERSION="${VERSION:-latest}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MINIO_USER="${MINIO_USER:-minioadmin}"
MINIO_PASSWORD="${MINIO_PASSWORD:-minioadmin}"
OPENAI_API_KEY="${OPENAI_API_KEY:-demo-key}"
OPENAI_BASE_URL="${OPENAI_BASE_URL:-https://api.minimaxi.com}"
OPENAI_MODEL="${OPENAI_MODEL:-MiniMax-M3}"

echo "=========================================="
echo " AI客服平台 - 单机部署"
echo " 版本: $VERSION"
echo "=========================================="

# ---------- 创建目录 ----------
mkdir -p /opt/aics/{mysql,redis,config,logs,images}
cd /opt/aics

# ---------- 加载镜像 ----------
echo ""
echo "[1/4] 加载业务镜像..."
for svc in ai-cs-gateway ai-cs-user ai-cs-knowledge ai-cs-chat ai-cs-search ai-cs-message ai-cs-notify; do
    tar_file="/opt/aics/images/${svc}-${VERSION}.tar"
    if [ -f "$tar_file" ]; then
        echo "  加载 ${svc}:${VERSION} ..."
        docker load -i "$tar_file"
    else
        echo "  WARN: 镜像文件不存在: $tar_file (跳过)"
    fi
done

# ---------- 启动基础设施 ----------
echo ""
echo "[2/4] 启动基础设施..."
export VERSION MYSQL_ROOT_PASSWORD MINIO_USER MINIO_PASSWORD OPENAI_API_KEY OPENAI_BASE_URL OPENAI_MODEL

docker compose -f /opt/aics/docker-compose-all.yml up -d mysql redis nacos elasticsearch rocketmq-namesrv rocketmq-broker minio

# 等待 MySQL 就绪
echo "  等待 MySQL 就绪..."
while ! docker exec aics-mysql mysqladmin ping -h localhost -uroot -p"$MYSQL_ROOT_PASSWORD" --silent 2>/dev/null; do
    sleep 2
done
echo "  MySQL 就绪 ✓"

# 等待 Nacos 就绪
echo "  等待 Nacos 就绪..."
while ! curl -s http://localhost:8848/nacos/v1/console/health/readiness 2>/dev/null | grep -q ok; do
    sleep 2
done
echo "  Nacos 就绪 ✓"

# 等待 Elasticsearch 就绪
echo "  等待 Elasticsearch 就绪..."
while ! curl -s http://localhost:9200/_cluster/health 2>/dev/null | grep -q '"status"'; do
    sleep 3
done
echo "  Elasticsearch 就绪 ✓"

# ---------- 启动业务服务 ----------
echo ""
echo "[3/4] 启动微服务..."
docker compose -f /opt/aics/docker-compose-all.yml up -d \
    api-gateway user-service knowledge-service ai-chat-service \
    search-service message-service notify-service

# ---------- 初始化 MinIO ----------
echo ""
echo "[4/4] 初始化 MinIO..."
sleep 5
docker exec aics-minio sh -c "
  mc alias set local http://localhost:9000 $MINIO_USER $MINIO_PASSWORD 2>/dev/null || true
  mc mb local/aics-knowledge --ignore-existing 2>/dev/null || true
" 2>/dev/null || echo "  MinIO 初始化跳过"

echo ""
echo "=========================================="
echo " 单机部署完成！"
echo ""
echo " 服务端口:"
echo "  - API 网关:      8080"
echo "  - 用户服务:      8081"
echo "  - 知识库服务:    8082"
echo "  - AI 对话服务:   8083"
echo "  - 搜索服务:      8084"
echo "  - 消息服务:      8085"
echo "  - 通知服务:      8086"
echo "  - Nacos 控制台:  8848"
echo "  - MinIO 控制台:  9001"
echo ""
docker compose -f /opt/aics/docker-compose-all.yml ps
echo "=========================================="
