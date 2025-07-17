package com.nhathuy.nextmeet.model

/**
 * Thông tin timing để hiển thị
 */
data class TimingInfo(
    val appointmentTime: String,
    val timeUntilAppointment: String,
    val idealDepartureTime: String,
    val timeUntilDeparture: String,
    val travelTime: String,
    val isToday: Boolean,
    val isTomorrow: Boolean
)