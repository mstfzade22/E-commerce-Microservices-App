# Build stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m"

COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m"

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/ms-product-*.jar app.jar

EXPOSE 8082 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xmx256m", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]
