plugins {
    kotlin("jvm") version "1.4.21"
    application
}

group = "sk.koppakurhiev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    // bot
    implementation("com.github.elbekD:kt-telegram-bot:1.3.8")
    // logging
    implementation("io.github.microutils:kotlin-logging:2.0.4")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    // json
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.8.+")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
    mainClass.set("org.koppakurhiev.janabot.JanaBotApplicationKt")
}

tasks.test {
    useJUnit()
}
