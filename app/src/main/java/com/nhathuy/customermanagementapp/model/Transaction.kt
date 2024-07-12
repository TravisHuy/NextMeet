package com.nhathuy.customermanagementapp.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "transactions",
        foreignKeys =[ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"], onDelete = ForeignKey.CASCADE)])
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id:Int=0,
    val userId:Int,
    val customerId:Int,
    val productOrService:String,
    val quantity: Int,
    val price:Double,
    val date:String,
)
