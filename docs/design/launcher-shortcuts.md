# Launcher Shortcuts

## Purpose

Pinned home screen shortcuts that deep-link directly into a specific Category or Subcategory within the app. Users create them from within the app using Android's `ShortcutManager.requestPinShortcut`.

## Shortcut types

| Type | Label | Deep-link destination |
|---|---|---|
| Category shortcut | Category name | Category Details screen |
| Subcategory shortcut | Subcategory name | Subcategory Details screen |

Future: destinations may change to Preview Study Session Screen (with the Category/Subcategory pre-selected) to reduce taps to session start. Not in current scope.

## Creation surface

Users access shortcut creation via the **overflow menu** (⋮) on:
- **Category Details screen** — "Add to home screen" → creates a Category shortcut
- **Subcategory Details screen** — "Add to home screen" → creates a Subcategory shortcut

No dedicated shortcut management screen. Creation is contextual — the user creates a shortcut for whatever screen they are currently viewing.

## Deep-link URL patterns

Follow the tab-prefixed route convention (ADR-0003). Shortcuts use the Study tab prefix:

- Category: `/study/category/{categoryId}`
- Subcategory: `/study/category/{categoryId}/subcategory/{subcategoryId}`

These routes resolve to the same `CategoryDetails` and `SubcategoryDetails` composables used in normal navigation.

## Shortcut metadata

- **Label:** Category name or Subcategory name (displayed under the shortcut icon on the launcher)
- **Short label:** Truncated version (≤10 chars) for launchers with limited space
- **Icon:** TBD at UI implementation time

## Android API

Uses `ShortcutManager.requestPinShortcut` (API 26+). The system shows a confirmation dialog asking the user to confirm placement on the home screen — this is Android-native behavior and cannot be bypassed.

Dynamic shortcuts (visible on app long-press) are not in scope for MVP. Only pinned shortcuts created explicitly by the user.
