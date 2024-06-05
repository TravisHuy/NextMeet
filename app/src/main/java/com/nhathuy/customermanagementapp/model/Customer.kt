package com.nhathuy.customermanagementapp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    val name:String,
    val address:String,
    val phone:String,
    val email:String,
    val group:String,
    val notes:String,
)
