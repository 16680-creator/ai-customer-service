#!/bin/bash
# ============================================================
# AI客服平台 - 部署前检查脚本
# 用途：部署前检查所有主机连通性和环境
# 用法：bash check-deploy.sh
# ============================================================

set -e

# 加载配置
source ./deploy-vars.sh 2>/dev/null || {
    echo "ERROR: 请先创建 deploy-vars.sh 配置文件"
    echo "复制 deploy-vars.example.sh 为 deploy-vars.sh 并填写主机信息"
    exit 1
}

echo "=========================================="
echo " AI客服平台 - 部署前检查"
echo "=========================================="

HOSTS=("$HOST1_IP" "$HOST2_IP" "$HOST3_IP")
HOST_NAMES=("Host1" "Host2" "Host3")

# 检查 SSH 连通性
echo ""
echo "[1] 检查 SSH 连通性..."
for i in "${!HOSTS[@]}"; do
    echo -n "  ${HOST_NAMES[$i]} (${HOSTS[$i]}) ... "
    if ssh -o ConnectTimeout=5 -o StrictHostKeyChecking=no root@"${HOSTS[$i]}" "echo OK" &>/dev/null; then
        echo "OK"
    else
        echo "FAILED"
        echo "  ERROR: 无法连接到 ${HOSTS[$i]}，请检查 IP 和 SSH 配置"
        exit 1
    fi
done

# 检查 Docker
echo ""
echo "[2] 检查 Docker 环境..."
for i in "${!HOSTS[@]}"; do
    echo -n "  ${HOST_NAMES[$i]} (${HOSTS[$i]}) ... "
    DOCKER_VER=$(ssh -o ConnectTimeout=5 root@"${HOSTS[$i]}" "docker --version 2>/dev/null" || echo "NOT_INSTALLED")
    if [ "$DOCKER_VER" = "NOT_INSTALLED" ]; then
        echo "NOT INSTALLED - 请先运行 init-host.sh"
    else
        echo "$DOCKER_VER"
    fi
done

# 检查磁盘空间
echo ""
echo "[3] 检查磁盘空间..."
for i in "${!HOSTS[@]}"; do
    echo -n "  ${HOST_NAMES[$i]} (${HOSTS[$i]}) ... "
    ssh -o ConnectTimeout=5 root@"${HOSTS[$i]}" "df -h / | tail -1 | awk '{print \"可用: \"\$4\" / 总量: \"\$2}'"
done

# 检查内存
echo ""
echo "[4] 检查内存..."
for i in "${!HOSTS[@]}"; do
    echo -n "  ${HOST_NAMES[$i]} (${HOSTS[$i]}) ... "
    ssh -o ConnectTimeout=5 root@"${HOSTS[$i]}" "free -h | grep Mem | awk '{print \"总量: \"\$2\", 可用: \"\$7}'"
done

# 检查端口占用
echo ""
echo "[5] 检查关键端口..."
HOST1_PORTS="3306 6379 8848 8080 8081 9000 9001"
HOST2_PORTS="3306 6379 8082 8083 8084"
HOST3_PORTS="3306 6379 9200 9876 10911 8085 8086"

for i in "${!HOSTS[@]}"; do
    echo "  ${HOST_NAMES[$i]} (${HOSTS[$i]}):"
    PORTS_VAR="HOST${i}_PORTS"
    # 根据主机选择端口列表
    case $i in
        0) PORTS="$HOST1_PORTS" ;;
        1) PORTS="$HOST2_PORTS" ;;
        2) PORTS="$HOST3_PORTS" ;;
    esac
    for port in $PORTS; do
        IN_USE=$(ssh -o ConnectTimeout=5 root@"${HOSTS[$i]}" "ss -tlnp | grep ':$port ' | wc -l" 2>/dev/null || echo "0")
        if [ "$IN_USE" -gt 0 ]; then
            echo "    [WARN] 端口 $port 已被占用"
        else
            echo "    [OK] 端口 $port 空闲"
        fi
    done
done

echo ""
echo "=========================================="
echo " 检查完成！"
echo "=========================================="