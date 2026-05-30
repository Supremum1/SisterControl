FROM eclipse-temurin:21-jdk-jammy AS backend-build
WORKDIR /workspace
COPY . .
RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip \
    && rm -rf /var/lib/apt/lists/*

COPY --from=backend-build /workspace/backend/build/libs/*.jar /app/sister-control-backend.jar
COPY server.py /app/server.py
COPY docker/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh \
    && pip3 install --no-cache-dir fastapi uvicorn python-multipart nudenet

ENV PORT=3000
ENV DETECTOR_PORT=8080
EXPOSE 3000 8080

ENTRYPOINT ["/app/entrypoint.sh"]
