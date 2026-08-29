plugins {
  application
  alias(libs.plugins.jetbrains.kotlin.jvm)
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

application {
  mainClass.set("com.silversky.cli.MainKt")
}

tasks.named<JavaExec>("run") {
  standardInput = System.`in`
}

dependencies {
  implementation(project(":core"))
  implementation("org.bouncycastle:bcprov-jdk18on:1.85")
}
