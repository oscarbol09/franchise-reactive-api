# syntax=docker/dockerfile:1.7

# ==============================================================================
# Stage 1: Build & Layer Extraction
# ==============================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

RUN java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination extracted

# ==============================================================================
# Stage 2: Minimal Non-Root JRE Runtime with Class Data Sharing (CDS)
# ==============================================================================
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /application

RUN groupadd -r spring && useradd -r -g spring -s /sbin/nologin spring

COPY --from=builder --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/application/ ./

USER spring:spring

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
