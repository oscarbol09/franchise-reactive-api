# syntax=docker/dockerfile:1.7

# ==============================================================================
# Stage 1: Build & Spring Boot Layer Extraction (BuildKit Cache Enabled)
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy@sha256:4b1a74fbffbe6b4e7b6f0171a472478282fb463790a51edf10e465d578f22b69 AS builder
WORKDIR /workspace

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw package -DskipTests -B && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ==============================================================================
# Stage 2: Hardened Non-Root JRE Runtime (Layered JAR & Dynamic CDS)
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269 AS runtime
WORKDIR /application

RUN apt-get update && \
    apt-get upgrade -y --no-install-recommends && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r -g 10001 spring && \
    useradd -r -u 10001 -g spring -s /sbin/nologin spring && \
    java -Xshare:dump && \
    chown -R 10001:10001 /application

COPY --from=builder --chown=10001:10001 /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=10001:10001 /workspace/extracted/application/ ./

USER 10001:10001

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Xshare:auto -XX:+AutoCreateSharedArchive -XX:SharedArchiveFile=/tmp/application.jsa"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=25s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 && printf 'GET /actuator/health/readiness HTTP/1.0\\r\\nHost: 127.0.0.1\\r\\n\\r\\n' >&3 && grep -q '200 OK' <&3"]

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
