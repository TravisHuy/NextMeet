package com.nhathuy.nextmeet.model

/**
 * Data class cho thống kê lịch sử
 */
data class HistoryStatistics(
    val totalAppointments: Int,
    val completedCount: Int,
    val cancelledCount: Int,
    val missedCount: Int,
    val completionRate: Float
)
