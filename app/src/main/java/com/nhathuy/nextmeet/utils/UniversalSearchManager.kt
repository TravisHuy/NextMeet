package com.nhathuy.nextmeet.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.repository.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalSearchManager @Inject constructor(
    private val searchRepository: SearchRepository,
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
                val result = withContext(Dispatchers.IO){
                    when (searchType) {
                        SearchType.ALL -> {
                            searchRepository.performUniversalSearch(userId, query)
                        }
                        SearchType.CONTACT -> {
                            val contacts = searchRepository.searchContacts(userId, query).first()
                            UniversalSearchResult(contacts = contacts, totalCount = contacts.size)
                        }
                        SearchType.APPOINTMENT -> {
                            val  appointments = searchRepository.searchAppointments(userId, query).first()
                            UniversalSearchResult(appointments = appointments, totalCount = appointments.size)
                        }
                        SearchType.NOTE-> {
                            val notes = searchRepository.searchNotes(userId, query).first()
                            UniversalSearchResult(notes = notes , totalCount = notes.size)
                        }
                    }
                }

                // lưu kết quả tìm kiếm nếu query không rỗng
                if(query.isNotBlank()){
                    withContext(Dispatchers.IO){
                        searchRepository.saveSearchHistory(
                            userId = userId,
                            query = query,
                            searchType = searchType,
                            resultCount = result.totalCount
                        )
                    }
                }

                withContext(Dispatchers.Main){
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main){
                    onError(e.message ?: "An error occurred during search")
                }
            }
        }
    }

    /**
     * tạo tìm kiếm với gợi ý with
     */
    fun generateSearchSuggestions(
        userId: Int,
        query:String,
        searchType: SearchType,
        onResult: (List<SearchSuggestion>) -> Unit,
        onError: (String) -> Unit = {}
    ){
        searchScope.launch {
            try {
                val suggestions = withContext(Dispatchers.IO){
                    searchRepository.generateSearchSuggestions(userId, query, searchType).first()
                }
                withContext(Dispatchers.Main){
                    onResult(suggestions)
                }
            }
            catch (e:Exception){
                withContext(Dispatchers.Main){
                    onError(e.message ?: "Error generating suggestions")
                }
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
                    onError(e.message ?: "Error deleting search history")
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
                    onError(e.message ?: "Error clearing search history")
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
