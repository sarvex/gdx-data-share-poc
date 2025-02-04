import java.time.LocalDate
import java.time.format.DateTimeFormatter

dependencyLocking {
  lockAllConfigurations()
}

plugins {
  id("org.springframework.boot") version "3.0.6"
  id("io.spring.dependency-management") version "1.1.0"
  id("org.jmailen.kotlinter") version "3.14.0"
  id("org.springdoc.openapi-gradle-plugin") version "1.6.0"
  kotlin("jvm") version "1.8.21"
  kotlin("plugin.spring") version "1.8.21"
}

openApi {
  outputFileName.set("openapi.json")
  customBootRun.args.set(listOf("--spring.profiles.active=dev,localstack"))
}

group = "uk.gov.gds"

dependencies {
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-hateoas")
  implementation("org.springframework.boot:spring-boot-starter-aop")

  //  AWS dependencies for SNS, SQS etc
  implementation(platform("software.amazon.awssdk:bom:2.20.59"))
  implementation("software.amazon.awssdk:sns")
  implementation("software.amazon.awssdk:sqs")
  implementation("com.amazonaws:amazon-sqs-java-messaging-lib:2.1.1")
  implementation("software.amazon.awssdk:rds")
  implementation("software.amazon.awssdk:cognitoidentityprovider")
  implementation("software.amazon.awssdk:ssm")
  implementation(platform("com.amazonaws:aws-xray-recorder-sdk-bom:2.14.0"))
  implementation("com.amazonaws:aws-xray-recorder-sdk-spring")
  implementation("com.amazonaws:aws-xray-recorder-sdk-aws-sdk-v2")
  implementation("com.amazonaws:aws-xray-recorder-sdk-slf4j")

  implementation("org.springframework:spring-jms")
  implementation("org.hibernate:hibernate-validator:8.0.0.Final")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

  runtimeOnly("org.flywaydb:flyway-core")

  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("software.amazon.jdbc:aws-advanced-jdbc-wrapper:2.0.0")
  implementation("org.postgresql:postgresql:42.6.0")

  implementation("org.springdoc:springdoc-openapi-starter-common:2.1.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.1.0")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

  implementation("io.micrometer:micrometer-core:1.10.6")
  implementation("io.micrometer:micrometer-registry-prometheus:1.10.6")
  implementation("io.opentelemetry:opentelemetry-api:1.25.0")

  implementation("net.javacrumbs.shedlock:shedlock-spring:5.2.0")
  implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.2.0")

  implementation("com.toedter:spring-hateoas-jsonapi:2.0.4")

  implementation("org.javers:javers-spring:7.0.0")
  implementation("org.javers:javers-persistence-sql:7.0.0")

  implementation("net.logstash.logback:logstash-logback-encoder:7.3")

  // test containers
  testImplementation(platform("org.testcontainers:testcontainers-bom:1.18.0"))
  testImplementation("org.testcontainers:localstack")
  testImplementation("org.testcontainers:postgresql")
  testImplementation("com.amazonaws:aws-java-sdk-core:1.12.462") // required for TestContainers https://github.com/testcontainers/testcontainers-java/issues/1442#issuecomment-694342883

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("io.jsonwebtoken:jjwt-api:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
  testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.13")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("io.mockk:mockk:1.13.5")
  testImplementation("com.approvaltests:approvaltests:18.6.0")
  testImplementation("com.google.code.gson:gson:2.10.1") // Needed for JsonApprovals
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
repositories {
  mavenCentral()
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
  withType<Test> {
    useJUnitPlatform()
  }
}

springBoot {
  buildInfo()
}
version = getVersion()

fun getVersion(): String {
  return if (System.getenv().contains("BUILD_NUMBER")) System.getenv("BUILD_NUMBER")
  else LocalDate.now().format(DateTimeFormatter.ISO_DATE)
}
