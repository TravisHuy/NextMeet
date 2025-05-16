package com.nhathuy.customermanagementapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại điện cho một cuộc hẹn giữa người dùng và khách hàng.
 *
 * Mỗi cuộc hẹn lưu thông tin chi tiết về địa điểm, thời gian, ghi chu, thông tin định vị, phương thức di chuyển và trạng thái
 * hiên tại của cuộc hẹn.
 * Cuộc hẹn được liên kết với mot khac hang qua khóa ngoại cutomerId.
 *
 * @property id Khóa chính tự động tăng
 * @property customerId ID của khách hàng liên kết với cuộc hẹn
 * @property date Ngày diễn ra cuộc hẹn (định dạng: "yyyy-MM-dd")
 * @property time Giờ diễn ra cuộc hẹn (định dạng: "HH:mm")
 * @property address Địa chỉ của khách hàng (điểm đến)
 * @property notes Ghi chú bổ sung cho cuộc hẹn
 * @property latitude Vĩ độ của địa chỉ khách hàng (có thể null)
 * @property longitude Kinh độ của địa chỉ khách hàng (có thể null)
 * @property startingAddress Địa chỉ xuất phát từ phía người dùng (có thể null)
 * @property startLatitudeAddress Vĩ độ điểm xuất phát (có thể null)
 * @property longitudeAddress Kinh độ điểm xuất phát (có thể null)
 * @property estimatedTravelTime Thời gian di chuyển ước tính (VD: "15 phút")
 * @property travelDistance Quãng đường di chuyển ước tính (VD: "5.2 km")
 * @property travelMode Phương thức di chuyển: DRIVING, WALKING, BICYCLING, TRANSIT
 * @property status Trạng thái của cuộc hẹn: SCHEDULED, COMPLETED, CANCELLED
 * @property reminderTime Thời gian nhắc nhở trước cuộc hẹn (timestamp, milliseconds)
 *
 * @author TravisHuy(Hồ Nhật Huy)
 * @version 2.0
 * @since 16.5.2025
 */
@RequiresApi(Build.VERSION_CODES.Q)
@Entity(
    tableName = "appointments",
    foreignKeys = [ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["customerId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("customerId")]
)
data class Appointment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val date: String,
    val time: String,
    val address: String,
    val notes: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val startingAddress: String? = null,
    val startLatitudeAddress: String? = null,
    val longitudeAddress: String? = null,
    val estimatedTravelTime: String? = null,
    val travelDistance: String? = null,
    val travelMode: String? =null,
    val status : String? = null,
    val reminderTime : Long? = 0
)