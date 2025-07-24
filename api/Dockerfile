# Stage 1: Build the application
FROM gradle:jdk21-alpine AS builder

WORKDIR /app

# Copy Gradle build files and source code
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Build the Spring Boot application
RUN gradle bootJar

# Stage 2: Create the final runtime image
FROM amazoncorretto:21-alpine


WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]