package com.nhathuy.nextmeet.model

import androidx.room.Embedded

/**
 * Data class kết hợp thông tin appointment với contact name
 * @since 19.07.2025
 * @author TravisHuy(Ho Nhat Huy)
 * @version 2.0
 */
data class AppointmentWithContact(
    @Embedded val appointment: AppointmentPlus,
    val contactName: String? = null
)