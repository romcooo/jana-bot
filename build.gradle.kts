import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.21"
}

group = "sk.koppakurhiev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("com.github.elbekD:kt-telegram-bot:1.3.8")
    implementation("io.github.microutils:kotlin-logging:2.0.4")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "15"
}