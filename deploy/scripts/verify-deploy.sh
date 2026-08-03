#!/bin/bash
# ============================================================
# AI客服平台 - 部署后验证脚本
# 用途：部署完成后验证所有服务是否正常运行
# 用法：bash verify-deploy.sh
# ============================================================

set -e

source ./deploy-vars.sh 2>/dev/null || {
    echo "ERROR: 请先创建 deploy-vars.sh 配置文件"
    exit 1
}

HOST1_IP="${HOST1_IP}"
HOST2_IP="${HOST2_IP}"
HOST3_IP="${HOST3_IP}"

PASS=0
FAIL=0

check() {
    local name="$1"
    local cmd="$2"
    echo -n "  [$name] ... "
    if eval "$cmd" &>/dev/null; then
        echo "PASS"
        ((PASS++))
    else
        echo "FAIL"
        ((FAIL++))
    fi
}

echo "=========================================="
echo " AI客服平台 - 部署验证"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# ---------- Host1 验证 ----------
echo ""
echo "--- Host1 ($HOST1_IP) ---"

echo "  基础设施:"
check "MySQL Master"    "ssh root@$HOST1_IP 'docker exec aics-mysql-master mysqladmin ping -h localhost -uroot -proot --silent'"
check "Redis Node1"     "ssh root@$HOST1_IP 'docker exec aics-redis-node1 redis-cli ping | grep -q PONG'"
check "Nacos"           "curl -s http://$HOST1_IP:8848/nacos/v1/console/health/readiness | grep -q ok"
check "MinIO"           "curl -s http://$HOST1_IP:9000/minio/health/live"
check "Seata"           "curl -s http://$HOST1_IP:7091/health"

echo "  微服务:"
check "API Gateway"     "curl -s -o /dev/null -w '%{http_code}' http://$HOST1_IP:8080/actuator/health | grep -q 200"
check "User Service"    "curl -s -o /dev/null -w '%{http_code}' http://$HOST1_IP:8081/actuator/health | grep -q 200"

# ---------- Host2 验证 ----------
echo ""
echo "--- Host2 ($HOST2_IP) ---"

echo "  基础设施:"
check "MySQL Slave1"    "ssh root@$HOST2_IP 'docker exec aics-mysql-slave1 mysqladmin ping -h localhost -uroot -proot --silent'"
check "Redis Node2"     "ssh root@$HOST2_IP 'docker exec aics-redis-node2 redis-cli ping | grep -q PONG'"

echo "  微服务:"
check "Knowledge Svc"   "curl -s -o /dev/null -w '%{http_code}' http://$HOST2_IP:8082/actuator/health | grep -q 200"
check "AI Chat Svc"     "curl -s -o /dev/null -w '%{http_code}' http://$HOST2_IP:8083/actuator/health | grep -q 200"
check "Search Svc"      "curl -s -o /dev/null -w '%{http_code}' http://$HOST2_IP:8084/actuator/health | grep -q 200"

# ---------- Host3 验证 ----------
echo ""
echo "--- Host3 ($HOST3_IP) ---"

echo "  基础设施:"
check "MySQL Slave2"    "ssh root@$HOST3_IP 'docker exec aics-mysql-slave2 mysqladmin ping -h localhost -uroot -proot --silent'"
check "Redis Node3"     "ssh root@$HOST3_IP 'docker exec aics-redis-node3 redis-cli ping | grep -q PONG'"
check "Elasticsearch"   "curl -s http://$HOST3_IP:9200/_cluster/health | grep -q green"
check "RocketMQ NS"     "ssh root@$HOST3_IP 'docker exec aics-rocketmq-namesrv curl -s http://localhost:9876/ > /dev/null'"

echo "  微服务:"
check "Message Svc"     "curl -s -o /dev/null -w '%{http_code}' http://$HOST3_IP:8085/actuator/health | grep -q 200"
check "Notify Svc"      "curl -s -o /dev/null -w '%{http_code}' http://$HOST3_IP:8086/actuator/health | grep -q 200"

# ---------- 跨主机验证 ----------
echo ""
echo "--- 跨主机验证 ---"

# MySQL 主从复制
check "MySQL 主从复制"  "ssh root@$HOST2_IP \"docker exec aics-mysql-slave1 mysql -uroot -proot -e 'SHOW SLAVE STATUS\\G' 2>/dev/null | grep -E 'Slave_IO_Running.*Yes'\""

# Nacos 服务注册
check "Nacos 服务注册"  "curl -s 'http://$HOST1_IP:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20&namespaceId=aics' | grep -q 'ai-cs-gateway'"

# API 网关路由
check "API 路由"        "curl -s -o /dev/null -w '%{http_code}' http://$HOST1_IP:8080/api/user/health | grep -q 200"

# ---------- 汇总 ----------
echo ""
echo "=========================================="
echo " 验证结果: PASS=$PASS  FAIL=$FAIL"
if [ "$FAIL" -eq 0 ]; then
    echo " 状态: 全部通过！"
    echo " 访问地址: http://$HOST1_IP:8080"
    echo " Nacos 控制台: http://$HOST1_IP:8848/nacos"
    echo " MinIO 控制台: http://$HOST1_IP:9001"
else
    echo " 状态: 有 $FAIL 项检查失败，请排查"
fi
echo "=========================================="