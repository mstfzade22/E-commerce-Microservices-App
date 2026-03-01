FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/cart-service-*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
