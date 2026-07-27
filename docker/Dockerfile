# JDK 21 — Gradle Wrapper + IntelliJ Platform Plugin build/test
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Run as root so named volumes (`gradle-cache`) are writable without chown dances.
# Matches the maven services in spring-fluent-query / spring-fluent-map.
ENTRYPOINT ["/entrypoint.sh"]
CMD ["./gradlew", "check", "--no-daemon"]
