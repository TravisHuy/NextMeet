package com.nhathuy.nextmeet.repository

import android.content.Context
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.SearchHistoryDao
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.SearchHistory
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.utils.Constant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val context: Context,
    private val searchHistoryDao: SearchHistoryDao,
    private val contactDao: ContactDao,
    private val appointmentDao: AppointmentPlusDao,
    private val noteDao: NoteDao
) {
    /**
     * thực hiện tìm kiếm chung
     */
    suspend fun performUniversalSearch(userId: Int, query: String): UniversalSearchResult {
        return coroutineScope {
            val contactsDeferred = async { contactDao.searchContacts(userId, query).first() }
            val appointmentsDeferred =
                async { appointmentDao.searchAppointments(userId, query).first() }
            val notesDeferred = async { noteDao.searchNotePlus(userId, query).first() }

            val contacts = contactsDeferred.await()
            val appointments = appointmentsDeferred.await()
            val notes = notesDeferred.await()

            UniversalSearchResult(
                contacts = contacts,
                appointments = appointments,
                notes = notes,
                totalCount = contacts.size + appointments.size + notes.size
            )
        }
    }

    /**
     * Lấy tất cả tìm kiếm với note
     */
    suspend fun getAllNotesByUser(userId: Int): Flow<List<Note>> =
        noteDao.getAllNotesByUser(userId)

    /**
     * Lấy tất cả tìm kiếm với contact
     */
    fun getAllContactsByUser(userId: Int): Flow<List<Contact>> =
        contactDao.getAllContactsByUser(userId)


    /**
     * Lấy tất cả tìm kiếm với contact
     */
    fun getAllAppointmentsByUser(userId: Int): Flow<List<AppointmentPlus>> =
        appointmentDao.getAllAppointmentsByUser(userId)
    /**
     * Tìm kiếm liên hệ
     */
    fun searchContacts(userId: Int, query: String): Flow<List<Contact>> =
        contactDao.searchContacts(userId, query)

    /**
     * Tìm kiếm cuộc hẹn
     */
    suspend fun searchAppointments(userId: Int, query: String): Flow<List<AppointmentPlus>> =
        appointmentDao.searchAppointments(userId, query)

    /**
     * Tìm kiếm ghi chú
     */
    suspend fun searchNotes(userId: Int, query: String): Flow<List<Note>> =
        noteDao.searchNotePlus(userId, query)

    /**
     * tạo gợi ý tìm kiếm
     */
    /**
     * tạo gợi ý tìm kiếm
     */
    suspend fun generateSearchSuggestions(
        userId: Int,
        query: String,
        searchType: SearchType
    ): Flow<List<SearchSuggestion>> = flow {

        val suggestions = mutableListOf<SearchSuggestion>()

        //1.history suggestions
        if (query.isEmpty()) {
            if (searchType == SearchType.ALL) {
                // Lấy history từ tất cả các loại search
                val allHistory = searchHistoryDao.getAllSearchHistory(userId, 15)
                allHistory.forEach { historyItem ->
                    suggestions.add(
                        SearchSuggestion(
                            text = historyItem.searchText,
                            type = SearchSuggestionType.HISTORY,
                            searchType = historyItem.searchType, // Giữ nguyên searchType gốc
                            icon = getHistoryIcon(historyItem.searchType),
                            subtitle = context.getString(R.string.search_recent) + " - ${getSearchTypeLabel(historyItem.searchType)}"
                        )
                    )
                }
            } else {
                val history = searchHistoryDao.getSearchHistory(userId, searchType)
                history.forEach { historyItem ->
                    suggestions.add(
                        SearchSuggestion(
                            text = historyItem.searchText,
                            type = SearchSuggestionType.HISTORY,
                            searchType = searchType,
                            icon = getHistoryIcon(searchType),
                            subtitle = context.getString(R.string.search_recent)
                        )
                    )
                }
            }
        } else {
            //filtered history
            if (searchType == SearchType.ALL) {
                // Tìm kiếm trong tất cả history
                val allHistory = searchHistoryDao.getAllSearchHistory(userId, 20)
                allHistory.filter {
                    it.searchText.contains(query, ignoreCase = true)
                }.take(5).forEach { historyItem ->
                    suggestions.add(
                        SearchSuggestion(
                            text = historyItem.searchText,
                            type = SearchSuggestionType.HISTORY,
                            searchType = historyItem.searchType,
                            icon = getHistoryIcon(historyItem.searchType),
                            subtitle = context.getString(R.string.search_history) + " - ${getSearchTypeLabel(historyItem.searchType)}"
                        )
                    )
                }
            } else {
                val history = searchHistoryDao.getSearchHistory(userId, searchType, 10)
                history.filter {
                    it.searchText.contains(query, ignoreCase = true)
                }.take(3).forEach { historyItem ->
                    suggestions.add(
                        SearchSuggestion(
                            text = historyItem.searchText,
                            type = SearchSuggestionType.HISTORY,
                            searchType = searchType,
                            icon = getHistoryIcon(searchType),
                            subtitle = context.getString(R.string.search_history)
                        )
                    )
                }
            }
        }

        //2. autocomplete suggestions
        if (query.length >= 2) {
            val autocompleteSuggestions =
                generateAutocompleteSuggestions(userId, query, searchType).first()
            suggestions.addAll(autocompleteSuggestions)
        }

        // 3. Quick filters
        if (query.isEmpty()) {
            suggestions.addAll(getQuickFilterSuggestions(searchType))
        }

        //4.Trending searchers
        if (query.isEmpty()) {
            if (searchType == SearchType.ALL) {
                // Lấy trending từ tất cả các loại
                val allTrending = mutableListOf<SearchSuggestion>()

                // Lấy trending từ từng loại
                val contactTrending = searchHistoryDao.getTrendingSearches(userId, SearchType.CONTACT, 2)
                val appointmentTrending = searchHistoryDao.getTrendingSearches(userId, SearchType.APPOINTMENT, 2)
                val noteTrending = searchHistoryDao.getTrendingSearches(userId, SearchType.NOTE, 2)

                contactTrending.forEach { trend ->
                    allTrending.add(
                        SearchSuggestion(
                            text = trend.search_text,
                            type = SearchSuggestionType.TRENDING,
                            searchType = SearchType.CONTACT,
                            icon = getHistoryIcon(SearchType.CONTACT),
                            subtitle = context.getString(R.string.search_trending, trend.count) + " - ${getSearchTypeLabel(SearchType.CONTACT)}"
                        )
                    )
                }

                appointmentTrending.forEach { trend ->
                    allTrending.add(
                        SearchSuggestion(
                            text = trend.search_text,
                            type = SearchSuggestionType.TRENDING,
                            searchType = SearchType.APPOINTMENT,
                            icon = getHistoryIcon(SearchType.APPOINTMENT),
                            subtitle = context.getString(R.string.search_trending, trend.count) + " - ${getSearchTypeLabel(SearchType.APPOINTMENT)}"
                        )
                    )
                }

                noteTrending.forEach { trend ->
                    allTrending.add(
                        SearchSuggestion(
                            text = trend.search_text,
                            type = SearchSuggestionType.TRENDING,
                            searchType = SearchType.NOTE,
                            icon = getHistoryIcon(SearchType.NOTE),
                            subtitle = context.getString(R.string.search_trending, trend.count) + " - ${getSearchTypeLabel(SearchType.NOTE)}"
                        )
                    )
                }

                // Sắp xếp theo count và lấy top 5
                suggestions.addAll(allTrending.sortedByDescending {
                    it.subtitle?.substringBefore(" lần")?.substringAfterLast(" ")?.toIntOrNull() ?: 0
                }.take(5))
            } else {
                val trending = searchHistoryDao.getTrendingSearches(userId, searchType, 3)
                trending.forEach { trend ->
                    suggestions.add(
                        SearchSuggestion(
                            text = trend.search_text,
                            type = SearchSuggestionType.TRENDING,
                            searchType = searchType,
                            icon = getHistoryIcon(searchType),
                            subtitle = context.getString(R.string.search_trending, trend.count)
                        )
                    )
                }
            }
        }
        emit(suggestions.distinctBy { it.text + it.searchType.name }.take(15))
    }

    private fun getSearchTypeLabel(searchType: SearchType): String {
        return when (searchType) {
            SearchType.CONTACT -> context.getString(R.string.contact)
            SearchType.APPOINTMENT -> context.getString(R.string.appointment)
            SearchType.NOTE -> context.getString(R.string.notes)
            SearchType.ALL -> context.getString(R.string.all)
        }
    }

    private suspend fun generateAutocompleteSuggestions(
        userId: Int,
        query: String,
        searchType: SearchType
    ): Flow<List<SearchSuggestion>> = flow {
        val suggestions = mutableListOf<SearchSuggestion>()

        when (searchType) {
            // tên gợi ý
            SearchType.CONTACT -> {
                contactDao.getNameSuggestions(userId, query, 5).first().forEach { name ->
                    suggestions.add(
                        SearchSuggestion(
                            text = name,
                            type = SearchSuggestionType.AUTOCOMPLETE,
                            searchType = searchType,
                            icon = R.drawable.ic_contact,
                            subtitle = context.getString(R.string.name_contact)
                        )
                    )
                }

                // Địa chỉ đề xuất
                contactDao.getAddressSuggestions(userId, query, 5).first().forEach { address ->
                    suggestions.add(
                        SearchSuggestion(
                            text = address,
                            type = SearchSuggestionType.AUTOCOMPLETE,
                            searchType = searchType,
                            icon = R.drawable.ic_geo,
                            subtitle = context.getString(R.string.address)
                        )
                    )
                }

                // Vai trò đề xuất
                contactDao.getRoleSuggestions(userId, query, 3).first().forEach { role ->
                    suggestions.add(
                        SearchSuggestion(
                            text = role,
                            type = SearchSuggestionType.AUTOCOMPLETE,
                            searchType = searchType,
                            icon = R.drawable.ic_role,
                            subtitle = context.getString(R.string.role)
                        )
                    )
                }
            }

            SearchType.APPOINTMENT -> {
                // tiêu đề đề xuất
                appointmentDao.getTitleSuggestions(userId, query, 5).first().forEach { title ->
                    suggestions.add(
                        SearchSuggestion(
                            text = title,
                            type = SearchSuggestionType.AUTOCOMPLETE,
                            searchType = searchType,
                            icon = R.drawable.ic_appointment,
                            subtitle = context.getString(R.string.title_appointment)
                        )
                    )
                }

                // Địa chỉ đề xuất
                appointmentDao.getLocationSuggestions(userId, query, 3).first()
                    .forEach { location ->
                        suggestions.add(
                            SearchSuggestion(
                                text = location,
                                type = SearchSuggestionType.AUTOCOMPLETE,
                                searchType = searchType,
                                icon = R.drawable.ic_geo,
                                subtitle = context.getString(R.string.address)
                            )
                        )
                    }
            }

            SearchType.NOTE -> {
                // Tiều đề gợi ý
                noteDao.getTitleSuggestions(userId, query, 8).first().forEach { title ->
                    suggestions.add(
                        SearchSuggestion(
                            text = title,
                            type = SearchSuggestionType.AUTOCOMPLETE,
                            searchType = searchType,
                            icon = R.drawable.ic_note,
                            subtitle = context.getString(R.string.title_note)
                        )
                    )
                }
            }

            SearchType.ALL -> {

                coroutineScope {
                    val contactSuggestionsDeferred = async {
                        generateAutocompleteSuggestions(
                            userId,
                            query,
                            SearchType.CONTACT
                        ).first()
                    }
                    val appointmentSuggestionsDeferred = async {
                        generateAutocompleteSuggestions(
                            userId,
                            query,
                            SearchType.APPOINTMENT
                        ).first()
                    }
                    val noteSuggestionsDeferred = async {
                        generateAutocompleteSuggestions(
                            userId,
                            query,
                            SearchType.NOTE
                        ).first()
                    }


                    val contactSuggestions = contactSuggestionsDeferred.await().take(3)
                    val appointmentSuggestions = appointmentSuggestionsDeferred.await().take(3)
                    val noteSuggestions = noteSuggestionsDeferred.await().take(3)

                    suggestions.addAll(contactSuggestions)
                    suggestions.addAll(appointmentSuggestions)
                    suggestions.addAll(noteSuggestions)
                }
            }
        }

        emit(suggestions)
    }

    private fun getHistoryIcon(searchType: SearchType): Int {
        return when (searchType) {
            SearchType.CONTACT -> R.drawable.ic_contact
            SearchType.APPOINTMENT -> R.drawable.ic_appointment
            SearchType.NOTE -> R.drawable.ic_note
            SearchType.ALL -> R.drawable.ic_search
        }
    }

    /**
     * lấy các gợi ý lọc nhanh
     */
    fun getQuickFilterSuggestions(searchType: SearchType): List<SearchSuggestion> {
        return when (searchType) {
            SearchType.CONTACT -> listOf(
                SearchSuggestion(
                    context.getString(R.string.favorite),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_favorite_heart,
                    context.getString(R.string.desc_favorite_contact)
                ),
                SearchSuggestion(
                    context.getString(R.string.have_phone_number),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_phone,
                    context.getString(R.string.desc_have_phone)
                ),
                SearchSuggestion(
                    context.getString(R.string.have_email),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_email,
                    context.getString(R.string.desc_have_email)
                ),
                SearchSuggestion(
                    context.getString(R.string.have_address),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_geo,
                    context.getString(R.string.desc_have_address)
                )
            )

            SearchType.APPOINTMENT -> listOf(
                SearchSuggestion(
                    context.getString(R.string.today),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_today,
                    context.getString(R.string.desc_today_appointment)
                ),
                SearchSuggestion(
                    context.getString(R.string.upcoming),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_upcoming,
                    context.getString(R.string.desc_upcoming_appointment)
                ),
                SearchSuggestion(
                    context.getString(R.string.pinned),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_pin,
                    context.getString(R.string.desc_pinned_appointment)
                ),
                SearchSuggestion(
                    context.getString(R.string.weekend),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_week_calendar,
                    context.getString(R.string.desc_week_appointment)
                )
            )

            SearchType.NOTE -> listOf(
                SearchSuggestion(
                    context.getString(R.string.pinned),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_pin,
                    context.getString(R.string.desc_pinned_note)
                ),
                SearchSuggestion(
                    context.getString(R.string.reminder),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_alarm,
                    context.getString(R.string.desc_reminder_note)
                ),
                SearchSuggestion(
                    context.getString(R.string.check_list),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_checklist,
                    context.getString(R.string.desc_checklist_note)
                ),
                SearchSuggestion(
                    context.getString(R.string.recent),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_history,
                    context.getString(R.string.desc_recent_note)
                )
            )

            SearchType.ALL -> listOf(
                SearchSuggestion(
                    context.getString(R.string.favorite),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_favorite_heart,
                    context.getString(R.string.desc_favorite_all)
                ),
                SearchSuggestion(
                    context.getString(R.string.today),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_today,
                    context.getString(R.string.desc_today_all)
                ),
                SearchSuggestion(
                    context.getString(R.string.recent),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_history,
                    context.getString(R.string.desc_recent_all)
                )
            )
        }
    }



    /**
     *  Lưu lịch sử tìm kiếm
     */
    suspend fun saveSearchHistory(
        userId: Int,
        query: String,
        searchType: SearchType,
        resultCount: Int
    ) {
        val existingHistory = searchHistoryDao.getSearchHistory(userId, searchType)
            .find { it.searchText.equals(query, ignoreCase = true) }

        if (existingHistory != null) {
            searchHistoryDao.updateLastUsed(userId, query, searchType)
        } else {
            searchHistoryDao.insertSearchHistory(
                SearchHistory(
                    userId = userId,
                    searchText = query,
                    searchType = searchType,
                    resultCount = resultCount
                )
            )
        }
    }


    /**
     * Xóa lịch sử tìm kiếm
     */
    suspend fun deleteSearchHistory(userId: Int, searchText: String, searchType: SearchType) {
        searchHistoryDao.deleteSearchHistory(userId, searchText, searchType)
    }

    /**
     * xóa toàn bộ lịch sử tìm kiếm
     */
    suspend fun clearSearchHistory(userId: Int, searchType: SearchType) {
        searchHistoryDao.clearSearchHistory(userId, searchType)
    }

    /**
     * Xóa toàn bộ lịch sử tìm kiếm của người dùng
     */
    suspend fun clearAllSearchHistory(userId: Int) = searchHistoryDao.clearAllSearchHistory(userId)


    /**
     * Lấy quick filter suggestions với counts cụ thể
     */
    suspend fun getQuickFilterSuggestionsWithCount(
        userId: Int,
        searchType: SearchType
    ): List<SearchSuggestion> {
        val baseSuggestions = getQuickFilterSuggestions(searchType)

        return baseSuggestions.map { suggestion ->
            val count = getQuickFilterCount(suggestion.text, searchType, userId)
            suggestion.copy(
                resultCount = count
            )
        }
    }

    /**
     * Lấy số lượng kết quả cho quick filter
     */
    private suspend fun getQuickFilterCount(
        filterText: String,
        searchType: SearchType,
        userId: Int
    ): Int {
        return try {
            when (searchType) {
                SearchType.CONTACT -> getContactsByQuickFilter(userId, filterText).first().size
                SearchType.APPOINTMENT -> getAppointmentsByQuickFilter(
                    userId,
                    filterText
                ).first().size
                SearchType.NOTE -> getNotesByQuickFilter(userId, filterText).first().size
                SearchType.ALL -> getAllItemsByQuickFilter(userId, filterText).totalCount
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Áp dụng quick filter cho contacts
     */
    suspend fun getContactsByQuickFilter(userId: Int, filterText: String): Flow<List<Contact>> {
        val filterKey = Constant.getFilterKeyFromText(filterText, context)
        return when (filterKey) {
            Constant.FILTER_FAVORITE -> contactDao.getFavoriteContacts(userId)
            Constant.FILTER_HAVE_PHONE -> contactDao.getContactsWithPhone(userId)
            Constant.FILTER_HAVE_EMAIL -> contactDao.getContactsWithEmail(userId)
            Constant.FILTER_HAVE_ADDRESS -> contactDao.getContactsWithAddress(userId)
            else -> contactDao.getAllContactsByUser(userId)
        }
    }

    /**
     * Lấy cuộn hẹn theo quick filter
     */
    suspend fun getAppointmentsByQuickFilter(
        userId: Int,
        filterType: String,
        status: AppointmentStatus = AppointmentStatus.SCHEDULED
    ): Flow<List<AppointmentPlus>> {
        val statusString = status.name

        val filterKey = Constant.getFilterKeyFromText(filterType, context)

        return when (filterKey) {
            Constant.FILTER_TODAY -> {
                appointmentDao.getTodayAppointments(userId, status)
            }

            Constant.FILTER_WEEK -> {
                val (weekStart, weekEnd) = getWeekRange()
                appointmentDao.getThisWeekAppointments(userId, weekStart, weekEnd, statusString)
            }

            Constant.FILTER_PINNED -> {
                appointmentDao.getPinnedAppointments(userId, status)
            }

            Constant.FILTER_UPCOMING -> {
                appointmentDao.getUpcomingAppointments(userId, System.currentTimeMillis(), status)
            }

            else -> {
                appointmentDao.getAllAppointmentsByUser(userId)
            }
        }
    }


    /**
     * Áp dụng quick filter cho notes
     */
    suspend fun getNotesByQuickFilter(userId: Int, filterText: String): Flow<List<Note>> {
        val filterKey = Constant.getFilterKeyFromText(filterText, context)
        return when (filterKey) {
            Constant.FILTER_PINNED -> noteDao.getPinnedNotes(userId)
            Constant.FILTER_REMINDER -> noteDao.getNotesWithReminder(userId)
            Constant.FILTER_CHECKLIST -> noteDao.getChecklistNotes(userId)
            Constant.FILTER_RECENT -> noteDao.getRecentNotesFlow(userId)
            else -> noteDao.getAllNotesByUser(userId)
        }
    }

    /**
     * Áp dụng quick filter cho tất cả loại (ALL)
     */
    suspend fun getAllItemsByQuickFilter(userId: Int, filterText: String): UniversalSearchResult {
        val filterKey = Constant.getFilterKeyFromText(filterText, context)
        return when (filterKey) {
            Constant.FILTER_FAVORITE -> {
                val contacts = contactDao.getFavoriteContacts(userId).first()
                val appointments = appointmentDao.getPinnedAppointments(userId).first()
                val notes = noteDao.getPinnedNotes(userId).first()
                UniversalSearchResult(
                    contacts = contacts,
                    appointments = appointments,
                    notes = notes,
                    totalCount = contacts.size + appointments.size + notes.size
                )
            }

            Constant.FILTER_TODAY -> {
                val appointments = appointmentDao.getTodayAppointments(userId).first()
                val notes = noteDao.getTodayNotes(userId).first()
                UniversalSearchResult(
                    appointments = appointments,
                    notes = notes,
                    totalCount = appointments.size + notes.size
                )
            }

            Constant.FILTER_RECENT -> {
                val contacts = contactDao.getRecentContacts(userId)
                val appointments = appointmentDao.getTodayAppointments(userId).first()
                val notes = noteDao.getRecentNotes(userId)
                UniversalSearchResult(
                    contacts = contacts,
                    appointments = appointments,
                    notes = notes,
                    totalCount = contacts.size + appointments.size + notes.size
                )
            }

            else -> UniversalSearchResult()
        }
    }


    /**
     * Lấy range của tuần hiện tại
     */
    private fun getWeekRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()

        // Đặt về đầu tuần (Chủ nhật)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.timeInMillis

        // Đặt về cuối tuần (Thứ bảy)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val weekEnd = calendar.timeInMillis

        return Pair(weekStart, weekEnd)
    }

    suspend fun getAppointmentByContactId(userId:Int,contactId:Int,status: AppointmentStatus) = appointmentDao.getAppointmentByContactId(userId,contactId,status)
}