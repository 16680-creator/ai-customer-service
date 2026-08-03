#!/bin/bash
# ============================================================
# AI客服平台 - 主机初始化脚本
# 用途：在每台主机上执行一次，安装 Docker、JDK、配置环境
# 用法：ssh root@<host-ip> 'bash -s' < init-host.sh
# ============================================================

set -e

echo "=========================================="
echo " AI客服平台 - 主机环境初始化"
echo " 主机: $(hostname) ($(hostname -I | awk '{print $1}'))"
echo " 时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

# ---------- 1. 系统更新 ----------
echo "[1/8] 更新系统包..."
if command -v apt-get &> /dev/null; then
    apt-get update -y && apt-get upgrade -y
elif command -v yum &> /dev/null; then
    yum update -y
fi

# ---------- 2. 安装必要工具 ----------
echo "[2/8] 安装基础工具..."
if command -v apt-get &> /dev/null; then
    apt-get install -y curl wget vim net-tools htop unzip
elif command -v yum &> /dev/null; then
    yum install -y curl wget vim net-tools htop unzip
fi

# ---------- 3. 安装 Docker ----------
echo "[3/8] 安装 Docker..."
if ! command -v docker &> /dev/null; then
    curl -fsSL https://get.docker.com | bash
    systemctl enable docker
    systemctl start docker
fi
docker --version

# ---------- 4. 安装 Docker Compose ----------
echo "[4/8] 安装 Docker Compose..."
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi
docker compose version || docker-compose --version

# ---------- 5. 配置 Docker 镜像加速 ----------
echo "[5/8] 配置 Docker 镜像加速..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'DOCKERCONF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.xuanyuan.me"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  }
}
DOCKERCONF
systemctl restart docker

# ---------- 6. 配置系统参数 ----------
echo "[6/8] 配置系统参数..."
# Elasticsearch 需要 vm.max_map_count 至少 262144
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" >> /etc/sysctl.conf

# 文件描述符限制
echo "* soft nofile 65536" >> /etc/security/limits.conf
echo "* hard nofile 65536" >> /etc/security/limits.conf

# ---------- 7. 关闭防火墙（生产环境请配置规则而非关闭） ----------
echo "[7/8] 配置防火墙..."
if command -v ufw &> /dev/null; then
    ufw disable || true
elif command -v firewall-cmd &> /dev/null; then
    systemctl stop firewalld || true
    systemctl disable firewalld || true
fi

# ---------- 8. 创建部署目录 ----------
echo "[8/8] 创建部署目录..."
mkdir -p /opt/aics/{mysql,redis,logs,config}

echo ""
echo "=========================================="
echo " 初始化完成！"
echo " Docker: $(docker --version)"
echo " 主机IP: $(hostname -I | awk '{print $1}')"
echo "=========================================="