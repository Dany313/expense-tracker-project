# Multi-stage Dockerfile for receipt-ocr-service
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY services/api-gateway/pom.xml services/api-gateway/
COPY services/expense-service/pom.xml services/expense-service/
COPY services/receipt-ocr-service/pom.xml services/receipt-ocr-service/
COPY services/expense-agent-service/pom.xml services/expense-agent-service/

COPY services/receipt-ocr-service/src services/receipt-ocr-service/src

RUN ./mvnw clean package -DskipTests -pl services/receipt-ocr-service -am

FROM eclipse-temurin:25-jre-noble
WORKDIR /app
COPY --from=builder /workspace/services/receipt-ocr-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
