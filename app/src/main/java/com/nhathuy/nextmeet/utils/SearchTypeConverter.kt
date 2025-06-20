package com.nhathuy.nextmeet.utils

import androidx.room.TypeConverters
import com.nhathuy.nextmeet.model.SearchType

class SearchTypeConverter {
    @TypeConverters
    fun fromSearchType(type:SearchType):String = type.name
    @TypeConverters
    fun toSearchType(value:String) : SearchType = SearchType.valueOf(value)
}