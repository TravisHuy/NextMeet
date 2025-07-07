package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.Note

sealed class SearchResultItem {
    data class AppointmentItem(val appointment: AppointmentPlus) : SearchResultItem()
    data class ContactItem(val contact: Contact) : SearchResultItem()
    data class NoteItem(val note: Note) : SearchResultItem()
    data class HeaderItem(val title: String, val count: Int) : SearchResultItem()
}