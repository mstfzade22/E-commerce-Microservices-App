# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy gradle files
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Download dependencies
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -Dorg.gradle.jvmargs="-Xmx512m"

# Copy source code
COPY src ./src

# Build application
RUN ./gradlew bootJar --no-daemon -Dorg.gradle.jvmargs="-Xmx512m"

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from builder
COPY --from=builder /app/build/libs/ms-user-*.jar app.jar

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", \
  "-Xmx256m", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=150m", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]
