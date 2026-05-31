package com.rossomak.flashcards.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.rossomak.flashcards.presentation.home.HomeScreen
import com.rossomak.flashcards.presentation.settings.SettingsScreen
import com.rossomak.flashcards.presentation.study.StudyScreen
import com.rossomak.flashcards.ui.navigation.HomeCategoryDetails
import com.rossomak.flashcards.ui.navigation.HomeGraph
import com.rossomak.flashcards.ui.navigation.HomeRoot
import com.rossomak.flashcards.ui.navigation.HomeSubcategoryDetails
import com.rossomak.flashcards.ui.navigation.SettingsGraph
import com.rossomak.flashcards.ui.navigation.SettingsRoot
import com.rossomak.flashcards.ui.navigation.StudyGraph
import com.rossomak.flashcards.ui.navigation.StudyRoot

private val BottomBarBackground = Color.White
private val SelectedIndicatorColor = Color(0xFFEDE7FF)
private val SelectedItemColor = Color(0xFF6B2FA0)
private val UnselectedItemColor = Color(0xFF7E7E9A)

private data class TabItem(
    val label: String,
    val icon: ImageVector,
    val route: Any
)

private val tabs = listOf(
    TabItem("Home", Icons.Filled.Home, HomeGraph),
    TabItem("Study", Icons.AutoMirrored.Filled.MenuBook, StudyGraph),
    TabItem("Settings", Icons.Filled.Settings, SettingsGraph)
)

@Composable
fun MainScreen(
    onNavigateToLogin: () -> Unit
) {
    val tabNavController = rememberNavController()
    val navBackStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = BottomBarBackground
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                        onClick = {
                            tabNavController.navigate(tab.route) {
                                popUpTo(tabNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SelectedItemColor,
                            selectedTextColor = SelectedItemColor,
                            unselectedIconColor = UnselectedItemColor,
                            unselectedTextColor = UnselectedItemColor,
                            indicatorColor = SelectedIndicatorColor
                        ),
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                        label = { Text(text = tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = HomeGraph,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            navigation<HomeGraph>(startDestination = HomeRoot) {
                composable<HomeRoot> {
                    HomeScreen(
                        onNavigateToCategoryDetails = { id ->
                            tabNavController.navigate(HomeCategoryDetails(id))
                        },
                        onNavigateToSubcategoryDetails = { categoryId, subcategoryId ->
                            tabNavController.navigate(HomeSubcategoryDetails(categoryId, subcategoryId))
                        }
                    )
                }
                composable<HomeCategoryDetails> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Category Details - NYI")
                    }
                }
                composable<HomeSubcategoryDetails> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Subcategory Details - NYI")
                    }
                }
            }
            navigation<StudyGraph>(startDestination = StudyRoot) {
                composable<StudyRoot> {
                    StudyScreen()
                }
            }
            navigation<SettingsGraph>(startDestination = SettingsRoot) {
                composable<SettingsRoot> {
                    SettingsScreen(onNavigateToLogin = onNavigateToLogin)
                }
            }
        }
    }
}
