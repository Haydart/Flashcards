package com.rossomak.flashcards

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.rossomak.flashcards.presentation.startup.AppStartViewModel
import com.rossomak.flashcards.presentation.startup.AppStartupState
import com.rossomak.flashcards.ui.navigation.FlashcardsNavGraph
import com.rossomak.flashcards.ui.theme.FlashcardsTheme
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
            ObjectAnimator.ofFloat(splashView.view, View.ALPHA, 1f, 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = SPLASH_EXIT_FADE_MS
                doOnEnd { splashView.remove() }
                start()
            }
        }

        enableEdgeToEdge()
        setContent {
            FlashcardsTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FlashcardsNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private companion object {
        const val SPLASH_EXIT_FADE_MS = 250L
    }
}
