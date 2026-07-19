FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
ARG JAR_FILE=target/azurion-backend-0.1.0-SNAPSHOT.jar
RUN addgroup -S azurion \
    && adduser -S azurion -G azurion \
    && mkdir -p /app/data/private-files /app/data/public-files \
    && chown -R azurion:azurion /app
COPY --chown=azurion:azurion ${JAR_FILE} app.jar
ENV AZURION_PRIVATE_FILES_DIR=/app/data/private-files \
    AZURION_PUBLIC_FILES_DIR=/app/data/public-files \
    MULTIPART_MAX_FILE_SIZE=8MB \
    MULTIPART_MAX_REQUEST_SIZE=10MB
VOLUME ["/app/data"]
USER azurion
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
