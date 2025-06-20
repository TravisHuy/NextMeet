package com.nhathuy.nextmeet.model

data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val searchType: SearchType,
    val icon: Int,
    val subtitle: String? = null,
    val resultCount: Int = 0
)
