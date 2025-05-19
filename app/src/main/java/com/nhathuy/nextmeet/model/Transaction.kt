package com.nhathuy.nextmeet.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity đại diện cho một giao dịch phát sinh trong hệ thống.
 *
 * Giao dịch có thể liên kết với người dùng, khách hàng, và cuộc hẹn.
 * Dùng để lưu thông tin về sản phẩm/dịch vụ đã cung cấp, số lượng, giá và ngày giao dịch.
 *
 * @property id Khóa chính tự động tăng
 * @property userId ID người dùng thực hiện giao dịch
 * @property customerId ID khách hàng liên quan đến giao dịch
 * @property appointmentId ID cuộc hẹn liên quan (có thể null)
 * @property productOrService Tên sản phẩm hoặc dịch vụ được giao dịch
 * @property quantity Số lượng sản phẩm/dịch vụ
 * @property price Giá đơn vị
 * @property date Ngày giao dịch (định dạng: yyyy-MM-dd)
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 16.05.2025
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Appointment::class, parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("customerId"), Index("appointmentId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val customerId: Int,
    val appointmentId: Int? = null,
    val productOrService: String,
    val quantity: Int,
    val price: Double,
    val date: String
)
