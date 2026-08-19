plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.silversky.skywatch"
  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "com.silversky.skywatch"
    minSdk = 23
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes { release { optimization { enable = false } } }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { compose = true }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.tv.foundation)
  implementation(libs.androidx.tv.material)
  implementation("androidx.compose.material:material-icons-extended")
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  implementation(project(":core"))
  implementation("androidx.media3:media3-exoplayer:1.9.0")
  implementation("androidx.media3:media3-ui:1.9.0")
  implementation("com.google.code.gson:gson:2.10.1")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.datastore:datastore-preferences:1.1.7")
  debugImplementation(libs.leakcanary)
}
