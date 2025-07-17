package com.nhathuy.nextmeet.model

data class NavigationRevertResult(
    val canRevert : Boolean,
    val shouldRevertStatus : Boolean ,
    val newStatus: AppointmentStatus,
    val reason : String
)
