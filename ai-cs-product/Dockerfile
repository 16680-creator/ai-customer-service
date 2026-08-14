FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="aics-team"

WORKDIR /app

COPY target/ai-cs-product-*.jar app.jar

EXPOSE 8088

ENTRYPOINT ["java", "-jar", "app.jar"]
