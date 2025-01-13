plugins {
    java
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "com.fergie"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
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

configurations {
    all {
        exclude(group = "org.liquibase")
    }
}


dependencies {
    implementation ("org.hibernate:hibernate-core:6.2.0.Final")
//    implementation("org.eclipse.persistence:eclipselink:3.0.2")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//    {
//        exclude(group = "org.hibernate", module = "hibernate-core")
//    }
    implementation("org.hibernate.validator:hibernate-validator:8.0.0.Final")
//    {
//        exclude(group = "org.hibernate", module = "hibernate-core")
//        exclude(group = "org.hibernate", module = "hibernate-entitymanager")
//    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation ("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging")
    implementation ("org.modelmapper:modelmapper:3.2.1")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation ("io.minio:minio:8.4.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("test") {
    enabled = false
}
