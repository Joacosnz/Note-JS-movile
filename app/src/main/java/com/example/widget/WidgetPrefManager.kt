package com.example.widget

import android.content.Context

object WidgetPrefManager {
    private const val PREF_NAME = "single_note_widget_prefs"
    private const val KEY_PREFIX = "widget_note_id_"

    fun saveSelectedNoteId(context: Context, appWidgetId: Int, noteId: Long) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong("$KEY_PREFIX$appWidgetId", noteId)
            .apply()
    }

    fun getSelectedNoteId(context: Context, appWidgetId: Int): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong("$KEY_PREFIX$appWidgetId", 0L)
    }

    fun removeWidgetNoteId(context: Context, appWidgetId: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove("$KEY_PREFIX$appWidgetId")
            .apply()
    }
}
