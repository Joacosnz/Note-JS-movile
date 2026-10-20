package com.example.util

import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.util.Locale

/**
 * Obtiene el código/tag de idioma del teclado actualmente seleccionado.
 * Si el teclado no especifica idioma, realiza un fallback a Locale.getDefault().toLanguageTag() o "es-ES".
 */
fun getKeyboardLanguage(context: Context): String {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    val subtype = imm?.currentInputMethodSubtype

    val rawLanguageTag = subtype?.languageTag

    val keyboardLanguage = rawLanguageTag
        ?.takeIf { it.isNotBlank() }
        ?.replace('_', '-')

    return keyboardLanguage
        ?: Locale.getDefault().toLanguageTag().takeIf { it.isNotBlank() }
        ?: "es-ES"
}
