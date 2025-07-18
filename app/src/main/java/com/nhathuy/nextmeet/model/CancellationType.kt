package com.nhathuy.nextmeet.model

enum class CancellationType {
    IMMEDIATE_CANCEL,      // Hủy ngay lập tức, có thể revert hoàn toàn
    GRACE_PERIOD_CANCEL,   // Hủy trong grace period, có thể revert với warning
    AFTER_MOVEMENT,        // Đã di chuyển, không revert status
    LATE_CANCEL           // Hủy muộn, cần tính toán lại status
}