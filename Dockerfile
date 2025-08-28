# =========================
# 빌드 스테이지
# =========================
FROM amazoncorretto:17 AS builder

WORKDIR /app

# Gradle Wrapper 복사
COPY gradle ./gradle
COPY gradlew ./gradlew
COPY build.gradle settings.gradle ./
RUN chmod +x ./gradlew && ./gradlew dependencies

# 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew build -x test --no-daemon


# =========================
# 런타임 스테이지
# =========================
FROM amazoncorretto:17-alpine3.21
WORKDIR /app

# 실행 가능한 JAR만 복사 (plain.jar 제외)
COPY --from=builder /app/build/libs/*-SNAPSHOT.jar app.jar

# Spring Boot 기본 포트 (8080)
EXPOSE 8080

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
