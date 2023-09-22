plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "io.bouckaert"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-net:commons-net:3.9.0")
    implementation("com.sksamuel.hoplite:hoplite-json:2.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}