# 多阶段构建（对标 Spring Boot 容器化）
# 学习章节：22-Python速成/20-部署与生产运行.md

# ============== 构建阶段 ==============
FROM python:3.12-slim as builder

WORKDIR /app

# 安装依赖
COPY requirements.txt .
RUN pip install --user --no-cache-dir -r requirements.txt

# ============== 运行阶段 ==============
FROM python:3.12-slim

WORKDIR /app

# 从构建阶段复制依赖
COPY --from=builder /root/.local /root/.local

# 设置环境变量
ENV PATH=/root/.local/bin:$PATH
ENV PYTHONUNBUFFERED=1

# 复制应用代码
COPY app/ ./app/
COPY .env.example .env

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8082/api/recommend/health')" || exit 1

# 启动应用
# 对标 Spring Boot:
#   java -jar app.jar --server.port=8082
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8082"]
