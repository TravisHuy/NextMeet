package com.nhathuy.nextmeet.viewmodel

import androidx.lifecycle.ViewModel
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.utils.UniversalSearchManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(private val searchManager: UniversalSearchManager) : ViewModel() {

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
     * Handle quick filter suggestions
     */
    private fun handleQuickFilter(suggestion: SearchSuggestion) {
        // For now, we'll treat quick filters as search queries
        // You can implement more specific filter logic here
        searchImmediate(suggestion.text, suggestion.searchType)
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
                _uiState.value = SearchUiState.SearchHistoryDeleted("Search history deleted")
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
                _uiState.value = SearchUiState.SearchHistoryCleared("Search history cleared")
                // Refresh suggestions
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

    override fun onCleared() {
        super.onCleared()
        searchManager.cleanup()
    }


}