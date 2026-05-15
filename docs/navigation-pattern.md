# Navigation Pattern

Use `StateFlow`-based navigation, not `SharedFlow`. Navigation destination is a nullable field in the screen's state class.

## Composable Taxonomy

- `XxxScreen` — nav-aware entry point; holds ViewModel, observes state, triggers navigation callbacks
- `XxxContent` — stateless UI; accepts state and callbacks, no ViewModel, fully previewable

## Rules

- All destination sealed interfaces implement `NavigationDestination` (`ui/navigation/NavigationDestination.kt`)
- Each screen has a `XxxDestination.kt` sealed interface listing possible next destinations
- `XxxScreenState` holds `navigationDestination: XxxDestination? = null`
- ViewModel sets destination via `_state.update { it.copy(navigationDestination = XxxDestination.Foo) }`
- `XxxScreen` observes via `LaunchedEffect(state.navigationDestination)` and calls nav callback
- No destination reset needed when screen is popped inclusive from back stack

## Why not SharedFlow

With default config (`replay=0`, no buffer), SharedFlow drops events when no collector is active — e.g., during recomposition or before the composable attaches. Replay/buffering can mitigate this, but navigation destination is state, not an event: it has a current value, survives recomposition, and is naturally idempotent. StateFlow models that directly without extra configuration.

## File layout per screen

- `XxxDestination.kt` — sealed interface implementing `NavigationDestination`, lists possible nav targets
- `XxxScreenState.kt` — UI state data class including `navigationDestination` field
- `XxxViewModel.kt` — sets `navigationDestination` on state
- `XxxScreen.kt` — contains both `XxxScreen` (nav entry point) and `XxxContent` (stateless UI)

## Example

```kotlin
// ui/navigation/NavigationDestination.kt
interface NavigationDestination

// XxxDestination.kt
sealed interface XxxDestination : NavigationDestination {
    data object Home : XxxDestination
    data object Login : XxxDestination
}

// XxxScreenState.kt
data class XxxScreenState(
    val navigationDestination: XxxDestination? = null
)

// XxxViewModel.kt
_state.update { it.copy(navigationDestination = XxxDestination.Login) }

// XxxScreen.kt — nav entry point
@Composable
fun XxxScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: XxxViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationDestination) {
        when (state.navigationDestination) {
            XxxDestination.Home -> onNavigateToHome()
            XxxDestination.Login -> onNavigateToLogin()
            null -> Unit
        }
    }

    XxxContent(state = state, onAction = viewModel::onAction)
}

// XxxScreen.kt — stateless UI
@Composable
fun XxxContent(
    state: XxxScreenState,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) { ... }
```
