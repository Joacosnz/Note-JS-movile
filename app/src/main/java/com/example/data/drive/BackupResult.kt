package com.example.data.drive

import android.content.Intent

sealed class BackupResult {
    object Success : BackupResult()
    data class AuthRequired(val intent: Intent) : BackupResult()
    data class Error(val exception: Exception, val message: String) : BackupResult()
}

sealed class RestoreResult {
    object Success : RestoreResult()
    object NotFound : RestoreResult()
    data class AuthRequired(val intent: Intent) : RestoreResult()
    data class Error(val exception: Exception, val message: String) : RestoreResult()
}

