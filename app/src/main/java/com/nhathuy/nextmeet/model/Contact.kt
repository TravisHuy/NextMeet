package com.nhathuy.nextmeet.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Contact Entity đại diện cho  bảng danh bạ liên hệ trong rool
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
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("userId")]
)
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name= "address")
    val address:String,
    @ColumnInfo(name = "phone")
    val phone: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "notes")
    val notes:String,
    @ColumnInfo(name="latitude")
    val latitude: Double,
    @ColumnInfo(name="longitude")
    val longitude:Double,
    @ColumnInfo(name="is_favorite")
    val isFavorite:Boolean
)