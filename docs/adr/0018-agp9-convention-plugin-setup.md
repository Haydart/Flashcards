# AGP 9 convention plugins: classic Kotlin + KSP, body-applied plugins, shared classloader

## Decision

The `build-logic/` convention plugins (introduced by ADR-0011) are implemented for **AGP 9.2.1 + Kotlin 2.3.0** with four non-obvious constraints:

1. **Classic Kotlin, not AGP built-in Kotlin.** `android.builtInKotlin=false` and `android.newDsl=false`; `kotlin("android")` is applied explicitly.
2. **KSP, not kapt**, for Hilt annotation processing (`com.google.devtools.ksp` 2.3.0).
3. **Plugins applied in the convention-script body** via `pluginManager.apply("…")`, with only the AGP plugin in the `plugins {}` block.
4. **KSP declared `apply false` in the root `build.gradle.kts`** so Hilt and KSP load from one classloader.

These flags are set in both the root `gradle.properties` and `build-logic/gradle.properties` (the included build does not inherit them).

## Context

ADR-0011 specified `build-logic/` precompiled script plugins but predates this project's upgrade to AGP 9.2.1, where Kotlin support is built into AGP and several long-standing plugin patterns changed. The convention-plugin recipe as originally drafted (kapt + every plugin in the `plugins {}` block + AGP built-in Kotlin) does not build. Each constraint above is forced by a concrete failure surfaced during the Phase 1 migration.

## Rationale — why each constraint

**KSP instead of kapt.** Under AGP 9 with built-in Kotlin, applying `org.jetbrains.kotlin.kapt` fails: *"The 'org.jetbrains.kotlin.kapt' plugin is not compatible with built-in Kotlin support."* kapt is legacy and unmaintained relative to KSP; Hilt 2.58 supports KSP. Moving to KSP is the modernization, not a workaround.

**Classic Kotlin instead of built-in.** Built-in Kotlin is AGP 9's intended direction, but **KSP 2.3.0 is incompatible with it** — Gradle errors outright: *"KSP is not compatible with Android Gradle Plugin's built-in Kotlin. Please disable by adding android.builtInKotlin=false and apply kotlin("android")."* Since Hilt requires an annotation processor and KSP is the only viable one, built-in Kotlin is a dead end until KSP supports it. We stay on classic Kotlin and revisit when KSP catches up.

**Body-applied plugins.** Gradle's `generatePrecompiledScriptPluginAccessors` task evaluates only the `plugins {}` block of each precompiled script against a throwaway synthetic project, to discover type-safe accessors. AGP-dependent plugins (Hilt, KSP, serialization, compose, google-services) blow up in that probe — e.g. Hilt: *"Could not find the Android Gradle Plugin (AGP) base extension."* Applying them in the script **body** with `pluginManager.apply(...)` defers application to real project configuration time, after AGP's extension exists, and the probe never sees them. Only the AGP plugin itself stays in `plugins {}`, because the `android {}` type-safe accessor must be generated.

**Configure Kotlin by type.** Because `kotlin("android")` is applied in the body, its `kotlin {}` type-safe accessor is not generated. The Kotlin extension is configured via `extensions.configure<KotlinAndroidProjectExtension> { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }`.

**Root `apply false` for the shared classloader.** With KSP applied only inside a sub-project's convention plugin while Hilt resolved elsewhere, the two plugins land in different classloaders and Gradle fails: *"The KSP plugin was detected to be applied but its task class could not be found … Hilt Gradle Plugin is using a different class loader"* (dagger #3965). Declaring every plugin — KSP included — `apply false` at the root `build.gradle.kts` loads them all from the root buildscript classloader, which sub-projects then reuse.

**`compileOnly` vs `implementation` in `build-logic/build.gradle.kts`.** The AGP marker is `compileOnly` (referenced from the `plugins {}` block at build-logic compile time). Every marker applied programmatically in a script body — kotlin, compose, serialization, hilt, ksp, google-services — must be `implementation`, so it reaches the **consuming** project's runtime classpath; otherwise `pluginManager.apply("…")` fails with "Plugin not found".

**`build-logic` catalog + properties wiring.** `build-logic/settings.gradle.kts` must declare its own repositories and re-create the version catalog (`from(files("../gradle/libs.versions.toml"))`), or `libs.*` is unresolved inside build-logic. `build-logic/gradle.properties` must repeat `android.builtInKotlin=false` / `android.newDsl=false`, because the included build does not inherit them during accessor generation.

## Alternatives considered

- **Migrate fully to AGP built-in Kotlin** — rejected (blocked, not chosen). KSP 2.3.0 refuses built-in Kotlin; Hilt needs an annotation processor. Revisit when KSP adds support.
- **Keep kapt** — rejected. Incompatible with AGP 9 built-in Kotlin and legacy; KSP is the forward path and faster.
- **All plugins in the `plugins {}` block** (as ADR-0011 drafted) — rejected. Fails `generatePrecompiledScriptPluginAccessors` under AGP 9.
- **Binary `Plugin<Project>` convention plugins instead of precompiled `*.gradle.kts`** — viable and sidesteps accessor generation entirely, but a larger rewrite. The body-applied precompiled-script approach keeps ADR-0011's file shape with minimal divergence. Reconsider if the body-application pattern becomes unwieldy as modules grow.

## Consequences

- Each Android module applies one convention plugin (`android-app` / `android-feature` / `android-core-android`); `:core:domain` uses `android-core-kotlin` (pure Kotlin, immune to all of the above).
- Hilt processors are declared `ksp(libs.hilt.android.compiler)`, not `kapt(...)`, in every module.
- Adding a new annotation-processor-based plugin later means: add its marker as `implementation` in `build-logic/build.gradle.kts`, `apply false` at the root, and `pluginManager.apply` it in the relevant convention script body.
- The `android.builtInKotlin` / `android.newDsl` flags must stay `false` in both `gradle.properties` files until KSP supports built-in Kotlin — flipping either re-breaks the build.
- Builds against ADR-0011's module architecture; see `docs/temp/multi-module-migration-plan.md` Phase 1 for the concrete files.
