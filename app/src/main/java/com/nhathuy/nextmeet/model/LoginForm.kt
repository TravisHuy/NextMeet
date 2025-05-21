package com.nhathuy.nextmeet.model

/**
 * Domain model for login form data
 *
 */
data class LoginForm(
    val phone : String = "",
    val password: String = "",
    val remember : Boolean = false
)
