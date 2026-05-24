# Flat two-level taxonomy (Category → Subcategory → Flashcard)

The Flashcard taxonomy is strictly two levels: a Flashcard belongs to exactly one Subcategory, which belongs to exactly one Category. No sub-Subcategory level exists in the schema or UI.

We considered allowing 3+ levels of `parentId` nesting to handle high-volume Subcategories such as Compose (expected ~120 cards spread across themes like Composables, State Management, Modifiers). 
Rejected in favor of using user-facing **specific Tags** as filter chips on Subcategory Details. 
Tags are already in the schema, the change is reversible, and it avoids cascading complexity into navigation, breadcrumbs, search result shapes (which already has two result types), Recents card variants, and composite session scoping.
