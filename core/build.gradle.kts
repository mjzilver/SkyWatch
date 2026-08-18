plugins {
  id("java-library")
  alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 } }

dependencies {
  implementation("com.hierynomus:smbj:0.14.0")
  implementation("com.rapid7.client:dcerpc:0.12.13")
  implementation("eu.agno3.jcifs:jcifs-ng:2.1.9") {
    exclude(
        group = "org.bouncycastle",
        module = "bcprov-jdk15on",
    )
  }
}
