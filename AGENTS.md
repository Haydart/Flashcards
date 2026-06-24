# Android Flashcards — Code Guidelines

@CONTEXT.md

## Project Overview
- **Package Name**: `com.rossomak.flashcards`
- **Architecture**: Clean Architecture with MVVM
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## Architecture Guidelines

### Clean Architecture Layers
```
presentation/   → ViewModels, Compose UI, UI State classes
domain/         → Use Cases, Domain Models, Repository Interfaces
data/           → Repository Implementations, Data Sources (Remote/Local), DTOs, Mappers
```

**Dependency Rule**: Outer layers depend on inner layers only
- Presentation depends on Domain
- Domain has NO dependencies (pure Kotlin)
- Data depends on Domain (implements repository interfaces)

### MVVM Pattern (Presentation Layer)
- **ViewModel**: Holds UI state, orchestrates use cases, survives configuration changes
- **UI State**: Single data class representing screen state
- **Composables**: Stateless when possible, receive state and emit events

Example structure:
```kotlin
@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val getFlashcardsUseCase: GetFlashcardsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FlashcardScreenState())
    val state: StateFlow<FlashcardScreenState> = _state.asStateFlow()

    fun loadFlashcards() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getFlashcardsUseCase()
                .onSuccess { cards -> _state.update { it.copy(cards = cards, isLoading = false) } }
                .onFailure { error -> _state.update { it.copy(error = error.message, isLoading = false) } }
        }
    }
}
```

### Use Cases (Domain Layer)
- One use case = one business action
- Return `Result<T>` for operations that can fail
- No Android framework dependencies
- Located in `domain/usecase/` package

### Repository Pattern (Data Layer)
- Interface in domain layer, implementation in data layer
- Coordinate between remote and local data sources
- Use mappers to convert DTOs ↔ Domain models

## Kotlin Code Style

- Descriptive variable names — no single letters except loop indices
- `val` over `var`; `when` over `if/else` chains for 3+ branches
- Avoid `!!`; prefer `?.let`, `?:`

### Function Signatures
- Single-line for < 250 characters; multi-line (one param per line) for 250+
- Prefer expression body (`= ...`) for short, non-complex functions returning a non-Unit value

### Naming Conventions
- Classes/Objects: PascalCase (`FlashcardViewModel`)
- Functions/Variables: camelCase (`getFlashcards`)
- Constants: UPPER_SNAKE_CASE
- Composable functions: PascalCase (`FlashcardScreen()`)
- ViewModel event handlers (UI → ViewModel callbacks): `onXxx` prefix, present tense (`onCategoriesRefresh`, `onCardSelect`)

### Sealed Classes for States
Use sealed classes for finite UI states (e.g. loading / content / error variants of a screen state).

For fallible operations, return `kotlin.Result<T>` and consume with `.onSuccess { ... }` / `.onFailure { ... }`. Do not define a project-local `Result` type — it would shadow the stdlib one.

## Jetpack Compose Guidelines

**Composable taxonomy:**
- `XxxScreen` — nav entry point; holds ViewModel, observes state, triggers navigation
- `XxxContent` — stateless UI; accepts state and callbacks, no ViewModel, fully previewable

Best practices:
- State hoisting: lift state to the lowest common ancestor
- Add `@Preview` for all major composables
- `contentDescription` on icon buttons and images
- `remember` for expensive calculations; `derivedStateOf` for computed state
- `LazyColumn` for lists; `key()` for stable item identity

## Dependency Injection (Hilt)

- `@HiltAndroidApp` on Application, `@AndroidEntryPoint` on Activities, `@HiltViewModel` on ViewModels

### Module Organization
- `NetworkModule`: Retrofit, OkHttp, API services
- `RepositoryModule`: Repository implementations
- `AppModule`: Application-level dependencies

### Scoping
- `@Singleton`: app-wide (database, API client)
- `@ViewModelScoped`: scoped to ViewModel lifecycle
- `@ActivityRetainedScoped`: survives configuration changes

## Asynchronous Programming

Dispatchers:
- `Dispatchers.IO` — network, file operations
- `Dispatchers.Default` — CPU-intensive work
- `Dispatchers.Main` — UI updates (default in Compose)

StateFlow / SharedFlow rules:
- `StateFlow` for UI state in ViewModels (single source of truth per screen)
- `SharedFlow` for transient one-time events such as snackbars and toasts
- **Navigation is the exception**: model navigation as a nullable destination field on the screen state, not as a `SharedFlow` event. Observe with `LaunchedEffect(state.navigationDestination)` and trigger the nav callback. See [docs/navigation-pattern.md](./docs/navigation-pattern.md) for the rationale (recomposition-safe, idempotent, no replay/buffer tuning).
- Handle errors with `.catch()` operator on upstream flows

## Data Layer Standards

- DTOs use `@Serializable`; named with `Dto` suffix (`FlashcardDto`)
- Mappers: `toDomain()` (DTO → Domain), `toDto()` (Domain → DTO)
- **Firestore string constants**: All Firestore collection names, document names, and field names used in queries must be extracted into `const val` constants in a `companion object` of the data source class. Never pass raw string literals to `.collection()`, `.document()`, `.orderBy()`, `.whereEqualTo()`, etc.

```kotlin
object FlashcardMapper {
    fun FlashcardDto.toDomain(): Flashcard = Flashcard(
        id = id,
        question = question,
        answer = answer,
        createdAt = Instant.parse(createdAt)
    )
}
```

## API Integration

Error handling pattern for repository methods:
```kotlin
override suspend fun submitResponse(cardId: String, audioFile: File): Result<EvaluationResult> {
    return try {
        Result.success(mapper.toDomain(api.submitResponse(cardId, audioPart)))
    } catch (e: IOException) {
        Result.failure(IOException("Network error: ${e.message}", e))
    } catch (e: HttpException) {
        Result.failure(e)
    }
}
```

## Testing Standards
See [TESTING.md](./TESTING.md) for full conventions: file/method naming, MainDispatcherRule usage, MockK + Kotest patterns, the "extract repeated literals" rule, and coverage targets.

## Security

- **NEVER** commit API keys, tokens, or secrets to Git
- Use `local.properties` for local secrets (gitignored)
- Use `BuildConfig` fields for compile-time config
- Use `EncryptedSharedPreferences` for auth tokens

## Project Documentation

- `SYSTEMDESIGN.md` — product design, screens, flows, Firestore schema
- `CONTEXT.md` — domain vocabulary glossary
- `TESTING.md` — testing conventions
- `docs/navigation-pattern.md` — state-based navigation pattern (why no SharedFlow)
