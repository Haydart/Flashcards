package com.rossomak.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.feature.auth.AuthRoute
import com.rossomak.flashcards.feature.auth.LoginScreen
import com.rossomak.flashcards.feature.browse.CategoryDetailsRoute
import com.rossomak.flashcards.feature.browse.CategoryDetailsScreen
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsRoute
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsScreen
import com.rossomak.flashcards.feature.study.session.StudySessionScreen
import com.rossomak.flashcards.presentation.main.MainScreen
import com.rossomak.flashcards.presentation.splash.SplashScreen

@Composable
fun FlashcardsNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Splash,
        modifier = modifier
    ) {
        composable<Splash> {
            SplashScreen(
                onNavigateToMain = {
                    navController.navigate(Main) {
                        popUpTo(Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AuthRoute) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<AuthRoute> {
            LoginScreen(
                onNavigateToMain = {
                    navController.navigate(Main) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                }
            )
        }
        composable<Main> {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(AuthRoute) {
                        popUpTo(Main) { inclusive = true }
                    }
                },
                onNavigateToCategoryDetails = { categoryId, categoryName ->
                    navController.navigate(CategoryDetailsRoute(categoryId, categoryName))
                }
            )
        }
        composable<CategoryDetailsRoute> {
            CategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubcategoryDetails = { categoryId, categoryName, subcategoryId, subcategoryName ->
                    navController.navigate(
                        SubcategoryDetailsRoute(categoryId, categoryName, subcategoryId, subcategoryName)
                    )
                }
            )
        }
        composable<SubcategoryDetailsRoute> {
            SubcategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudySession = { subcategoryId, subcategoryName ->
                    navController.navigate(StudyRoute(subcategoryId, subcategoryName))
                }
            )
        }
        composable<StudyRoute> {
            StudySessionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
