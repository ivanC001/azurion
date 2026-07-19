FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src/ src/

RUN ./mvnw clean package -DskipTests -B


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S azurion && adduser -S azurion -G azurion

RUN mkdir -p /app/data/public-files /app/data/private-files \
    && chown -R azurion:azurion /app/data

COPY --from=builder /app/target/*.jar app.jar

ENV AZURION_PRIVATE_FILES_DIR=/app/data/private-files \
    AZURION_PUBLIC_FILES_DIR=/app/data/public-files \
    MULTIPART_MAX_FILE_SIZE=8MB \
    MULTIPART_MAX_REQUEST_SIZE=10MB
VOLUME ["/app/data"]
USER azurion

EXPOSE 8080

ENTRYPOINT ["java","-XX:MaxRAMPercentage=70.0","-jar","/app/app.jar"]
