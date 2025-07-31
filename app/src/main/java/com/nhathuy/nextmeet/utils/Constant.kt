package com.nhathuy.nextmeet.utils

import android.content.Context
import com.nhathuy.nextmeet.R

object Constant {
    const val EXTRA_NOTE_TYPE = "extra_note_type"
    const val EXTRA_NOTE_ID = "note_id"
    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    const val REQUEST_CODE_TURN_BY_TURN = 1002
    const val EXTRA_APPOINTMENT_ID= "extra_appointment_id"
    const val EXTRA_USER_ID = "user_id"

    const val FILTER_TODAY = "Today"
    const val FILTER_PINNED = "Pinned"
    const val FILTER_WEEK = "This Week"
    const val FILTER_UPCOMING = "Upcoming"
    const val FILTER_FAVORITE = "Favorite"
    const val FILTER_HAVE_PHONE = "Have Phone Number"
    const val FILTER_HAVE_ADDRESS = "Have Address"
    const val FILTER_HAVE_EMAIL = "Have Email"
    const val FILTER_RECENT = "Recent"
    const val FILTER_REMINDER = "Reminder"
    const val FILTER_CHECKLIST = "Checklist"

    fun getFilterKeyFromText(text: String, context: Context): String? {
        return when (text) {
            context.getString(R.string.today) -> FILTER_TODAY
            context.getString(R.string.pinned) -> FILTER_PINNED
            context.getString(R.string.weekend) -> FILTER_WEEK
            context.getString(R.string.upcoming) -> FILTER_UPCOMING
            context.getString(R.string.favorite) -> FILTER_FAVORITE
            context.getString(R.string.have_phone_number) -> FILTER_HAVE_PHONE
            context.getString(R.string.have_address) -> FILTER_HAVE_ADDRESS
            context.getString(R.string.have_email) -> FILTER_HAVE_EMAIL
            context.getString(R.string.recent) -> FILTER_RECENT
            context.getString(R.string.reminder) -> FILTER_REMINDER
            context.getString(R.string.checklist) -> FILTER_CHECKLIST

            "Today" -> FILTER_TODAY
            "Upcoming" -> FILTER_UPCOMING
            "Pinned" -> FILTER_PINNED
            "This Week" -> FILTER_WEEK
            "Favorite" -> FILTER_FAVORITE
            "Have Phone Number" -> FILTER_HAVE_PHONE
            "Have Email" -> FILTER_HAVE_EMAIL
            "Have Address" -> FILTER_HAVE_ADDRESS
            "Reminder" -> FILTER_REMINDER
            "Checklist" -> FILTER_CHECKLIST
            "Recent" -> FILTER_RECENT

            else -> text
        }
    }

    fun getLocalizedTextFromFilterKey(filterKey: String, context: Context): String {
        return when (filterKey) {
            FILTER_TODAY -> context.getString(R.string.today)
            FILTER_UPCOMING -> context.getString(R.string.upcoming)
            FILTER_PINNED -> context.getString(R.string.pinned)
            FILTER_WEEK -> context.getString(R.string.weekend)
            FILTER_FAVORITE -> context.getString(R.string.favorite)
            FILTER_HAVE_PHONE -> context.getString(R.string.have_phone_number)
            FILTER_HAVE_EMAIL -> context.getString(R.string.have_email)
            FILTER_HAVE_ADDRESS -> context.getString(R.string.have_address)
            FILTER_REMINDER -> context.getString(R.string.reminder)
            FILTER_CHECKLIST -> context.getString(R.string.check_list)
            FILTER_RECENT -> context.getString(R.string.recent)
            else -> filterKey
        }
    }
}