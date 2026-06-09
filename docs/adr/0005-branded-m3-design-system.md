# Branded M3 design system: full palette + additive BrandColors layer

Material 3 is used as the foundation with its complete `ColorScheme` intact. App-specific branded colors (gradients, difficulty tints, etc.) live in a separate `BrandColors` layer exposed as `MaterialTheme.brandColors`. `BrandColors` is additive — it does not rename or replace any M3 role.

We considered trimming `ColorScheme` to only the roles our composables reference directly. Rejected because M3 components (`ModalBottomSheet`, `NavigationBar`, `Snackbar`, etc.) read roles like `surfaceDim`, `inverseSurface`, and `scrim` internally — removing them silently breaks component behavior. `Color.kt` is treated as a raw palette file; composable code never reads it directly, only through `MaterialTheme.colorScheme` or `MaterialTheme.brandColors`.

We considered renaming M3 roles into app-specific vocabulary (e.g. `actionColor` instead of `primary`). Rejected because M3 components already consume `colorScheme` internally, so a parallel vocabulary creates a maintenance burden without eliminating `colorScheme` usage. M3 naming is kept for M3 slots; `brandColors` covers only what M3 has no role for.

**`BrandColors` structure:**
- Exposed as `MaterialTheme.brandColors` via `LocalBrandColors` (`staticCompositionLocalOf`)
- Provided in `FlashcardsTheme` alongside `MaterialTheme`, switching between `lightBrandColors` / `darkBrandColors` on `darkTheme`
- Gradients stored as `Brush` objects (direction and color stops are design-system decisions, not call-site decisions)
- Slots added incrementally as screens concretely need them — no speculative pre-population

**Call-site convention:**
```kotlin
MaterialTheme.colorScheme.primary         // M3 roles
MaterialTheme.brandColors.topBarGradient  // branded extras
```
