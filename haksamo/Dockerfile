# 멀티스테이지 빌드: JDK로 jar 빌드 → 같은 JDK 이미지로 실행
# 빌드 단계
FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY gradlew .
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
COPY src ./src

RUN chmod +x gradlew && ./gradlew clean bootJar --no-daemon

# 실행 단계 (JRE만 사용해서 이미지 크기 축소)
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# non-root 사용자로 실행 (보안)
RUN addgroup --system appgroup \
    && adduser --system --ingroup appgroup appuser \
    && chown -R appuser:appgroup /app
USER appuser

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
