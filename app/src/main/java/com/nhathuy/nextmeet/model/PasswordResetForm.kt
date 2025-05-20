package com.nhathuy.nextmeet.model

/**
 * Domain model for password reset from data
 */
data class PasswordResetForm(
    val phone: String = "",
    val newPassword: String = "",
    val confirmPassword: String = ""
)
