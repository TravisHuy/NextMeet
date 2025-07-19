package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ItemSearchSuggestionBinding
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.SearchSuggestion
import com.nhathuy.nextmeet.model.SearchSuggestionType

class SearchSuggestionsAdapter(
    private val onSuggestionClick: (SearchSuggestion) -> Unit,
    private val onDeleteSuggestion: (SearchSuggestion) -> Unit
) : ListAdapter<SearchSuggestion, SearchSuggestionsAdapter.SuggestionViewHolder>(DiffCallback) {

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
                if (suggestion.type == SearchSuggestionType.HISTORY || suggestion.type == SearchSuggestionType.RECENT) {
                    ivDeleteSuggestion.visibility = View.VISIBLE
                    ivDeleteSuggestion.setOnClickListener {
                        onDeleteSuggestion(suggestion   )
                    }
                } else {
                    ivDeleteSuggestion.visibility = View.GONE
                }

                root.setOnClickListener { onSuggestionClick(suggestion) }

                if (suggestion.resultCount > 0) {
                    binding.tvResultCount.text = root.context.getString(
                        R.string.search_result_count,
                        suggestion.resultCount
                    )
                    binding.tvResultCount.visibility = View.VISIBLE
                } else {
                    binding.tvResultCount.visibility = View.GONE
                }

                when (suggestion.type) {
                    SearchSuggestionType.HISTORY -> {
                        tvSuggestionText.setTextColor(
                            root.context.getColor(R.color.black)
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


    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SearchSuggestion>() {
        override fun areItemsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem.text == newItem.text && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem == newItem
        }
    }

}