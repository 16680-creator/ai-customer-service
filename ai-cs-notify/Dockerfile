# 多阶段构建 - 通知服务
# 阶段1: Maven构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先复制父POM和公共模块，利用Docker缓存加速构建
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-notify/pom.xml ai-cs-notify/
# 下载依赖
RUN mvn dependency:go-offline -B -pl ai-cs-notify -am
# 复制源码并构建
COPY ai-cs-common/ ai-cs-common/
COPY ai-cs-notify/ ai-cs-notify/
RUN mvn package -pl ai-cs-notify -am -DskipTests -B

# 阶段2: JRE运行
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/ai-cs-notify/target/*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
