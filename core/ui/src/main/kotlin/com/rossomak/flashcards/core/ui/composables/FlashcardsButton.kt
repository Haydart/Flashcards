package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors
import com.rossomak.flashcards.core.ui.theme.cornerRadius
import com.rossomak.flashcards.core.ui.theme.spacing

@Composable
fun FlashcardsPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(
                brush = MaterialTheme.brandColors.ctaButtonGradient,
                shape = RoundedCornerShape(MaterialTheme.cornerRadius.large),
            ),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = MaterialTheme.spacing.xxsmall))
    }
}

@ShowkaseComposable(name = "Primary", group = "Buttons")
@Composable
fun FlashcardsPrimaryButtonShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsPrimaryButton(text = "Start studying", onClick = {})
        }
    }
}

@ShowkaseComposable(name = "Primary — disabled", group = "Buttons")
@Composable
fun FlashcardsPrimaryButtonDisabledShowcase() {
    FlashcardsTheme {
        Surface {
            FlashcardsPrimaryButton(text = "Start studying", onClick = {}, enabled = false)
        }
    }
}

@PreviewLightDark
@Composable
private fun FlashcardsPrimaryButtonPreview() {
    FlashcardsTheme {
        Surface {
            FlashcardsPrimaryButton(text = "Start studying", onClick = {})
        }
    }
}
