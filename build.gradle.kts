plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}
val springCloudGcpVersion by extra("7.0.0")

group = "com"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.google.cloud:spring-cloud-gcp-starter-storage")
    implementation("com.google.cloud:google-cloud-storage")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testRuntimeOnly("com.h2database:h2")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    implementation("org.springframework.boot:spring-boot-starter-websocket")
    testImplementation ("org.springframework:spring-messaging")
    testImplementation ("org.springframework:spring-websocket")

    // Swagger UI 및 OpenAPI 3.0 문서화
    implementation("io.swagger.core.v3:swagger-annotations:2.2.34")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // JWT 관련 추가
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Redis 관련 추가
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

}
dependencyManagement {
    imports {
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:$springCloudGcpVersion")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 배포시 경로 잡아주기
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    mainClass.set("com.back.BackApplication") // ◀◀ @SpringBootApplication이 있는 클래스의 전체 경로
}