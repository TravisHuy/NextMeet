package com.nhathuy.nextmeet.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Contact Entity đại diện cho  bảng danh bạ liên hệ trong room
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
 * @property notes Ghi chú thêm
 * @property latitude Vĩ độ (vị trí khách hàng, có thể null)
 * @property longitude Kinh độ (vị trí khách hàng, có thể null)
 * @property isFavorite Xem khách hàng yêu thích của người dùng
 */
@Entity(
    tableName = "contacts",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["user_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("user_id"),
        Index("name"),
        Index("user_id", "phone", unique = true)]
)
@Parcelize
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "user_id")
    val userId: Int,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "address")
    val address: String = "",
    @ColumnInfo(name = "phone")
    val phone: String = "",
    @ColumnInfo(name = "email")
    val email: String = "",
    @ColumnInfo(name = "role")
    val role: String ="",
    @ColumnInfo(name = "notes")
    val notes: String = "",
    @ColumnInfo(name = "latitude")
    val latitude: Double? = null,
    @ColumnInfo(name = "longitude")
    val longitude: Double? = null,
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updateAt: Long = System.currentTimeMillis()
) : Parcelable

