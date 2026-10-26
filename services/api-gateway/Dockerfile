# Multi-stage Dockerfile for api-gateway
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace

# Copy root maven wrapper and poms to leverage layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/expense-service/pom.xml services/expense-service/
COPY services/receipt-ocr-service/pom.xml services/receipt-ocr-service/
COPY services/expense-agent-service/pom.xml services/expense-agent-service/

# Copy sources for api-gateway
COPY services/api-gateway/src services/api-gateway/src

# Build the jar
RUN ./mvnw clean package -DskipTests -pl services/api-gateway -am

# Runtime stage
FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=builder /workspace/services/api-gateway/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
