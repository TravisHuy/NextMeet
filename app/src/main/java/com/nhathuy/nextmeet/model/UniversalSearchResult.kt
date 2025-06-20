package com.nhathuy.nextmeet.model

data class UniversalSearchResult(
    val contacts: List<Contact> = emptyList(),
    val appointments: List<AppointmentPlus> = emptyList(),
    val notes: List<Note> = emptyList(),
    val totalCount: Int = 0
) {
    fun isEmpty() = totalCount == 0
    fun isNotEmpty() = totalCount > 0

    /**
     * Get results by type
     */
    fun getResultsByType(searchType: SearchType): List<Any> {
        return when (searchType) {
            SearchType.CONTACT -> contacts
            SearchType.APPOINTMENT -> appointments
            SearchType.NOTE -> notes
            SearchType.ALL -> contacts + appointments + notes
        }
    }

    /**
     * Get count by type
     */
    fun getCountByType(searchType: SearchType): Int {
        return when (searchType) {
            SearchType.CONTACT -> contacts.size
            SearchType.APPOINTMENT -> appointments.size
            SearchType.NOTE -> notes.size
            SearchType.ALL -> totalCount
        }
    }
}