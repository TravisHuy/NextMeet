package com.nhathuy.nextmeet.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.utils.UniversalSearchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val searchManager: UniversalSearchManager,
    @ApplicationContext private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _currentQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    private val _currentSearchType = MutableStateFlow(SearchType.ALL)
    val currentSearchType: StateFlow<SearchType> = _currentSearchType.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchResults = MutableStateFlow(UniversalSearchResult())
    val searchResults: StateFlow<UniversalSearchResult> = _searchResults.asStateFlow()

    private val _suggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val suggestions: StateFlow<List<SearchSuggestion>> = _suggestions.asStateFlow()

    private val _navigationFilter = MutableStateFlow("")
    val navigationFilter: StateFlow<String> = _navigationFilter.asStateFlow()


    private var currentUserId: Int = 0

    fun initializeSearch(userId: Int) {
        currentUserId = userId
        loadInitialSuggestions()
    }

    /**
     * Set search type and reload suggestions
     */
    fun setSearchType(searchType: SearchType) {
        _currentSearchType.value = searchType
        generateSuggestions(_currentQuery.value)
    }

    /**
     * Set search type without generating suggestions
     */
    fun setSearchTypeOnly(searchType: SearchType) {
        _currentSearchType.value = searchType
    }


    /**
     * Perform search with debounce
     */
    fun search(query: String, searchType: SearchType = _currentSearchType.value) {
        _currentQuery.value = query
        _currentSearchType.value = searchType

        if (query.isBlank()) {
            // Load suggestions for empty query
            generateSuggestions("")
            _searchResults.value = UniversalSearchResult()
            return
        }

        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performDebouncedSearch(
            userId = currentUserId,
            query = query,
            searchType = searchType,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = query,
                    searchType = searchType
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
     * Perform immediate search without debounce
     */

    fun searchImmediate(query: String,searchType: SearchType = _currentSearchType.value){
        _currentQuery.value = query
        _currentSearchType.value = searchType
        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performImmediateSearch(
            userId = currentUserId,
            query = query,
            searchType = searchType,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = query,
                    searchType = searchType
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    fun searchImmediateCategory(query: String, searchType: SearchType = _currentSearchType.value){
        _currentQuery.value = query
        _currentSearchType.value = searchType
        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performCategorySearch(
            userId = currentUserId,
            searchType = searchType,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = query,
                    searchType = searchType
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }


    /**
     * Search for all items in a specific category (for category buttons)
     */
    fun searchByCategory(searchType: SearchType) {
        _currentQuery.value = ""
        _currentSearchType.value = searchType
        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performCategorySearch(
            userId = currentUserId,
            searchType = searchType,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = "", // Empty query for category search
                    searchType = searchType
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }


    private fun loadInitialSuggestions() {
        generateSuggestions("")
    }

    /**
     * Generate search suggestions based on current query
     */
    fun generateSuggestions(query: String){
        _uiState.value = SearchUiState.LoadingSuggestions
        searchManager.generateSearchSuggestions(
            userId = currentUserId,
            query = query,
            searchType = _currentSearchType.value,
            onResult = { suggestions ->
                _suggestions.value = suggestions
                _uiState.value = SearchUiState.SuggestionsLoaded(suggestions)
            },
            onError = { error ->
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
     * Handle suggestion click
     */
    fun onSuggestionClick(suggestion: SearchSuggestion) {
        when (suggestion.type) {
            SearchSuggestionType.HISTORY,
            SearchSuggestionType.TRENDING,
            SearchSuggestionType.AUTOCOMPLETE -> {
                // Perform search with suggestion text
                searchImmediate(suggestion.text, suggestion.searchType)
            }

            SearchSuggestionType.QUICK_FILTER -> {
                // Handle quick filter differently - you might want to implement
                // specific filter logic here
                handleQuickFilter(suggestion)
            }

            SearchSuggestionType.RECENT -> {
                // Search with recent suggestion
                searchImmediate(suggestion.text, suggestion.searchType)
            }
        }
    }

    /**
     * Áp dụng quick filter cho tất cả search types
     */
    fun applyQuickFilter(filterText: String, searchType: SearchType = _currentSearchType.value) {
        _currentQuery.value = filterText
        _currentSearchType.value = searchType
        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performQuickFilterSearch(
            userId = currentUserId,
            filterText = filterText,
            searchType = searchType,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = filterText,
                    searchType = searchType
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }
    /**
     * appply contact quickfilter
     */
    fun applyContactFilter(contactId: Int, contactName: String) {
        _currentQuery.value = context.getString(R.string.contact_names, contactName)
        _currentSearchType.value = SearchType.APPOINTMENT
        _isSearching.value = true
        _uiState.value = SearchUiState.Loading

        searchManager.performContactFilterSearch(
            userId = currentUserId,
            contactId = contactId,
            contactName = contactName,
            onResult = { result ->
                _isSearching.value = false
                _searchResults.value = result
                _uiState.value = SearchUiState.SearchResultsLoaded(
                    results = result,
                    query = context.getString(R.string.contact_names, contactName),
                    searchType = SearchType.APPOINTMENT
                )
            },
            onError = { error ->
                _isSearching.value = false
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
    * Check if current search is a contact filter
    */
    fun isContactFilter(): Boolean {
        return _currentQuery.value.startsWith(context.getString(R.string.search_filter_contact_prefix))
    }

    /**
     * Get contact name from contact filter query
     */
    fun getContactNameFromFilter(): String? {
        return if (isContactFilter()) {
            _currentQuery.value.removePrefix(context.getString(R.string.search_filter_contact_prefix))
        } else null
    }
    /**
     * Handle quick filter suggestions
     */
    private fun handleQuickFilter(suggestion: SearchSuggestion) {
        applyQuickFilter(suggestion.text, suggestion.searchType)
    }


    /**
     * Delete search history item
     */
    fun deleteSearchHistory(suggestion: SearchSuggestion) {
        searchManager.deleteSearchHistory(
            userId = currentUserId,
            searchText = suggestion.text,
            searchType = suggestion.searchType,
            onComplete = {
                _uiState.value = SearchUiState.SearchHistoryDeleted(context.getString(R.string.search_history_deleted))
                // Refresh suggestions
                generateSuggestions(_currentQuery.value)
            },
            onError = { error ->
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
     * Clear all search history for current search type
     */
    fun clearSearchHistory() {
        searchManager.clearSearchHistory(
            userId = currentUserId,
            searchType = _currentSearchType.value,
            onComplete = {
                _uiState.value = SearchUiState.SearchHistoryCleared(context.getString(R.string.search_history_cleared))
                // Refresh suggestions
                generateSuggestions(_currentQuery.value)
            },
            onError = { error ->
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
     * Clear all search history
     */
    fun clearAllSearchHistory(){
        searchManager.clearAllSearchHistory(
            userId = currentUserId,
            onComplete = {
                _uiState.value = SearchUiState.SearchHistoryCleared(context.getString(R.string.search_history_cleared_all))
                generateSuggestions(_currentQuery.value)
            },
            onError = { error ->
                _uiState.value = SearchUiState.Error(error)
            }
        )
    }

    /**
     * Clear search and reset to initial state
     */
    fun clearSearch() {
        _currentQuery.value = ""
        _searchResults.value = UniversalSearchResult()
        _isSearching.value = false
        loadInitialSuggestions()
    }

    /**
     * Reset search to initial state with ALL type
     */
    fun resetToInitialState() {
        _currentQuery.value = ""
        _searchResults.value = UniversalSearchResult()
        _isSearching.value = false
        _currentSearchType.value = SearchType.ALL
        loadInitialSuggestions()
    }

    /**
     * Update query without triggering search (for text input changes)
     */
    fun updateQuery(query: String) {
        _currentQuery.value = query
        // Generate suggestions for query changes
        generateSuggestions(query)
    }



    /**
     * Get current search state info
     */
    fun getSearchInfo(): Triple<String, SearchType, Boolean> {
        return Triple(
            _currentQuery.value,
            _currentSearchType.value,
            _isSearching.value
        )
    }

    /**
     * Check if has search results
     */
    fun hasResults(): Boolean {
        return _searchResults.value.isNotEmpty()
    }

    /**
     * Check if has suggestions
     */
    fun hasSuggestions(): Boolean {
        return _suggestions.value.isNotEmpty()
    }

    fun setNavigationFilter(filter: String) {
        _navigationFilter.value = filter
    }

    // THÊM: Method để clear navigation filter
    fun clearNavigationFilter() {
        _navigationFilter.value = ""
    }
    override fun onCleared() {
        super.onCleared()
//        searchManager.cleanup()
    }
}