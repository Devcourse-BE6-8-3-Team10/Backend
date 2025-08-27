## 배포 준비를 위한 Dockerfile을 작성합니다.(현재는 작동 X)
## 1. 빌드(컴파일)를 위한 단계
#FROM openjdk:21-jdk-slim as builder
#WORKDIR /app
#COPY gradlew .
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#RUN chmod +x ./gradlew
#RUN ./gradlew bootJar --no-daemon
#
## 2. 실제 실행을 위한 최종 단계
#FROM eclipse-temurin:21-jre-jammy
#WORKDIR /app
#COPY --from=builder /app/build/libs/*.jar app.jar
#EXPOSE 8080
#CMD ["java", "-jar", "app.jar"]