plugins {
    `java`
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    // JPA (BaseEntity에서 사용)
    compileOnly("jakarta.persistence:jakarta.persistence-api:3.1.0")
    compileOnly("org.springframework.data:spring-data-jpa:3.2.3")
}
