package com.rossomak.flashcards.core.ui.showcase

import android.content.Context
import android.content.Intent
import com.airbnb.android.showkase.ui.ShowkaseBrowserActivity

object Showcase {
    fun intentOrNull(context: Context): Intent? = ShowkaseBrowserActivity.getIntent(context, FlashcardsShowkaseRoot::class.java.name)
}
