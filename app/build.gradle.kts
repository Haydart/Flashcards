import java.util.Properties

plugins {
    id("android-app")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { load(it) }
    }
}
val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID", "")

// Bitrise doesn't have local props file, so it will have to call assembleRelease with the -P option and directly passed creds
fun releaseSigningProperty(name: String): String? =
    project.findProperty(name) as String? ?: localProperties.getProperty(name)

val gitCommitCount: Int = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toInt()

val versionMajor = 0
val versionMinor = 1

android {
    namespace = "com.rossomak.flashcards"

    signingConfigs {
        create("release") {
            storeFile = releaseSigningProperty("RELEASE_STORE_FILE")?.let { file(it) }
            storePassword = releaseSigningProperty("RELEASE_STORE_PASSWORD")
            keyAlias = releaseSigningProperty("RELEASE_KEY_ALIAS")
            keyPassword = releaseSigningProperty("RELEASE_KEY_PASSWORD")
        }
    }

    defaultConfig {
        applicationId = "com.rossomak.flashcards"
        versionCode = gitCommitCount
        versionName = "$versionMajor.$versionMinor.$gitCommitCount"

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:data"))
    implementation(project(":core:voice"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:study"))
    implementation(project(":feature:settings"))
    debugImplementation(project(":feature:voicedebug"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}
