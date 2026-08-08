package com.rossomak.flashcards.presentation.main

import androidx.compose.ui.graphics.vector.ImageVector

/** [selectedIcon] (filled) shows for the active tab, [unselectedIcon] (outlined) for the rest. */
internal data class TabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Any,
)
