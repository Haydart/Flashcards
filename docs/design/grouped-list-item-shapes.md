# Grouped List Item Shapes

Pattern for a visually-grouped list card (e.g. Settings screen) where the group has one
big outer radius, and internal seams between items get a small "pinch" radius. Item count
must never be hardcoded — shape is derived from `index` + `list.size`.

## Shape helper

```kotlin
fun Modifier.groupItemShape(index: Int, count: Int, shapes: Shapes = MaterialTheme.shapes): Modifier {
    val outer = shapes.large as RoundedCornerShape       // e.g. ShapeDefaults.Large, 16.dp
    val inner = shapes.extraSmall as RoundedCornerShape  // e.g. ShapeDefaults.ExtraSmall, 4.dp — the "pinch"
    val shape = when {
        count == 1 -> outer
        index == 0 -> RoundedCornerShape(
            topStart = outer.topStart, topEnd = outer.topEnd,
            bottomStart = inner.bottomStart, bottomEnd = inner.bottomEnd
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = inner.topStart, topEnd = inner.topEnd,
            bottomStart = outer.bottomStart, bottomEnd = outer.bottomEnd
        )
        else -> inner
    }
    return this.clip(shape)
}
```

Pull radii from `MaterialTheme.shapes` tokens, not magic dp values — stays consistent if
theme shape scale changes, matches M3 token naming (`extraSmall` = spec's pinch corner).

## Non-lazy version (small, fixed group — e.g. Settings)

```kotlin
data class SettingsGroupItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val control: @Composable () -> Unit,
)

@Composable
fun SettingsGroup(items: List<SettingsGroupItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(GroupCornerRadius))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        items.forEachIndexed { index, item ->
            SettingsRow(
                item = item,
                modifier = Modifier.groupItemShape(index, items.size),
            )
            if (index != items.lastIndex) {
                HorizontalDivider()
            }
        }
    }
}
```

## Lazy version (large/scrollable grouped list)

```kotlin
@Composable
fun SettingsGroup(items: List<SettingsGroupItem>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id },
        ) { index, item ->
            Column {
                SettingsRow(
                    item = item,
                    modifier = Modifier.groupItemShape(index, items.size),
                )
                if (index != items.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}
```

Notes:
- `itemsIndexed` + `key = { _, item -> item.id }` — no manual counters, stable recomposition on insert/remove.
- Divider lives inside the same item lambda as its row, not as a separate lazy item — keeps it tied to that row's recomposition scope.
- Don't clip the outer `LazyColumn` itself for the group's outer radius — reserve `LazyColumn` for genuinely large/scrollable groups; a small fixed group should use the plain `Column` version so the group's own background+clip works.
- `count == 1` edge case must round both corners — easy to miss.

## What Material3 actually ships (and doesn't)

Checked as of Compose Material3 1.5.0-alpha — no built-in "shape by position in group" API
exists for `LazyColumn`/`Column` items in general.

- **M3 design spec** ([m3.material.io/styles/shape/corner-radius-scale](https://m3.material.io/styles/shape/corner-radius-scale))
  documents asymmetric shapes for closely-grouped items, but names **menus and split buttons**
  as the components that use it — not lists generically.
- **`ListItemShapes` / `ListItemDefaults.shapes()`** (`@ExperimentalMaterial3ExpressiveApi`,
  material3 1.4+) is real, but solves a different problem: per-**interaction-state** shape
  morphing (default/pressed/focused/dragged shape on one item), not per-**position-in-group**
  shape. Different axis — can be layered on top of `groupItemShape` if item-press morph is
  also wanted, needs `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

Conclusion: the "big radius at ends, small pinch radius at seams" grouped-list look is a
design guideline to implement manually (as above), not a ready-made Compose component.
