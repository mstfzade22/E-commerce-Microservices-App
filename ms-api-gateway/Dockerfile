FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -Dorg.gradle.jvmargs="-Xmx512m"

COPY src ./src

RUN ./gradlew bootJar --no-daemon -Dorg.gradle.jvmargs="-Xmx512m"

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/build/libs/ms-api-gateway-*.jar app.jar

USER spring:spring

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xmx256m", \
  "-Xss512k", \
  "-XX:MaxMetaspaceSize=150m", \
  "-XX:+UseSerialGC", \
  "-jar", "app.jar"]
