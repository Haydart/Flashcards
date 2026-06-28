import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("com.android.application")
}

// Applied in the script body (not the plugins block) so the precompiled-script
// accessor-generation probe — which only evaluates the plugins block against a
// synthetic project — does not try to apply AGP-dependent plugins before AGP's
// extension exists. The body runs at real project configuration time.
pluginManager.apply("org.jetbrains.kotlin.android")
pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
pluginManager.apply("com.google.devtools.ksp")
pluginManager.apply("com.google.dagger.hilt.android")
pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
pluginManager.apply("com.google.gms.google-services")

android {
    compileSdk = 37
    defaultConfig {
        minSdk = 24
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true }
}

// kotlin.android is applied in the body, so its type-safe `kotlin {}` accessor is
// not generated for this precompiled script — configure the extension by type.
extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
}
