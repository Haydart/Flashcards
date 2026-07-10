plugins {
    id("android-core-kotlin")
}

// Architecture-rule tests. Konsist scans the whole project via its scope API, so this
// module has no production code — only tests asserting cross-module conventions.
dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
