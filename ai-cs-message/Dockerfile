# 多阶段构建 - 消息服务
# 阶段1: Maven构建
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
# 先复制父POM和公共模块，利用Docker缓存加速构建
COPY pom.xml .
COPY ai-cs-common/pom.xml ai-cs-common/
COPY ai-cs-message/pom.xml ai-cs-message/
# 下载依赖
RUN mvn dependency:go-offline -B -pl ai-cs-message -am
# 复制源码并构建
COPY ai-cs-common/ ai-cs-common/
COPY ai-cs-message/ ai-cs-message/
RUN mvn package -pl ai-cs-message -am -DskipTests -B

# 阶段2: JRE运行
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=builder /build/ai-cs-message/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar app.jar"]
