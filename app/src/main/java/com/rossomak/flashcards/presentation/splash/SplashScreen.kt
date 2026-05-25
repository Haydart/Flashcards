package com.rossomak.flashcards.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.rossomak.flashcards.R

private val SplashGradientStart = Color(0xFF7B2FBE)
private val SplashGradientEnd = Color(0xFF2979FF)

private val splashGradient = Brush.linearGradient(
    colors = listOf(SplashGradientStart, SplashGradientEnd),
    start = Offset.Zero,
    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
)

private val LogoWidth = 280.dp

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigationDestination) {
        when (state.navigationDestination) {
            SplashDestination.Main -> onNavigateToMain()
            SplashDestination.Login -> onNavigateToLogin()
            null -> Unit
        }
    }

    SplashContent(onAnimationCompleted = viewModel::onAnimationCompleted)
}

@Composable
fun SplashContent(
    onAnimationCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.flashcards_lottie_splash))
    val lottieState = animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        isPlaying = composition != null
    )

    LaunchedEffect(lottieState.isAtEnd) {
        if (lottieState.isAtEnd && composition != null) {
            onAnimationCompleted()
        }
    }

    val gradientSlide = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        gradientSlide.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = GRADIENT_SLIDE_MS, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBlue),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = (1f - gradientSlide.value) * size.height }
                .background(splashGradient)
        )
        LottieAnimation(
            composition = composition,
            progress = { lottieState.progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 800)
@Composable
private fun SplashContentPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(splashGradient),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.flashcards_white),
            contentDescription = null,
            modifier = Modifier.width(LogoWidth)
        )
    }
}
