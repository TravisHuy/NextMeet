package com.nhathuy.customermanagementapp.model

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "customers",
    foreignKeys = [ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    @NonNull val userId:Int,
    @NonNull val name:String,
    @NonNull val address:String,
    @NonNull val phone:String,
    @NonNull val email:String,
    val group:String,
    val notes:String,
)
