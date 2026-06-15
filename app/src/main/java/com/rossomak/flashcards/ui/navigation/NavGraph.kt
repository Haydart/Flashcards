package com.rossomak.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.presentation.categorydetails.CategoryDetailsScreen
import com.rossomak.flashcards.presentation.login.LoginScreen
import com.rossomak.flashcards.presentation.main.MainScreen
import com.rossomak.flashcards.presentation.splash.SplashScreen
import com.rossomak.flashcards.presentation.subcategorydetails.SubcategoryDetailsScreen

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
                    navController.navigate(Login) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<Login> {
            LoginScreen(
                onNavigateToMain = {
                    navController.navigate(Main) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Main> {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(Main) { inclusive = true }
                    }
                },
                onNavigateToCategoryDetails = { categoryId, categoryName ->
                    navController.navigate(CategoryDetails(categoryId, categoryName))
                }
            )
        }
        composable<CategoryDetails> {
            CategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubcategoryDetails = { categoryId, categoryName, subcategoryId, subcategoryName ->
                    navController.navigate(
                        SubcategoryDetails(categoryId, categoryName, subcategoryId, subcategoryName)
                    )
                }
            )
        }
        composable<SubcategoryDetails> {
            SubcategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
