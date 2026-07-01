# ============================================================
# AI客服平台 - 部署变量配置文件
# 复制此文件为 deploy-vars.sh 并修改为你的实际值
# ============================================================

# ---------- 三台主机信息 ----------
# 主机1 - 部署 MySQL Master, Redis Node1, Nacos, MinIO, Seata, Gateway, User Service
export HOST1_IP="192.168.1.101"
export HOST1_USER="root"
export HOST1_PASSWORD="your_password_1"

# 主机2 - 部署 MySQL Slave1, Redis Node2, Knowledge Service, Chat Service, Search Service
export HOST2_IP="192.168.1.102"
export HOST2_USER="root"
export HOST2_PASSWORD="your_password_2"

# 主机3 - 部署 MySQL Slave2, Redis Node3, Elasticsearch, RocketMQ, Message Service, Notify Service
export HOST3_IP="192.168.1.103"
export HOST3_USER="root"
export HOST3_PASSWORD="your_password_3"

# ---------- 通用配置 ----------
export MYSQL_ROOT_PASSWORD="root"
export MINIO_USER="minioadmin"
export MINIO_PASSWORD="minioadmin"

# ---------- 镜像仓库配置 ----------
# 如果使用私有镜像仓库，填写以下信息
export REGISTRY="aics"                    # 镜像仓库地址，如 harbor.example.com/aics
export REGISTRY_USER=""                   # 仓库用户名（可选）
export REGISTRY_PASSWORD=""               # 仓库密码（可选）
export VERSION="latest"                   # 镜像版本号

# ---------- AI 模型配置 ----------
export OPENAI_API_KEY="demo-key"          # OpenAI API Key（如果用 Ollama 则填 demo-key）
export OPENAI_BASE_URL="http://host.docker.internal:11434/v1"  # Ollama 地址
export OPENAI_MODEL="qwen2.5:7b"          # 模型名称