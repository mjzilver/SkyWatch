plugins {
  application
  alias(libs.plugins.jetbrains.kotlin.jvm)
  alias(libs.plugins.kotlin.serialization)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
  compilerOptions {
    jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
  }
}

dependencies {
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jmdns)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.logging)
  implementation(libs.exposed.core)
  implementation(libs.exposed.jdbc)
  implementation(libs.sqlite.jdbc)
  testImplementation(kotlin("test"))
}

application {
  mainClass.set("com.silversky.subtitle.server.ServerKt")
}
