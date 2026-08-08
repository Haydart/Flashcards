# Category icon + color

Design basis for giving each Category its own icon image and accent color, matching the updated visual design for the categories list (Study tab Browse). Decided by grilling session on 2026-08-01. Not yet implemented.

## Scope

Category level only. Subcategory keeps no icon/color field — no change to `Subcategory`/`SubcategoryDto`. Subcategory rows may later reuse the parent Category's color for visual continuity (e.g. a tinted `CategoryDetailsScreen` top app bar), but that consumption is future work and requires no new field now.

## Domain model

`core/domain/.../model/Category.kt`

```kotlin
data class Category(
    val id: String,
    val name: String,
    val order: Int,
    val subcategoryCount: Int,
    val iconUrl: String,
    val color: String,
)
```

- `iconUrl` changes from `String?` (always `""` in practice today, unused by any UI) to a mandatory non-null `String`: a Firebase Storage download URL to an SVG asset.
- `color` is new: a mandatory hex color string (e.g. `"#6B2FA0"`).
- Both fields stay plain `String` in the domain layer — no `androidx.compose.ui.graphics.Color` dependency in `core/domain`. Parsing happens only at the UI edge (see Rendering).
- No fallback/nullable handling anywhere in the app for missing icon/color: these are trusted, hand-curated fields with a small, known category count (3 today). A malformed hex string should fail loudly in debug builds, not silently degrade — this is authored data, not user input.

## Firestore / DTO

`core/data/.../model/CategoryDto.kt` mirrors the domain shape with Firestore-safe defaults for deserialization:

```kotlin
data class CategoryDto(
    val id: String = "",
    val name: String = "",
    val order: Int = 0,
    val subcategoryCount: Int = 0,
    val iconUrl: String = "",
    val color: String = "",
)
```

Document shape (`categories/{categoryId}`, per ADR-0007):

```
{ name, order, subcategoryCount, iconUrl, color }
```

`FlashcardMapper.kt` gets a straight passthrough for `color`, same as the existing `iconUrl` line.

## Seed pipeline

`scripts/seed/build_fixture.py` derives Category `id`/`name`/`order`/`subcategoryCount` automatically from inbox folder slugs — there is no manual metadata input today beyond the `NAME_OVERRIDES` display-name dict. Icon URL and color cannot be derived from a slug, so add two parallel hand-maintained dicts, keyed by category slug, alongside `NAME_OVERRIDES`:

```python
CATEGORY_ICON_OVERRIDES = {
    "android": "https://firebasestorage.googleapis.com/.../android.svg",
    "python": "https://firebasestorage.googleapis.com/.../python.svg",
    "ios": "https://firebasestorage.googleapis.com/.../ios.svg",
}
CATEGORY_COLOR_OVERRIDES = {
    "android": "#6B2FA0",
    "python": "#0277BD",
    "ios": "#00838F",
}
```

Both dicts must have an entry for every category slug — `build_fixture.py` should fail fast (raise) if a slug is missing from either, rather than emitting an empty string, since the app now treats these as mandatory. `seed_firestore.py`'s header comment (documenting the doc shape) gets `color` added alongside `iconUrl`.

The 3 existing live Firestore categories (android, python, ios) need a one-time backfill of `color` (and a real `iconUrl`, replacing the current always-`""` value) before/alongside this change ships — there is no migration path in the app itself, per the "mandatory, no fallback" decision above.

## Icon asset: single-color SVG, tinted at runtime

The icon is a **monochrome glyph** (single-color silhouette, authored as SVG), not a pre-colored image. The app tints it at render time using `Category.color`, so icon color and container background color always derive from the same source of truth and can never drift out of sync with each other.

This also anticipates reusing the same tinted glyph in other contexts later (e.g. as background art in a `CategoryDetailsScreen` top app bar) — a pre-colored raster image would only work for the one context it was authored for.

Rejected: pre-colored icon images (author bakes color into the asset) — couples icon color to container color by convention only, easy to drift; local drawable/`ImageVector` lookup keyed by category — blocks adding new categories without an app release, which conflicts with categories being continuously developed server-side.

Format: **SVG**, loaded via Coil's SVG decoder (already a project dependency elsewhere — confirm `coil-svg` artifact + decoder registration if not already wired for this use case). SVG scales cleanly across the small list-tile size and any future larger context, unlike raster.

Hosting: **Firebase Storage**, consistent with the rest of the Firestore-backed content pipeline.

## Rendering

New composable in `core/ui`, sibling to `FlashcardsIconTile` rather than a modification of it — `FlashcardsIconTile` stays a simple `ImageVector`-based tile for its existing non-remote callers:

```kotlin
@Composable
fun FlashcardsRemoteIconTile(
    iconUrl: String,
    tintColor: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = tintColor.copy(alpha = DEFAULT_CONTAINER_ALPHA),
)
```

- Loads `iconUrl` via Coil `AsyncImage`, applying `ColorFilter.tint(tintColor)` to the SVG.
- `containerColor` derives from `tintColor` at the same reduced alpha `FlashcardsIconTile` already uses (`DEFAULT_CONTAINER_ALPHA`) — reuse that constant rather than redefining it.
- Fallback: while loading, on error, or when offline and never cached, render **one single generic app-wide fallback `ImageVector`** (e.g. a generic tag/folder glyph), tinted with the same `tintColor`, inside the same tinted container. Same fallback for all three cases — no distinct loading/error/offline visuals. The container tint is never blocked on the image load, since `color` is ordinary Firestore document data (cached offline like any other field) independent of the image bytes.
- `tintColor` is a plain `androidx.compose.ui.graphics.Color`, obtained by the caller via a small `String.toCategoryColor(): Color` parsing helper (living in `core/ui`, not `core/domain`) applied to `Category.color`.

Offline behavior: Coil's disk cache (already used elsewhere in the app) persists the SVG bytes after first load, so previously-viewed categories render their real icon offline; a genuinely never-loaded category falls back to the generic tinted glyph as above.

## Cleanup

`core/ui/.../composables/lists/FlashcardsListGroup.kt:285-344` — the Showkase showcase's hardcoded `categoryColorAndroid`/`categoryColorPython`/`categoryColorIos` constants and their doc comment ("standing in for a future `Category.color` domain field") become dead once real data flows through `FlashcardsRemoteIconTile`. Replace with fixture data exercising the new composable, or remove if no longer needed for showcase purposes.

## Out of scope (for now)

- Subcategory-level icon/color.
- Reusing the tinted glyph in `CategoryDetailsScreen`'s top app bar or any other secondary context — noted as a likely future use, not designed here.
- Any in-app category-management UI — icon/color are seeded via the existing Python seed pipeline / manual Firestore edits only.
