FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

RUN chmod +x gradlew && ./gradlew dependencies --no-daemon -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m"

COPY src ./src

RUN ./gradlew bootJar -x test --no-daemon -Dorg.gradle.jvmargs="-Xmx768m -XX:MaxMetaspaceSize=256m"

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/ms-cart-*.jar app.jar

EXPOSE 8084

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8084/api/v1/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xmx512m", \
  "-Xss2m", \
  "-XX:MaxMetaspaceSize=256m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", "app.jar"]
