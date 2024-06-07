package com.nhathuy.customermanagementapp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(tableName = "appointments",
        foreignKeys = [ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)])
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id:Int=0,
    val customerId:Int,
    val date:String,
    val time:String,
    val address:String,
    val notes:String,
)