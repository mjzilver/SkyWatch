plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false

    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}
