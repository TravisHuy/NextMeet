package com.nhathuy.nextmeet.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.SearchSuggestionsAdapter
import com.nhathuy.nextmeet.databinding.ActivitySearchBinding
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding

    //view model
    private val searchViewModel : SearchViewModel by viewModels()

    // adapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter
    private var currentSearch = ""
    companion object {
        const val TAG = "SearchActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.search_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        setupUI()
//        observeSearchData()
    }

    private fun setupUI(){

    }

    private fun observeSearchData(){
        lifecycleScope.launch {
            searchViewModel.uiState.collect { state ->
                handleSearchUiState(state)
            }
        }

        lifecycleScope.launch {
            searchViewModel.suggestions.collect { suggestions ->
                searchSuggestionsAdapter.submitList(suggestions)
            }
        }
    }

    private fun handleSearchUiState(state: SearchUiState) {
//        when (state) {
//            is SearchUiState.Loading -> showLoading()
//
//            is SearchUiState.SearchResultsLoaded -> {
//                hideLoading()
//                updateContactsList(state.results.contacts)
//                updateSearchBar(state.query)
//                updateUIState()
//            }
//
//            is SearchUiState.SuggestionsLoaded -> {
//                // Already handled by flow collection
//            }
//
//            is SearchUiState.Error -> {
//                hideLoading()
//                showMessage(state.message)
//            }
//
//            is SearchUiState.SearchHistoryDeleted -> {
//                showMessage(state.message)
//            }
//
//            else -> {}
//        }
    }
}