# =============================================================================
# Machine-specific environment overrides for start-all.ps1 / stop-all.ps1
#
# On a NEW machine: copy this file, then edit the three path variables below.
# Paths can also be provided via env vars JAVA8_HOME / JAVA17_HOME / MAVEN_HOME,
# or auto-detected from common install dirs, so usually only paths are needed.
# =============================================================================

# JDK 8 (required by Nacos)
$JAVA8_HOME = "D:\DevTools\jdk\jdk8u492"

# JDK 17 (required by RocketMQ and all Spring Boot services)
$JAVA17_HOME = "D:\DevTools\jdk\jdk17.0.19"

# Apache Maven (required to build / spring-boot:run thin-jar services)
$MAVEN_HOME = "D:\DevTools\maven\maven3.9.16"

# Remote MySQL password (fills the ${DB_PASSWORD} placeholders in ai-cs-*.yml)
$DB_PASSWORD = "Yxw172707"

# LLM API keys：不要写在本文件（本文件会提交到仓库）。
# 请复制 tools/env.local.ps1 模板填入真实密钥，该文件已被 .gitignore 排除，
# 由 common.ps1 启动时自动加载；也可直接配置机器级环境变量 DEEPSEEK_API_KEY / SILICONFLOW_API_KEY。