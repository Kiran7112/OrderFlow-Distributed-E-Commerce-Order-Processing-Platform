# Multi-module reactor build — build context is the services/ directory
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy the whole reactor (parent POM + all module sources)
COPY . .

# Build only this module (and any reactor dependencies)
RUN mvn -B -pl inventory-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /build/inventory-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
