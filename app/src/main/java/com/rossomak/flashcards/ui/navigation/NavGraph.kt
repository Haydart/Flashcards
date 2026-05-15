package com.rossomak.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.presentation.login.LoginRoute
import com.rossomak.flashcards.presentation.main.MainRoute
import com.rossomak.flashcards.presentation.splash.SplashRoute
import kotlinx.serialization.Serializable

@Serializable
object Splash

@Serializable
object Login

@Serializable
object Main

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
            SplashRoute(
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
            LoginRoute(
                onNavigateToMain = {
                    navController.navigate(Main) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Main> {
            MainRoute(
                onNavigateToLogin = {
                    navController.navigate(Login) {
                        popUpTo(Main) { inclusive = true }
                    }
                }
            )
        }
    }
}
