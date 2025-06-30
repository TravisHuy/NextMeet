package com.nhathuy.nextmeet.utils

import com.nhathuy.nextmeet.model.Contact

interface AppointmentNavigationCallback {
    fun onNavigateToAppointmentWithContact(contact: Contact)
    fun onNavigateToAppointmentWithDashboard(filter: String)
}