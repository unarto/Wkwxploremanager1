// [Jalur Class/Modul]: /treeview/build.gradle.kts
// [Penjelasan]: Menggunakan Version Catalog libs.junit dan libs.kotlinx.coroutines.test, serta menghapus unused dependency androidx.appcompat.
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.compose)
}
android {
  namespace = "com.wakwau.xplore.treeview"
  compileSdk { version = release(36) { minorApiLevel = 1 } }
  defaultConfig {
    minSdk = 24
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
  }
}
dependencies {
  implementation(project(":core-ui"))
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
