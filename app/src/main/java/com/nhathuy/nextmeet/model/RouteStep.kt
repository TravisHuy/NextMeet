package com.nhathuy.nextmeet.model

/**
 * Đại diện cho một bước (step) trong tuyến đường dẫn đường.
 *
 * Lớp data này đóng gói tất cả thông tin cần thiết cho một thao tác di chuyển
 * hoặc chỉ dẫn cụ thể trong một tuyến đường lớn hơn.
 *
 * @property instruction Hướng dẫn hiển thị cho người dùng (ví dụ: "Rẽ trái vào đường Main St").
 * @property distance Quãng đường cần đi cho bước này, thường là chuỗi định dạng (ví dụ: "0.5 mi", "200 m").
 * @property duration Thời gian ước tính để hoàn thành bước này, thường là chuỗi định dạng (ví dụ: "2 phút", "30 giây").
 * @property iconResId ID tài nguyên của icon đại diện cho thao tác di chuyển (ví dụ: mũi tên rẽ trái).
 *                     Nên tương ứng với một tài nguyên drawable.
 * @property maneuver (Tùy chọn) Chuỗi mô tả thao tác di chuyển ở dạng máy đọc được
 *                     (ví dụ: "turn-left", "roundabout-exit-3"). Mặc định là chuỗi rỗng nếu không cung cấp.
 *
 * @version 2.0
 * @author TravisHuy(Ho Nhat Huy)
 * @since 12.06.2025
 */
data class RouteStep(
    val instruction: String,
    val distance: String,
    val duration: String,
    val iconResId: Int,
    val maneuver: String = ""
)