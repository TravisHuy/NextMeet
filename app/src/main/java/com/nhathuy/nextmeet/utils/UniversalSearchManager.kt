package com.nhathuy.nextmeet.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.repository.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalSearchManager @Inject constructor(
    private val searchRepository: SearchRepository,
    private val context: Context
) {
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDelay = 300L

    private val searchJob = SupervisorJob()
    private val searchScope = CoroutineScope(searchJob + Dispatchers.Main)
    private var currentSearchJob: Job? = null

    /**
     * Thực hiện tìm kiếm đã trả về với kết quả gọi lại
     */
    fun performDebouncedSearch(
        userId: Int, query: String, searchType: SearchType,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        // huy previous search
        searchRunnable?.let { handler.removeCallbacks(it) }
        currentSearchJob?.cancel()

        // tạo new gọi lại search
        searchRunnable = Runnable {
            performSearch(userId, query, searchType, onResult, onError)
        }

        handler.postDelayed(searchRunnable!!, searchDelay)
    }

    /**
     * Thực hiện tìm kiếm ngay lập tức mà ko cần debounce
     */
    fun performImmediateSearch(
        userId: Int,
        query: String,
        searchType: SearchType,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        searchRunnable?.let { handler.removeCallbacks(it) }
        currentSearchJob?.cancel()

        performSearch(userId, query, searchType, onResult, onError)
    }

    private fun performSearch(
        userId: Int,
        query: String,
        searchType: SearchType,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit
    ) {
        currentSearchJob = searchScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (searchType) {
                        SearchType.ALL -> {
                            searchRepository.performUniversalSearch(userId, query)
                        }

                        SearchType.CONTACT -> {
                            val contacts = searchRepository.searchContacts(userId, query).first()
                            UniversalSearchResult(contacts = contacts, totalCount = contacts.size)
                        }

                        SearchType.APPOINTMENT -> {
                            val appointments =
                                searchRepository.searchAppointments(userId, query).first()
                            UniversalSearchResult(
                                appointments = appointments,
                                totalCount = appointments.size
                            )
                        }

                        SearchType.NOTE -> {
                            val notes = searchRepository.searchNotes(userId, query).first()
                            UniversalSearchResult(notes = notes, totalCount = notes.size)
                        }
                    }
                }

                // lưu kết quả tìm kiếm nếu query không rỗng
                if (query.isNotBlank()) {
                    withContext(Dispatchers.IO) {
                        searchRepository.saveSearchHistory(
                            userId = userId,
                            query = query,
                            searchType = searchType,
                            resultCount = result.totalCount
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_search_failed))
                }
            }
        }
    }

    /**
     * Thực hiện quick filter search cho tất cả search types
     */
    fun performQuickFilterSearch(
        userId: Int,
        filterText: String,
        searchType: SearchType,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        // cancel previous searches
        searchRunnable?.let { handler.removeCallbacks(it) }
        currentSearchJob?.cancel()

        currentSearchJob = searchScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (searchType) {
                        SearchType.ALL -> {
                            searchRepository.getAllItemsByQuickFilter(userId, filterText)
                        }

                        SearchType.CONTACT -> {
                            val contacts =
                                searchRepository.getContactsByQuickFilter(userId, filterText)
                                    .first()
                            UniversalSearchResult(
                                contacts = contacts,
                                totalCount = contacts.size
                            )
                        }

                        SearchType.APPOINTMENT -> {
                            val filterKey = Constant.getFilterKeyFromText(filterText, context)
                            if (filterKey != null) {
                                val appointments =
                                    searchRepository.getAppointmentsByQuickFilter(userId, filterKey)
                                        .first()
                                UniversalSearchResult(
                                    appointments = appointments,
                                    totalCount = appointments.size
                                )
                            } else {
                                throw IllegalArgumentException(context.getString(R.string.error_invalid_filter))
                            }
                        }

                        SearchType.NOTE -> {
                            val notes =
                                searchRepository.getNotesByQuickFilter(userId, filterText).first()
                            UniversalSearchResult(
                                notes = notes,
                                totalCount = notes.size
                            )
                        }
                    }
                }


                withContext(Dispatchers.Main) {
                    onResult(result)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_quick_filter))
                }
            }
        }
    }

    /**
     * Perform category search to get all items of a specific type
     */
    fun performCategorySearch(
        userId: Int,
        searchType: SearchType,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit
    ) {
        searchRunnable?.let { handler.removeCallbacks(it) }
        currentSearchJob?.cancel()

        currentSearchJob = searchScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    when (searchType) {
                        SearchType.ALL -> {
                            val appointmentsDeferred =
                                async { searchRepository.getAllAppointmentsByUser(userId).first() }
                            val contactsDeferred =
                                async { searchRepository.getAllContactsByUser(userId).first() }
                            val notesDeferred =
                                async { searchRepository.getAllNotesByUser(userId).first() }

                            val appointments = appointmentsDeferred.await()
                            val contacts = contactsDeferred.await()
                            val notes = notesDeferred.await()

                            UniversalSearchResult(
                                appointments = appointments,
                                contacts = contacts,
                                notes = notes,
                                totalCount = appointments.size + contacts.size + notes.size
                            )
                        }

                        SearchType.APPOINTMENT -> {
                            val appointments =
                                searchRepository.getAllAppointmentsByUser(userId).first()
                            UniversalSearchResult(
                                appointments = appointments,
                                totalCount = appointments.size
                            )
                        }

                        SearchType.CONTACT -> {
                            val contacts = searchRepository.getAllContactsByUser(userId).first()
                            UniversalSearchResult(
                                contacts = contacts,
                                totalCount = contacts.size
                            )
                        }

                        SearchType.NOTE -> {
                            val notes = searchRepository.getAllNotesByUser(userId).first()
                            UniversalSearchResult(
                                notes = notes,
                                totalCount = notes.size
                            )
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult(result)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(
                        context.getString(
                            R.string.error_loading_items,
                            searchType.name.lowercase(),
                            e.message ?: "unknown"
                        )
                    )

                }
            }
        }
    }


    /**
     * tạo tìm kiếm với gợi ý with
     */
    fun generateSearchSuggestions(
        userId: Int,
        query: String,
        searchType: SearchType,
        onResult: (List<SearchSuggestion>) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        searchScope.launch {
            try {
                val suggestions = withContext(Dispatchers.IO) {
                    searchRepository.generateSearchSuggestions(userId, query, searchType).first()
                }
                withContext(Dispatchers.Main) {
                    onResult(suggestions)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_generate_suggestion))
                }
            }
        }
    }

    /**
     * filter search contact
     */
    fun performContactFilterSearch(
        userId: Int,
        contactId: Int,
        contactName: String,
        onResult: (UniversalSearchResult) -> Unit,
        onError: (String) -> Unit
    ) {
        searchScope.launch {
            try {
                // Get appointments for specific contact with SCHEDULED status
                val appointments = searchRepository.getAppointmentByContactId(
                    userId,
                    contactId,
                    AppointmentStatus.SCHEDULED
                )

                // Calculate total count
                val totalCount = appointments.size

                val result = UniversalSearchResult(
                    contacts = emptyList(),
                    appointments = appointments,
                    notes = emptyList(),
                    totalCount = totalCount
                )

                onResult(result)

                // Save this as search history

                searchRepository.saveSearchHistory(
                    userId,
                    "Contact: $contactName",
                    SearchType.APPOINTMENT,
                    1
                )

            } catch (e: Exception) {
                onError(
                    context.getString(
                        R.string.error_filter_by_contact_detail,
                        e.message ?: "unknown"
                    )
                )
            }
        }
    }

    /**
     * Delete search history item
     */
    fun deleteSearchHistory(
        userId: Int,
        searchText: String,
        searchType: SearchType,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        searchScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    searchRepository.deleteSearchHistory(userId, searchText, searchType)
                }

                withContext(Dispatchers.Main) {
                    onComplete()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_delete_history))
                }
            }
        }
    }

    /**
     * Clear all search history for a specific type
     */
    fun clearSearchHistory(
        userId: Int,
        searchType: SearchType,
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        searchScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    searchRepository.clearSearchHistory(userId, searchType)
                }

                withContext(Dispatchers.Main) {
                    onComplete()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_clear_history))
                }
            }
        }
    }

    fun clearAllSearchHistory(
        userId: Int, onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        searchScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    searchRepository.clearAllSearchHistory(userId)
                }

                withContext(Dispatchers.Main) {
                    onComplete()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: context.getString(R.string.error_clear_all_history))
                }
            }
        }
    }

    /**
     * Cancel all ongoing search operations
     */
    fun cancelAllSearches() {
        searchRunnable?.let { handler.removeCallbacks(it) }
        currentSearchJob?.cancel()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        cancelAllSearches()
        searchJob.cancel()
    }

}
