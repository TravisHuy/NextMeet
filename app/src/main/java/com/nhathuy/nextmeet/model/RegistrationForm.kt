package com.nhathuy.nextmeet.model

/**
 * Domain model registration from data
 */
data class RegistrationForm(
    val name:String = "",
    val phone:String = "",
    val email:String = "",
    val password: String = "",
    val address : String = "",
    val latitude : Double? = null,
    val longitude: Double? = null
)
