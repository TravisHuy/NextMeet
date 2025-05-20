package com.nhathuy.nextmeet.model

/**
 * Data class representing validation result
 *
 * @property isValid whether validation passed
 * @property errorMessage error message if validation failed
 *
 *
 * @version 2.0
 * @author TravisHuy
 * @since 16.05.2025
 */
data class ValidationResult(
    val isValid : Boolean,
    val errorMessage: String = ""
)
