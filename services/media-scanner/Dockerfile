# syntax=docker/dockerfile:1.7
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY services/media-scanner/.mvn services/media-scanner/.mvn
COPY services/media-scanner/mvnw services/media-scanner/pom.xml services/media-scanner/
COPY services/media-scanner/src services/media-scanner/src
WORKDIR /workspace/services/media-scanner
RUN --mount=type=cache,target=/root/.m2 \
    chmod +x mvnw \
    && ./mvnw package -DskipTests -Dmaven.wagon.http.retryHandler.count=5

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends clamav-daemon clamav-freshclam netcat-openbsd \
    && rm -rf /var/lib/apt/lists/* \
    && sed -i 's/^LocalSocket /#LocalSocket /' /etc/clamav/clamd.conf \
    && printf '\nTCPSocket 3310\nTCPAddr 127.0.0.1\n' >> /etc/clamav/clamd.conf \
    && useradd --system --uid 10001 --create-home wambe

WORKDIR /app
COPY --from=build /workspace/services/media-scanner/target/media-scanner-*.jar /app/media-scanner.jar
COPY services/media-scanner/docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 8081
ENTRYPOINT ["/app/docker-entrypoint.sh"]
