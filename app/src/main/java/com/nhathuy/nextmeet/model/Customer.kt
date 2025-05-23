package com.nhathuy.nextmeet.model

import android.os.Parcel
import android.os.Parcelable

//import android.os.Parcelable
import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Customer entity đại diện cho bảng `customers` trong Room Database.
 *
 * - Có ràng buộc foreign key tới bảng `users` thông qua cột `userId`.
 * - Có chỉ mục (index) trên `userId` để tối ưu truy vấn và tránh cảnh báo KSP.
 * - Implements Parcelable để truyền dữ liệu giữa các Activity/Fragment.
 *
 * @property id ID tự tăng của khách hàng
 * @property userId ID của user sở hữu khách hàng này (foreign key tới User)
 * @property name Tên khách hàng
 * @property address Địa chỉ
 * @property phone Số điện thoại
 * @property email Email
 * @property group Nhóm khách hàng (ví dụ: VIP, Regular, etc.)
 * @property notes Ghi chú thêm
 * @property latitude Vĩ độ (vị trí khách hàng, có thể null)
 * @property longitude Kinh độ (vị trí khách hàng, có thể null)
 */
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

    // Constructor dùng để đọc dữ liệu từ Parcel
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        if (parcel.readByte().toInt() == 0) parcel.readDouble() else null,
        if (parcel.readByte().toInt() == 0) parcel.readDouble() else null
    ) {
    }

    // Ghi dữ liệu vào Parcel (để truyền đối tượng)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(userId)
        parcel.writeString(name)
        parcel.writeString(address)
        parcel.writeString(phone)
        parcel.writeString(email)
        parcel.writeString(group)
        parcel.writeString(notes)

        if (latitude != null) {
            parcel.writeByte(0)
            parcel.writeDouble(latitude)
        } else {
            parcel.writeByte(1)
        }

        if (longitude != null) {
            parcel.writeByte(0)
            parcel.writeDouble(longitude)
        } else {
            parcel.writeByte(1)
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
