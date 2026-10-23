# Build stage: compile and package with the same Java version used in production.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /build

# Cache Maven dependencies separately from application source changes.
COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY src src
RUN mvn -B -q -DskipTests package

# Runtime stage: small JRE image, non-root user, no build tools.
FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system collabnexus \
    && useradd --system --gid collabnexus --home-dir /app --shell /usr/sbin/nologin collabnexus

WORKDIR /app

COPY --from=build /build/target/collabnexus-*.jar /app/collabnexus.jar

RUN mkdir -p /app/data /app/uploads && chown -R collabnexus:collabnexus /app

USER collabnexus

ENV CN_PROFILE=prod \
    SERVER_PORT=8080 \
    CN_DATA_DIR=/app/data \
    CN_UPLOAD_DIR=/app/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/api/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/collabnexus.jar"]
