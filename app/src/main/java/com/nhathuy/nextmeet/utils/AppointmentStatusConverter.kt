package com.nhathuy.nextmeet.utils

import androidx.room.TypeConverter
import com.nhathuy.nextmeet.model.AppointmentStatus

/**
 * Typeconverter cho appointment enum
 * chuyển đổi giữa enum va string trong room database.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 12/07/2025
 */
class AppointmentStatusConverter {

    @TypeConverter
    fun fromAppointmentStatus(status: AppointmentStatus): String {
        return status.name
    }
    @TypeConverter
    fun toAppointmentStatus(status:String): AppointmentStatus {
        return try {
            AppointmentStatus.valueOf(status)
        }
        catch (e: IllegalArgumentException) {
            AppointmentStatus.SCHEDULED
        }
    }

}