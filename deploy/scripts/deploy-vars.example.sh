# ============================================================
# AI客服平台 - 单机部署变量配置
# 复制此文件为 deploy-vars.sh 并修改为你的实际值
# ============================================================

# ---------- 基础配置 ----------
export MYSQL_ROOT_PASSWORD="root"
export MINIO_USER="minioadmin"
export MINIO_PASSWORD="minioadmin"

# ---------- 镜像版本 ----------
export VERSION="latest"

# ---------- AI 模型配置 ----------
export OPENAI_API_KEY="demo-key"
export OPENAI_BASE_URL="https://api.minimaxi.com"
export OPENAI_MODEL="MiniMax-M3"
