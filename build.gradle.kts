plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
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

val ktorVersion = "2.3.13"
val nettyVersion = "4.1.133.Final"
val logbackVersion = "1.4.12"

dependencies {

    constraints {
        implementation("io.netty:netty-buffer:$nettyVersion")
        implementation("io.netty:netty-codec:$nettyVersion")
        implementation("io.netty:netty-codec-http:$nettyVersion")
        implementation("io.netty:netty-codec-http2:$nettyVersion")
        implementation("io.netty:netty-common:$nettyVersion")
        implementation("io.netty:netty-handler:$nettyVersion")
        implementation("io.netty:netty-resolver:$nettyVersion")
        implementation("io.netty:netty-transport:$nettyVersion")
        implementation("io.netty:netty-transport-native-epoll:$nettyVersion")
        implementation("io.netty:netty-transport-native-kqueue:$nettyVersion")
        implementation("io.netty:netty-transport-classes-epoll:$nettyVersion")
        implementation("io.netty:netty-transport-classes-kqueue:$nettyVersion")
        implementation("io.netty:netty-transport-native-unix-common:$nettyVersion")
    }

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    // =========================
    // KTOR SERVER
    // =========================
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion") {
        exclude(group = "io.netty")
    }
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-codec-http:$nettyVersion")
    implementation("io.netty:netty-codec-http2:$nettyVersion")
    implementation("io.netty:netty-common:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-resolver:$nettyVersion")
    implementation("io.netty:netty-transport:$nettyVersion")
    implementation("io.netty:netty-transport-native-epoll:$nettyVersion")
    implementation("io.netty:netty-transport-native-kqueue:$nettyVersion")
    implementation("io.netty:netty-transport-classes-epoll:$nettyVersion")
    implementation("io.netty:netty-transport-classes-kqueue:$nettyVersion")
    implementation("io.netty:netty-transport-native-unix-common:$nettyVersion")

    // =========================
    // KTOR CLIENT (HTTP)
    // =========================
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    // Database - Exposed + H2 (in-memory)
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("com.h2database:h2:2.2.220")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // Mail
    implementation("com.sun.mail:javax.mail:1.6.2")
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion") {
        exclude(group = "ch.qos.logback", module = "logback-core")
    }
    implementation("ch.qos.logback:logback-core:$logbackVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion") {
        exclude(group = "commons-codec", module = "commons-codec")
    }
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
    testImplementation("commons-codec:commons-codec:1.17.1")
}

tasks.test {
    useJUnitPlatform()
}
