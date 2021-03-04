plugins {
    kotlin("jvm") version "1.4.21"
    application
}

group = "sk.koppakurhiev"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    jcenter()
}

dependencies {
    implementation("com.google.api-client:google-api-client:1.30.4")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.6")
    implementation("com.google.apis:google-api-services-sheets:v4-rev581-1.25.0")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    // bot
    implementation("com.github.elbekD:kt-telegram-bot:1.3.8")
    // logging
    implementation("io.github.microutils:kotlin-logging:2.0.4")
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
    // json
    implementation("com.beust:klaxon:5.0.1")
    // coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

application {
    mainClass.set("org.koppakurhiev.janabot.JanaBotApplicationKt")
}

tasks.test {
    useJUnit()
}
