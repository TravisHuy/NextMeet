package com.nhathuy.nextmeet.model

data class NavigationCancelAction(
    val shouldUpdateNavigationStarted: Boolean,
    val shouldUpdateStatus: Boolean,
    val newStatus: AppointmentStatus,
    val message: String
)