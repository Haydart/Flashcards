# Composable Content: `modifier` moves to first parameter, superseding ADR-0020's "modifier last"

## Status

Accepted. Supersedes the Composable Content clause of [ADR-0020](./0020-argument-order-conventions.md).

## Decision

**Composable Content** (`XxxContent`): `modifier` first → `state` → callbacks.

ADR-0020 placed `modifier` last for Content composables, reasoning that since `modifier` is the only optional param, "first optional param" and "last param" are the same position. That reasoning was wrong — they are different positions, and ADR-0020's own Decision line explicitly said "last." This ADR corrects it: `modifier` is always the first parameter, matching the official androidx Compose guideline and the position it already holds on `XxxScreen` composables, so the rule is now uniform across both signature types.

## Consequences

- `scripts/check-arg-order.py`'s Content check now requires `modifier_idx == 0`, not `== len(names) - 1`.
- Every existing `XxxContent` composable had `modifier` moved to the first parameter; call sites already use named arguments throughout, so this is a non-breaking reorder.
