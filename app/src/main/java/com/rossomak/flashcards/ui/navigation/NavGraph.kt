package com.rossomak.flashcards.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rossomak.flashcards.feature.auth.AuthRoute
import com.rossomak.flashcards.feature.auth.LoginScreen
import com.rossomak.flashcards.feature.browse.CategoryDetailsRoute
import com.rossomak.flashcards.feature.browse.CategoryDetailsScreen
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsRoute
import com.rossomak.flashcards.feature.browse.SubcategoryDetailsScreen
import com.rossomak.flashcards.feature.onboarding.OnboardingRoute
import com.rossomak.flashcards.feature.onboarding.OnboardingScreen
import com.rossomak.flashcards.feature.study.PreviewStudySessionRoute
import com.rossomak.flashcards.feature.study.StudySessionRoute
import com.rossomak.flashcards.feature.study.preview.PreviewStudySessionScreen
import com.rossomak.flashcards.feature.study.session.StudySessionScreen
import com.rossomak.flashcards.presentation.main.MainScreen
import com.rossomak.flashcards.presentation.splash.SplashScreen
import kotlinx.serialization.Serializable

@Serializable object Splash

@Serializable object Main

@Serializable object HomeGraph

@Serializable object StudyGraph

@Serializable object SettingsGraph

@Serializable object HomeRoot

@Serializable object StudyRoot

@Serializable object SettingsRoot

/**
 * Reached from three places — Category Details, and both a search result's row and its play button
 * on the Study tab — so the route construction lives here once rather than in each `composable`
 * block.
 */
private fun NavHostController.navigateToSubcategoryDetails(
    categoryId: String,
    categoryName: String,
    subcategoryId: String,
    subcategoryName: String,
) {
    navigate(SubcategoryDetailsRoute(categoryId, categoryName, subcategoryId, subcategoryName))
}

/**
 * Study Creation takes a list of subcategories; every entry point that starts from a single
 * Subcategory wraps it in a one-element list here.
 */
private fun NavHostController.navigateToPreviewStudySession(
    categoryId: String,
    categoryName: String,
    subcategoryId: String,
    subcategoryName: String,
) {
    navigate(
        PreviewStudySessionRoute(
            categoryId = categoryId,
            categoryName = categoryName,
            subcategoryIds = listOf(subcategoryId),
            subcategoryNames = listOf(subcategoryName),
        )
    )
}

/**
 * Splash, Login and Onboarding — the three screens a user passes through before reaching [Main].
 * Each pops itself off the back stack on the way out, so [Main] is never reachable "back" into a
 * launch screen. Extracted from [FlashcardsNavGraph] to keep that function readable as the flow
 * grew a third pre-Main step.
 */
private fun NavGraphBuilder.launchDestinations(navController: NavHostController) {
    composable<Splash> {
        SplashScreen(
            onNavigateToMain = {
                navController.navigate(Main) {
                    popUpTo(Splash) { inclusive = true }
                }
            },
            onNavigateToOnboarding = {
                navController.navigate(OnboardingRoute) {
                    popUpTo(Splash) { inclusive = true }
                }
            },
            onNavigateToLogin = {
                navController.navigate(AuthRoute) {
                    popUpTo(Splash) { inclusive = true }
                }
            },
        )
    }
    composable<AuthRoute> {
        LoginScreen(
            onNavigateToMain = {
                navController.navigate(Main) {
                    popUpTo(AuthRoute) { inclusive = true }
                }
            },
            onNavigateToOnboarding = {
                navController.navigate(OnboardingRoute) {
                    popUpTo(AuthRoute) { inclusive = true }
                }
            },
        )
    }
    composable<OnboardingRoute> {
        OnboardingScreen(
            onNavigateToMain = {
                navController.navigate(Main) {
                    popUpTo(OnboardingRoute) { inclusive = true }
                }
            },
        )
    }
}

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
        launchDestinations(navController)
        composable<Main> {
            MainScreen(
                onNavigateToLogin = {
                    navController.navigate(AuthRoute) {
                        popUpTo(Main) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(OnboardingRoute) {
                        popUpTo(Main) { inclusive = true }
                    }
                },
                onNavigateToCategoryDetails = { categoryId, categoryName ->
                    navController.navigate(CategoryDetailsRoute(categoryId, categoryName))
                },
                onNavigateToSubcategoryDetails = navController::navigateToSubcategoryDetails,
                onNavigateToPreviewStudySession = navController::navigateToPreviewStudySession,
            )
        }
        composable<CategoryDetailsRoute> {
            CategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubcategoryDetails = navController::navigateToSubcategoryDetails,
            )
        }
        composable<SubcategoryDetailsRoute> {
            SubcategoryDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreviewStudySession = navController::navigateToPreviewStudySession,
            )
        }
        composable<PreviewStudySessionRoute> {
            PreviewStudySessionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudySession = { route -> navController.navigate(route) }
            )
        }
        composable<StudySessionRoute> {
            StudySessionScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
