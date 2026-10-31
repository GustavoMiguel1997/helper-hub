FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN sh gradlew --no-daemon bootJar

FROM node:22-bookworm-slim AS node

FROM eclipse-temurin:25-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg python3-venv ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=node /usr/local/bin/node /usr/local/bin/node
COPY requirements-video.txt /opt/requirements-video.txt
RUN python3 -m venv /opt/yt-dlp \
    && /opt/yt-dlp/bin/pip install --no-cache-dir -r /opt/requirements-video.txt \
    && useradd --uid 10001 --create-home helperhub
ENV PORT=80
ENV PATH="/opt/yt-dlp/bin:${PATH}" JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -Djava.awt.headless=true"
WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/build/libs/*-SNAPSHOT.jar /app/helperhub.jar
USER 10001:10001
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl -fsS http://localhost:${PORT}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/helperhub.jar"]
