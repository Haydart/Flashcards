package com.rossomak.flashcards.core.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rossomak.flashcards.core.ui.theme.FlashcardsTheme
import com.rossomak.flashcards.core.ui.theme.brandColors

@Composable
fun FlashcardsPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .background(
                brush = MaterialTheme.brandColors.ctaButtonGradient,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Preview(name = "FlashcardsPrimaryButton - Light")
@Composable
fun FlashcardsPrimaryButtonPreview() {
    FlashcardsTheme(darkTheme = false) {
        FlashcardsPrimaryButton(text = "Start studying", onClick = {})
    }
}

@Preview(name = "FlashcardsPrimaryButton - Dark")
@Composable
fun FlashcardsPrimaryButtonDarkPreview() {
    FlashcardsTheme(darkTheme = true) {
        FlashcardsPrimaryButton(text = "Start studying", onClick = {})
    }
}
