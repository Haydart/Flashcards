# Unified button component: 4 types × 2 sizes

## Decision

`core:ui` gets a family of four button composables — `FlashcardsFilledButton`, `FlashcardsTonalButton`, `FlashcardsOutlinedButton`, `FlashcardsTextButton`. Each type is its own composable (not one `FlashcardsButton(type, size, ...)` entry point), living in its own file, plus a shared `FlashcardsButtonSize` enum in its own file.

**Sizes.** Exactly two: `FlashcardsButtonSize.Normal` (56dp, M3's native "Medium" tier) and `FlashcardsButtonSize.Small` (32dp, M3's native "ExtraSmall" tier). Naming follows the app's existing spacing/sizing vocabulary (`Normal`/`Small`), not raw M3 terms. The other three M3 expressive tiers (S-40, L-96, XL-136) are not implemented — nothing in the target design uses them.

**Icon slot.** One param, not two: `icon: ImageVector? = null` plus an `iconPosition: IconPosition` enum (`Leading`/`Trailing`). No button in the design ever needs both a leading and trailing icon at once, so a single slot matches actual usage and keeps call sites simpler than a `leadingIcon`/`trailingIcon` pair.

**Geometry foundation.** M3's own per-size expressive tokens (32dp/56dp height, 20dp/24dp icon, 8dp gap, 16dp/24dp padding, full-pill radius) are `internal` in `material3-android:1.4.0` and unreachable from app code, so these values are declared directly in the project's own token files instead: content padding and icon-label gap reuse existing `AppSpacing` entries (`spacing.normal`=16dp, `spacing.medium`=24dp, `spacing.xsmall`=8dp — exact matches), button height and icon size are four new fields on `AppSizes` (`buttonHeightNormal`/`buttonHeightSmall`/`buttonIconSizeNormal`/`buttonIconSizeSmall`), and shape reuses existing `cornerRadius.full`.

**Shape.** Static full pill (stadium) at every size and type. No M3 expressive shape-morph-on-press — that feature targets round buttons (FABs/IconButtons) per Material docs, not labeled pill buttons.

**Elevation.** Flat, zero elevation on all four types at both sizes — including `Filled`.

**Press feedback.** Default M3 ripple only. No scale-down press animation.

**Colors** — mapped onto existing `Color.kt` roles rather than introducing new hardcoded values, so the button family stays consistent with the rest of the theme in both light and dark:

| Type | Container | Content |
|---|---|---|
| Filled | `brandColors.ctaButtonGradient` (existing) | `onPrimary` (existing) |
| Tonal | `secondaryContainer` | `onSecondaryContainer` |
| Outlined | transparent, `primary` border | `primary` |
| Text | transparent | `primary` |
| Disabled (all types) | `onSurface` @ 12% alpha | `onSurface` @ 38% alpha |
| On-gradient surface | `brandColors.topBarGradient` (existing, theme-fixed) | — |
| On-gradient Filled | white | new `BrandColors.onGradientFilled` = `primaryLight` (#7D3AC8), fixed in both themes |
| On-gradient Tonal/Outlined/Text | white @ varying alpha | `onTopBarGradient` (existing, white, fixed) |

Disabled uses the standard M3 low-alpha convention (`onSurface` at reduced alpha) rather than a bespoke opaque color, since it's automatically correct in both themes without adding a new token.

**On-gradient variant** is in scope now — a `style: FlashcardsButtonStyle = FlashcardsButtonStyle.Surface` param (`Surface`/`OnGradient`) on all four composables, not separate `*OnGradient` composables. Matches the identical axis already modeled this way on `FlashcardsMetadataBadge` (`MetadataBadgeStyle`). No current screen places these buttons on a gradient surface yet.

**Scope exclusions:** no loading state; representative preview/Showkase coverage per type (not the full type×size×icon-position×surface combinatorial matrix).

## Consequences

- `BrandColors` gains one new member, `onGradientFilled`, fixed (non-theme-flipping) like `onTopBarGradient`.
- Exact on-gradient color/alpha values beyond what's specified here may be revisited once a concrete screen actually consumes `onGradientBackground` (none exist yet).
