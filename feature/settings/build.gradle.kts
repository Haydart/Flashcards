plugins {
    id("android-feature")
}

android {
    namespace = "com.rossomak.flashcards.feature.settings"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    // Debug-only: the dialog gallery (feature/settings/src/debug) renders the concrete dialogs
    // that live in :feature:study. Keeps their L3 homes correct instead of parking them in
    // :core:ui. Ships in no release build. See docs/temp/dialog-system-plan.md §9.
    debugImplementation(project(":feature:study"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
