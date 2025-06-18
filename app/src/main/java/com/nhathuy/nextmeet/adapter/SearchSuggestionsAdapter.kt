package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemSearchSuggestionBinding
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType

class SearchSuggestionsAdapter(
    private val onSuggestionClick: (String) -> Unit,
    private val onDeleteSuggestion: (String) -> Unit
) : ListAdapter<SearchSuggestion, SearchSuggestionsAdapter.SuggestionViewHolder>(DiffCallback()) {


    inner class SuggestionViewHolder(
        private val binding: ItemSearchSuggestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestion: SearchSuggestion) {
            binding.apply {
                tvSuggestionText.text = suggestion.text
                ivSuggestionIcon.setImageResource(suggestion.icon)

                if (suggestion.subtitle != null) {
                    tvSuggestionSubtitle.text = suggestion.subtitle
                    tvSuggestionSubtitle.visibility = View.VISIBLE
                } else {
                    tvSuggestionSubtitle.visibility = View.GONE
                }

                //show delete button only for history items
                if (suggestion.type == SearchSuggestionType.HISTORY) {
                    ivDeleteSuggestion.visibility = View.VISIBLE
                    ivDeleteSuggestion.setOnClickListener {
                        onDeleteSuggestion(suggestion.text)
                    }
                } else {
                    ivDeleteSuggestion.visibility = View.GONE
                }

                root.setOnClickListener { onSuggestionClick(suggestion.text) }

                when(suggestion.type){
                    SearchSuggestionType.HISTORY -> {
                        tvSuggestionText.setTextColor(
                            root.context.getColor(R.color.light_on_secondary)
                        )
                    }
                    SearchSuggestionType.QUICK_FILTER -> {
                        tvSuggestionText.setTextColor(
                            root.context.getColor(R.color.light_primary)
                        )
                    }
                    else -> {
                        tvSuggestionText.setTextColor(
                            root.context.getColor(R.color.text_primary)
                        )
                    }
                }

            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SearchSuggestionsAdapter.SuggestionViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SearchSuggestionsAdapter.SuggestionViewHolder,
        position: Int
    ) {
        holder.bind(getItem(position))
    }


    private class DiffCallback : DiffUtil.ItemCallback<SearchSuggestion>() {
        override fun areItemsTheSame(
            oldItem: SearchSuggestion,
            newItem: SearchSuggestion
        ): Boolean {
            return oldItem.text == newItem.text && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(
            oldItem: SearchSuggestion,
            newItem: SearchSuggestion
        ): Boolean {
            return oldItem == newItem
        }
    }

}