# Fixed argument order per signature type, deviating from Compose's modifier-then-optional rule for Screens

## Status

Accepted.

## Decision

Four signature types get one fixed, non-negotiable parameter order (documented in AGENTS.md §Argument Order):

- **Composable Screens** (`XxxScreen`): `modifier` → `viewModel` → nav callbacks (`onNavigateBack` first, then remaining callbacks happy-path-first).
- **Composable Content** (`XxxContent`): `state` → callbacks → `modifier` last.
- **ViewModel constructors**: `SavedStateHandle` (if present) → use cases → gateways/controllers.
- **Repository / DataSource / UseCase multi-param methods**: identifiers/keys → payload/action/value.

## Context

An audit of existing Screen composables found real drift: `viewModel` and `modifier` swapped positions across files, and `modifier` was missing entirely on 6 of 9 Screens. The official [androidx Compose component API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-component-api-guidelines.md) mandate required params, then a single `modifier: Modifier = Modifier` as the first optional param, then remaining optional params — which would put `viewModel` after `modifier` but nav callbacks (required, no default) *before* both.

We deliberately deviate from that guideline's ordering of nav callbacks: they're placed **last**, after `modifier` and `viewModel`, not first. The androidx guideline targets reusable design-system elements meant to be composed and modifier-chained by arbitrary callers; `XxxScreen` composables are navigation destinations consumed only by `NavGraphBuilder`, always with named arguments. Every existing call site in `NavGraph.kt` already calls Screens with full named arguments, so a required param sitting after defaulted ones is not a positional-call hazard here.

## Rationale

- **`modifier` before `viewModel`**: matches the official guideline's "modifier is the first optional param" rule — no reason to deviate here since it's cheap to follow and keeps Screens consistent with any future reusable Compose components in the codebase.
- **Nav callbacks last, `onNavigateBack` first among them**: groups the two "wiring" params (`modifier`, `viewModel`) at the top, since they're boilerplate every Screen has; nav callbacks are what's actually variable per screen and read better clustered together, back-navigation first since it's the most universal.
- **Content composables need no exception**: `modifier` is their only optional param, so "modifier is the first optional param" and "modifier is last" are the same position — no conflict with the official guideline.

## Alternatives considered

- **Strict androidx guideline (nav callbacks first, as required params)** — rejected; would keep Screens reusable-library-style even though they're never chained or wrapped by other composables in this codebase.
- **No fixed order, case-by-case** — rejected; this is exactly the status quo that produced the drift being fixed.

## Consequences

- `scripts/check-arg-order.sh` provides a grep-based check since no Detekt/ktlint custom-rule infra exists yet; heuristic, not AST-verified. AGENTS.md §Argument Order tells agents to run it after touching any Screen/Content/ViewModel/Repository file.
