package com.nhathuy.customermanagementapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_history")
data class AlarmHistory (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val time : String,
    val date : String,
    val notes: String,
    val wasDisplayed: Boolean =false,
)