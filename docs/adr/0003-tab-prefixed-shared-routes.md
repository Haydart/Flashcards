# Tab-prefixed route types for shared screens

`CategoryDetails` and `SubcategoryDetails` are reachable from both the Home and Study tabs. They use tab-prefixed route types (`HomeCategoryDetails` / `StudyCategoryDetails`, `HomeSubcategoryDetails` / `StudySubcategoryDetails`) rather than a single shared route type.

A single route type can only be registered once per `NavHost`. Registering the same type in two nested graphs within the same `NavHost` causes resolution ambiguity. Tab-prefixed types allow each tab's nested graph to own its copy, so back-stack save/restore works independently per tab — navigating deep in Home, switching to Study, then switching back restores the Home stack correctly. The composable function itself is shared (no UI duplication); only the route type differs. Deep links map cleanly to distinct URL patterns (`/home/category/{id}` vs `/study/category/{id}`).
