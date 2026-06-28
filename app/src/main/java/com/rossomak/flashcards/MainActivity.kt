package com.rossomak.flashcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.rossomak.flashcards.presentation.startup.AppStartViewModel
import com.rossomak.flashcards.presentation.startup.AppStartupState
import com.rossomak.flashcards.ui.navigation.FlashcardsNavGraph
import com.rossomak.flashcards.core.design.theme.FlashcardsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appStartViewModel: AppStartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            appStartViewModel.startupState.value is AppStartupState.Loading
        }
        splashScreen.setOnExitAnimationListener { splashView ->
            // Deliberately no animation to avoid jank - the goal is a seamless transition to the secondary splash screen in the SplashScreen composable
            splashView.remove()
        }

        enableEdgeToEdge()
        setContent {
            FlashcardsTheme(dynamicColor = false) {
                val navController = rememberNavController()
                FlashcardsNavGraph(
                    navController = navController,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
