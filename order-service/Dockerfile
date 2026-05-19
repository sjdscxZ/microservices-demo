FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
RUN mvn -B -pl order-service -am dependency:go-offline
COPY order-service order-service
RUN mvn -B -pl order-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
