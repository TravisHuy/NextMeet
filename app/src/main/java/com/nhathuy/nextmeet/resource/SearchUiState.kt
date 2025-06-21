package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.model.UniversalSearchResult

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    object LoadingSuggestions : SearchUiState()
    data class SearchResultsLoaded(
        val results: UniversalSearchResult,
        val query: String,
        val searchType: SearchType
    ) : SearchUiState()

    data class SuggestionsLoaded(val suggestions: List<SearchSuggestion>):SearchUiState()
    data class Error(val message:String) :SearchUiState()
    data class SearchHistoryDeleted(val message: String) :SearchUiState()
    data class SearchHistoryCleared(val message: String) : SearchUiState()
}