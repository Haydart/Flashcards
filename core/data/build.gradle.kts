import java.util.Properties

plugins {
    id("android-core-data")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val voiceGradingBaseUrl: String = localProperties.getProperty("VOICE_GRADING_BASE_URL", "")
val escapedVoiceGradingBaseUrl: String = voiceGradingBaseUrl
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.rossomak.flashcards.core.data"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "VOICE_GRADING_BASE_URL", "\"$escapedVoiceGradingBaseUrl\"")
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.reactive)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
