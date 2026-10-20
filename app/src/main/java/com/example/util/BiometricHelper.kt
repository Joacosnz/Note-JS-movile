package com.example.util

import android.content.Context
import com.example.ui.utils.BiometricAuth
import com.example.ui.utils.BiometricHelper as UiBiometricHelper

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricAuth.isBiometricOrCredentialAvailable(context)
    }

    fun authenticate(
        context: Context,
        onSuccess: () -> Unit,
        onError: () -> Unit = {}
    ) {
        UiBiometricHelper.authenticate(
            context = context,
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
