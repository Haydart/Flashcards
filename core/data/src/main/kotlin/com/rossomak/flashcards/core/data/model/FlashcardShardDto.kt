package com.rossomak.flashcards.core.data.model

/**
 * Firestore document shape for `subcategories/{id}/shards/{n}` (ADR-0037). Cards are packed into
 * byte-budgeted shard docs instead of one Firestore document per card, so this DTO is the
 * whole-shard payload — [FlashcardRemoteDataSource][com.rossomak.flashcards.core.data.source.FlashcardRemoteDataSource]
 * flattens every shard's [flashcards] values into one list before it reaches the repository layer.
 *
 * [flashcards] is a **map keyed by card id**, not a list — deliberately, so a future admin
 * curation-fix tool (see `users/{uid}/curationRequests/{cardId}`, ADR-0017) can patch one card's
 * field via a Firestore dot-path update (`update("flashcards.<cardId>.answer", fix)`) without
 * touching any other card in the same shard. Firestore has no equivalent partial update into a
 * single array element, which is why this isn't `List<FlashcardDto>`. Each [FlashcardDto] still
 * carries its own `id`, redundant with its map key — the seed pipeline asserts the two agree.
 */
data class FlashcardShardDto(
    val flashcards: Map<String, FlashcardDto> = emptyMap()
)
