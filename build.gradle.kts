plugins {
    kotlin("jvm") version "1.9.22"
    id("application")
}

group = "com.flightalert"
version = "0.1.0"

application {
    mainClass = "com.flightpricealert.ApplicationKt"
}


repositories {
    mavenCentral()
}

dependencies {

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // =========================
    // KTOR SERVER
    // =========================
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.7")

    // =========================
    // KTOR CLIENT (HTTP)
    // =========================
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}
