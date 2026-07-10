import com.diffplug.gradle.spotless.SpotlessExtension
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Shared static-analysis convention: Spotless (ktlint formatting) + detekt (smells) +
// Android Lint baseline wiring. Applied from the language/module convention plugins so
// every module gets the same tasks. Plugins applied in the body via pluginManager.apply
// to avoid the precompiled-script-plugin accessor probe (same reason as AGP plugins).
pluginManager.apply("com.diffplug.spotless")
pluginManager.apply("io.gitlab.arturbosch.detekt")

// ktlint rule overrides applied on top of the repo-root .editorconfig. `backing-property-naming`
// clashes with the ViewModel `_prefix` idiom for private MutableStateFlow with no public mirror.
// KNOWN LIMITATION (see feedback_ktlint_no_autocollapse memory / PR discussion): disabling
// `function-signature`, `parameter-list-wrapping`, `argument-list-wrapping`, and the two
// `trailing-comma-on-*` rules this way — the officially documented Spotless mechanism — does NOT
// take effect against ktlint 1.5.0 via Spotless 7.0.4 in this project; verified the key format is
// correct (decompiled the rule IDs) and ruled out caching/staleness, an explicit older ktlint
// version, and `setEditorConfigPath`. ktlint still collapses multi-line signatures and re-adds/
// strips trailing commas on `spotlessApply`/`formatCode`. Left here as the documented-correct
// config in case a future Spotless/ktlint upgrade fixes the underlying bug; until then, treat
// signature/trailing-comma formatting as NOT enforced by this task — never run `formatCode`
// blindly on files with intentional multi-line signatures without reviewing the diff first.
val ktlintOverrides = mapOf(
    "ktlint_standard_backing-property-naming" to "disabled",
    "ktlint_standard_function-signature" to "disabled",
    "ktlint_standard_parameter-list-wrapping" to "disabled",
    "ktlint_standard_argument-list-wrapping" to "disabled",
    "ktlint_standard_parameter-wrapping" to "disabled",
    "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
    "ktlint_standard_trailing-comma-on-call-site" to "disabled",
)

extensions.configure<SpotlessExtension> {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")
        ktlint().editorConfigOverride(ktlintOverrides)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint().editorConfigOverride(ktlintOverrides)
    }
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    // Per-module baseline; created by `./gradlew detektBaseline`. Missing file is ignored.
    baseline = file("detekt-baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

// The baseline task is a separate type, so it needs the jvmTarget set independently —
// otherwise detekt derives it from the running JDK (23), which detekt 1.23.x rejects.
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

// Android Lint: freeze existing findings via a per-module baseline. No rule re-tuning here.
pluginManager.withPlugin("com.android.library") {
    extensions.configure<com.android.build.api.dsl.LibraryExtension>("android") {
        lint { baseline = file("lint-baseline.xml") }
    }
}
pluginManager.withPlugin("com.android.application") {
    extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
        lint { baseline = file("lint-baseline.xml") }
    }
}
