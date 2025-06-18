package com.nhathuy.nextmeet.model

data class SearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val icon: Int,
    val subtitle: String? = null
)
