package com.nhathuy.nextmeet.model

/**
 * Kết quả kiểm tra navigation
 */
data class NavigationCheckResult(
    val canStart: Boolean,
    val reason: String,
    val buttonText: String,
    val showSuccess: Boolean = false,
    val showWarning: Boolean = false,
    val showInfo: Boolean = false
)
