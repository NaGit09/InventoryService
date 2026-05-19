# Stage 1: Cache dependencies and build the fat JAR
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests

# Stage 2: Extract Spring Boot Layered JAR (Using standard alpine + openjdk)
FROM alpine:3.19 AS extractor
RUN apk add --no-cache openjdk17-jre
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Final lightweight runtime image
FROM alpine:3.19
RUN apk add --no-cache openjdk17-jre
WORKDIR /app

# Copy the extracted layers
COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

EXPOSE 8081

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]