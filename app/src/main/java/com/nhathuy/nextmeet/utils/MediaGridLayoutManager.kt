package com.nhathuy.nextmeet.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MediaGridLayoutManager(
    context: Context,
    private val imageCount: Int
) : GridLayoutManager(context, getSpanCount(imageCount)) {

    init {
        spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (imageCount) {
                    1 -> 2 // item chiếm hết 2 cột
                    2 -> 1 // mỗi item 1 cột
                    else -> if (position == 0) 2 else 1 // item đầu chiếm 2 cột
                }
            }
        }
    }

    companion object {
        private fun getSpanCount(imageCount: Int): Int {
            return when {
                imageCount <= 1 -> 2
                imageCount == 2 -> 2
                else -> 2 // số cột tối đa
            }
        }
    }
}
