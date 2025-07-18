package com.nhathuy.nextmeet

import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.utils.AppointmentStatusManager
import org.junit.Test

class AppointmentStatusTest {

    @Test
    fun testStatusTransitions(){
        val statusManager = AppointmentStatusManager()
        val now = System.currentTimeMillis()
        val appointment1 = AppointmentPlus(
            id = 1,
            userId = 1,
            contactId = 1,
            title = "Meeting",
            startDateTime = now + 60 * 60 * 1000,
            endDateTime = now + 90 * 60 * 1000,
            travelTimeMinutes = 30,
            status = AppointmentStatus.SCHEDULED
        )

        val newStatus1= statusManager.calculateNewStatus(appointment1,now)
        assert(newStatus1 == AppointmentStatus.PREPARING) { "Expected SCHEDULED but got $newStatus1" }

        //đã đến lúc prepare
        val prepareTime = appointment1.startDateTime - (30 + 30) * 60 * 1000 // 1 hour before start
        val newStatus2 = statusManager.calculateNewStatus(appointment1, prepareTime)
        assert(newStatus2 == AppointmentStatus.PREPARING)

        // Test case 3: Đã đến lúc đi
        val travelTime = appointment1.startDateTime - 30 * 60 * 1000 // 30 minutes before start
        val appointment2 = appointment1.copy(status = AppointmentStatus.PREPARING)
        val newStatus3 = statusManager.calculateNewStatus(appointment2, travelTime)
        assert(newStatus3 == AppointmentStatus.DELAYED)

        val newStatus4 = statusManager.calculateNewStatus(
            appointment2, travelTime, hasStartedNavigation = true
        )
        assert(newStatus4 == AppointmentStatus.TRAVELLING)

    }
}