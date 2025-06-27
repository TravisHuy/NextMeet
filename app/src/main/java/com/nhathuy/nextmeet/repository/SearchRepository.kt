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
     * Tìm kiếm liên hệ
     */
    suspend fun searchContacts(userId: Int, query: String): Flow<List<Contact>> =
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
    suspend fun generateSearchSuggestions(
        userId: Int,
        query: String,
        searchType: SearchType
    ): Flow<List<SearchSuggestion>> = flow {

        val suggestions = mutableListOf<SearchSuggestion>()

        //1.history suggestions
        if (query.isEmpty()) {
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
        } else {
            //filtered history
            val history = searchHistoryDao.getSearchHistory(userId, searchType, 10)
            history.filter {
                it.searchText.contains(query, ignoreCase = true)
            }
                .take(3).forEach { historyItem ->
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
        emit(suggestions.distinctBy { it.text }.take(15))
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
                            subtitle = "Vai trò"
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
                    "Liên hệ đã ghim"
                ),
                SearchSuggestion(
                    context.getString(R.string.have_phone_number),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_phone,
                    "Liên hệ có SĐT"
                ),
                SearchSuggestion(
                    context.getString(R.string.have_email),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_email,
                    "Liên hệ có email"
                ),
                SearchSuggestion(
                    context.getString(R.string.have_address),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_geo,
                    "Liên hệ có địa chỉ"
                )
            )

            SearchType.APPOINTMENT -> listOf(
                SearchSuggestion(
                    context.getString(R.string.today),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_today,
                    "Cuộc hẹn hôm nay"
                ),
                SearchSuggestion(
                    context.getString(R.string.upcoming),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_upcoming,
                    "Cuộc hẹn sắp tới"
                ),
                SearchSuggestion(
                    context.getString(R.string.pinned),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_pin,
                    "Cuộc hẹn đã ghim"
                ),
                SearchSuggestion(
                    context.getString(R.string.weekend),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_week_calendar,
                    "Cuộc hẹn tuần này"
                )
            )

            SearchType.NOTE -> listOf(
                SearchSuggestion(
                    context.getString(R.string.pinned),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_pin,
                    "Ghi chú đã ghim"
                ),
                SearchSuggestion(
                    context.getString(R.string.reminder),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_alarm,
                    "Ghi chú có nhắc nhở"
                ),
                SearchSuggestion(
                    context.getString(R.string.check_list),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_checklist,
                    "Checklist"
                ),
                SearchSuggestion(
                    context.getString(R.string.recent),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_history,
                    "Ghi chú gần đây"
                )
            )

            SearchType.ALL -> listOf(
                SearchSuggestion(
                    context.getString(R.string.favorite),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_favorite_heart,
                    "Tất cả mục yêu thích"
                ),
                SearchSuggestion(
                    context.getString(R.string.today),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_today,
                    "Hoạt động hôm nay"
                ),
                SearchSuggestion(
                    context.getString(R.string.recent),
                    SearchSuggestionType.QUICK_FILTER,
                    searchType,
                    R.drawable.ic_history,
                    "Được tạo gần đây"
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
        status: String = "SCHEDULED"
    ): Flow<List<AppointmentPlus>> {
        return when (filterType) {
            Constant.FILTER_TODAY -> {
                appointmentDao.getTodayAppointments(userId, status)
            }

            Constant.FILTER_WEEK -> {
                val (weekStart, weekEnd) = getWeekRange()
                appointmentDao.getThisWeekAppointments(userId, weekStart, weekEnd, status)
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