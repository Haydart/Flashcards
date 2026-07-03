plugins {
    id("android-core-kotlin")
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.javax.inject)
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
}
