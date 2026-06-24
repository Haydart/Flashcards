package com.rossomak.flashcards.ui.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

@Composable
fun String.withInlineCode(): AnnotatedString {
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val codeColor = MaterialTheme.colorScheme.onSurfaceVariant
    return remember(this, codeBackground) {
        buildAnnotatedString {
            val source = this@withInlineCode
            var cursor = 0
            while (cursor < source.length) {
                val open = source.indexOf('`', cursor)
                if (open == -1) {
                    append(source.substring(cursor))
                    break
                }
                append(source.substring(cursor, open))
                val close = source.indexOf('`', open + 1)
                if (close == -1) {
                    append(source.substring(open))
                    break
                }
                val codeText = source.substring(open + 1, close)
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        background = codeBackground,
                        color = codeColor,
                    )
                )
                append(" $codeText ")
                pop()
                cursor = close + 1
            }
        }
    }
}
