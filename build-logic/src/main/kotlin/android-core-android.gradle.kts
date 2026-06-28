import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("com.android.library")
}

// See android-app.gradle.kts for why these are applied in the body.
pluginManager.apply("org.jetbrains.kotlin.android")
pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
pluginManager.apply("com.google.devtools.ksp")
pluginManager.apply("com.google.dagger.hilt.android")
pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

android {
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}
