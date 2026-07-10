plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-test-fixtures")
}

pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
pluginManager.apply("android-quality")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
}
