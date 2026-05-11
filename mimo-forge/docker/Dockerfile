# ═══════════════════════════════════════════
#  MiMo Forge — 多阶段构建
# ═══════════════════════════════════════════
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S mimo && adduser -S mimo -G mimo
COPY --from=builder /app/target/*.jar app.jar
USER mimo
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/v1/health || exit 1
ENTRYPOINT ["java", "-XX:+UseZGC", "-Xmx2g", "-jar", "app.jar"]
