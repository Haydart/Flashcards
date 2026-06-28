plugins {
    id("com.android.library")
}

pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
pluginManager.apply("com.google.devtools.ksp")
pluginManager.apply("com.google.dagger.hilt.android")
pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

android {
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}
