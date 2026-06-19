# Multi-module architecture: feature modules with shared core

## Decision
The project is structured as a multi-module Gradle build with one module per user flow (`:feature:*`), a thin `:app` shell, and three shared core modules. Navigation between features uses callbacks wired in `:app`'s NavGraph — features never reference each other's routes or classes.

## Context
Portfolio project designed to demonstrate enterprise-grade Android development. A single-module structure works at the current scale but does not showcase the architectural skills expected in professional Android teams. Modularization is the de facto standard in large Android codebases (see Google's Now in Android). This ADR establishes the target architecture before the codebase grows further. Migration is incremental — not a one-shot rewrite.

## Module structure

| Module | Contents | Depends on |
|---|---|---|
| `:app` | `MainActivity`, `NavGraph`, splash/startup, `@HiltAndroidApp` | all modules |
| `:feature:auth` | `LoginScreen` + VM | `:core:domain`, `:core:design` |
| `:feature:home` | `HomeScreen` + VM | `:core:domain`, `:core:design` |
| `:feature:browse` | `CategoryDetailsScreen`, `SubcategoryDetailsScreen` + VMs | `:core:domain`, `:core:design` |
| `:feature:study` | `PreStartScreen`, `StudySessionScreen`, `SessionSummaryScreen` + VMs | `:core:domain`, `:core:design` |
| `:feature:flags` | `FlagsScreen` + VM | `:core:domain`, `:core:design` |
| `:feature:settings` | `SettingsScreen` + VM | `:core:domain`, `:core:design` |
| `:core:domain` | models, repository interfaces, use cases — pure Kotlin, no Android deps | nothing |
| `:core:data` | repository impls, DTOs, mappers, Firestore sources, Hilt bindings | `:core:domain` |
| `:core:design` | Theme, colors, typography, spacing, shared composables | Compose/M3 only |

**Strict rule:** no `:feature:*` may depend on another `:feature:*`. No `:core:*` may depend on `:app` or any `:feature:*`.

## Navigation
Callback-based. Feature screens receive typed `() -> Unit` (or parametrised) lambdas for every nav action. The root `NavGraph` in `:app` wires callbacks to `navController.navigate()`. All route strings live only in `:app`. Rationale: renaming a route or moving a screen only requires changes in `:app`, not across all callers.

## Key rationale

**Hilt bindings in `:core:data`, not `:app`** — bindings live alongside their implementations. Every new repository is self-contained; `:app` never needs touching for new data wiring.

**`:feature:study` holds all three session screens** — `PreStartScreen`, `StudySessionScreen`, `SessionSummaryScreen` form one user journey and share session state. Splitting them buys no isolation.

**No `:core:navigation` module** — with callbacks, no feature ever imports a route string. A dedicated module would have one consumer (`:app`) and add Gradle overhead for nothing.

**Splash/startup in `:app`** — startup routing (auth check → home vs login) is assembly logic, not an auth feature concern.

## Alternatives considered
- **Layer modules** (`:presentation`, `:domain`, `:data`) — rejected. Doesn't enforce feature isolation; a developer can still freely import across features. Not how enterprise teams structure Android projects.
- **All Hilt bindings in `:app`** — rejected. Creates a merge conflict hotspot; every new repository requires touching `:app`.
- **Big-bang migration** — rejected. Incremental migration starting with `:feature:settings` (fewest dependencies, lowest risk).

## Consequences
- Parallel Gradle compilation reduces incremental build times as the project grows.
- Illegal cross-feature imports are compile errors, not code review findings.
- Each module gets its own isolated test source set.
- Each new module requires its own `build.gradle.kts` and Hilt wiring — expected overhead.
