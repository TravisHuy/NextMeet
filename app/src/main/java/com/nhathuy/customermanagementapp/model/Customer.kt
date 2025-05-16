package com.nhathuy.customermanagementapp.model

import android.os.Build
import android.os.Parcel
import android.os.Parcelable

//import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@RequiresApi(Build.VERSION_CODES.Q)
@Entity(
    tableName = "customers",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @NonNull val userId: Int,
    @NonNull val name: String,
    @NonNull val address: String,
    @NonNull val phone: String,
    @NonNull val email: String,
    val group: String,
    val notes: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble().takeIf { !parcel.readBoolean() },
        parcel.readDouble().takeIf { !parcel.readBoolean() }
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(userId)
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(phone)
        parcel.writeString(email)
        parcel.writeString(group)
        parcel.writeString(notes)

        latitude?.let {
            parcel.writeDouble(it)
            parcel.writeBoolean(false)
        } ?: run {
            parcel.writeDouble(0.0)
            parcel.writeBoolean(true)
        }

        longitude?.let {
            parcel.writeDouble(it)
            parcel.writeBoolean(false)
        } ?: run {
            parcel.writeDouble(0.0)
            parcel.writeBoolean(true)
        }
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Customer> {
        override fun createFromParcel(parcel: Parcel): Customer {
            return Customer(parcel)
        }

        override fun newArray(size: Int): Array<Customer?> {
            return arrayOfNulls(size)
        }
    }
}
