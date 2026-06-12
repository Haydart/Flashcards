package com.rossomak.flashcards.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.presentation.login.LoginScreen
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
                onNavigateToCategoryDetails = { categoryId ->
                    navController.navigate(CategoryDetails(categoryId))
                },
                onNavigateToSubcategoryDetails = { categoryId, subcategoryId ->
                    navController.navigate(SubcategoryDetails(categoryId, subcategoryId))
                }
            )
        }
        composable<CategoryDetails> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Category Details - NYI")
            }
        }
        composable<SubcategoryDetails> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Subcategory Details - NYI")
            }
        }
    }
}
