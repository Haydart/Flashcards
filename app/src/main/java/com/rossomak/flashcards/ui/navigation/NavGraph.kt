package com.rossomak.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.presentation.main.MainScreen
import com.rossomak.flashcards.presentation.main.MainViewModel
import com.rossomak.flashcards.presentation.splash.SplashScreen
import com.rossomak.flashcards.presentation.splash.SplashViewModel
import kotlinx.serialization.Serializable

@Serializable
object Splash

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
            val viewModel: SplashViewModel = hiltViewModel()
            SplashScreen(
                viewModel = viewModel,
                onSplashFinished = {
                    navController.navigate(Main) {
                        popUpTo(Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<Main> {
            val viewModel: MainViewModel = hiltViewModel()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            MainScreen(state = state)
        }
    }
}
