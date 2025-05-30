package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.NoteType

/**
 * Filter state
 *
 * @version 2.0
 * @since 29/05/2025
 * @author TravisHuy
 */
data class FilterState(
    val searchQuery : String = "",
    val selectedNoteType: NoteType? = null,
    val showPinnedOnly : Boolean = false,
    val showSharedOnly: Boolean = false
)
