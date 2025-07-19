package com.nhathuy.nextmeet.model

/**
 * Data class để nhận kết quả thống kê từ query
 * @version 2.0
 * @author TravisHuy (Ho Nhat Huy)
 * @since 18/07/2025
 */
data class HistoryCounts(
    val completed: Int,
    val cancelled: Int,
    val missed: Int,
    val total: Int
)