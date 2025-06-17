package com.nhathuy.nextmeet.model

import com.google.android.gms.maps.model.LatLng
import com.nhathuy.nextmeet.R

data class NavigationStep(
    val instruction: String,
    val distance: String,
    val startLocation: LatLng,
    val endLocation: LatLng
) {
    fun getDirectionIcon(): Int {
        return when {
            instruction.contains("rẽ trái", ignoreCase = true) ||
                    instruction.contains("turn left", ignoreCase = true) -> R.drawable.ic_turn_left

            instruction.contains("rẽ phải", ignoreCase = true) ||
                    instruction.contains("turn right", ignoreCase = true) -> R.drawable.ic_turn_right

            instruction.contains("nhẹ trái", ignoreCase = true) ||
                    instruction.contains("slight left", ignoreCase = true) -> R.drawable.ic_turn_slight_left

            instruction.contains("nhẹ phải", ignoreCase = true) ||
                    instruction.contains("slight right", ignoreCase = true) -> R.drawable.ic_turn_slight_right

            instruction.contains("quay đầu", ignoreCase = true) ||
                    instruction.contains("u-turn", ignoreCase = true) -> R.drawable.ic_u_turn

            else -> R.drawable.ic_straight
        }
    }
}