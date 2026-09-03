// [Jalur Class/Modul]: /core-worker/build.gradle.kts
// [Penjelasan]: Menyelaraskan format deklarasi compileSdk release(36) minorApiLevel=1 sesuai standar AGP multi-modul.
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.wakwau.xplore.core.worker"
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core-storage-api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
