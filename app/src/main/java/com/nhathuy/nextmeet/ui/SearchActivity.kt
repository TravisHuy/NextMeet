package com.nhathuy.nextmeet.ui

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.SearchResultsAdapter
import com.nhathuy.nextmeet.adapter.SearchSuggestionsAdapter
import com.nhathuy.nextmeet.databinding.ActivitySearchBinding
import com.nhathuy.nextmeet.model.SearchType
import com.nhathuy.nextmeet.resource.SearchUiState
import com.nhathuy.nextmeet.viewmodel.SearchViewModel
import com.nhathuy.nextmeet.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySearchBinding

    //view model
    private val searchViewModel : SearchViewModel by viewModels()
    private val userViewModel : UserViewModel by viewModels()
    // adapter
    private lateinit var searchSuggestionsAdapter: SearchSuggestionsAdapter
    private lateinit var searchResultsAdapter : SearchResultsAdapter

    private var currentSearch = ""
    private var currentSearchType = SearchType.ALL
    private var isUpdatingSearch = false

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

        setupUI()
        observeSearchData()
    }

    private fun setupUI(){
        setupUserInfo()
        setupSearchBar()
        setupSuggestionsRecyclerView()
        setupSearchResultsRecyclerView()
        setupSearchTabs()
        setupCategoryButtons()
        setupBackButton()
    }

    private fun setupUserInfo(){
        userViewModel.getCurrentUser().observe(this) { user ->
            user?.let {
                Log.d(TAG, "User loaded: ${user.id}, ${user.name}")
                searchViewModel.initializeSearch(user.id)
            }
        }
    }

    private fun setupSearchBar(){
        binding.apply {
            etSearch.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

                override fun afterTextChanged(s: Editable?) {

                    if (isUpdatingSearch) return

                    val query = s.toString().trim()
                    currentSearch = query

                    btnClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE

                    searchViewModel.generateSuggestions(query)
                }

            })

            etSearch.setOnEditorActionListener {
                _,actionId,_ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch()
                    true
                } else false
            }

            btnClear.setOnClickListener {
                etSearch.setText("")
                currentSearchType = SearchType.ALL
                binding.tabSearchFilters.getTabAt(0)?.select()
                searchViewModel.resetToInitialState()
                hideKeyboard()
            }

            btnVoiceSearch.setOnClickListener {

            }

            tvSearch.setOnClickListener {
                performSearch()
            }
        }
    }

    private fun setupSuggestionsRecyclerView(){
        searchSuggestionsAdapter = SearchSuggestionsAdapter(
            onSuggestionClick = { suggestion ->
                searchViewModel.onSuggestionClick(suggestion)
                hideKeyboard()
            },
            onDeleteSuggestion = { suggestion ->
                searchViewModel.deleteSearchHistory(suggestion)
            }
        )
        binding.rvRecentSearches.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchSuggestionsAdapter
        }
        binding.tvClearAll.setOnClickListener {
            searchViewModel.clearSearchHistory()
        }
    }

    private fun setupSearchResultsRecyclerView(){
        searchResultsAdapter = SearchResultsAdapter(
            this,
            onAppointmentClick = { appointment ->
                // TODO: Handle appointment click
                Toast.makeText(this, "Appointment clicked: ${appointment.title}", Toast.LENGTH_SHORT).show()
            },
            onContactClick = { contact ->
                // TODO: Handle contact click
                Toast.makeText(this, "Contact clicked: ${contact.name}", Toast.LENGTH_SHORT).show()
            },
            onNoteClick = { note ->
                // TODO: Handle note click
                Toast.makeText(this, "Note clicked: ${note.title}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchResultsAdapter
        }
    }

    private fun setupSearchTabs(){
        binding.tabSearchFilters.apply {
            addTab(newTab().setText(context.getString(R.string.all)).setTag(SearchType.ALL))
            addTab(newTab().setText(context.getString(R.string.appointment)).setTag(SearchType.APPOINTMENT))
            addTab(newTab().setText(context.getString(R.string.contact)).setTag(SearchType.CONTACT))
            addTab(newTab().setText(context.getString(R.string.notes)).setTag(SearchType.NOTE))

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.tag?.let { searchType ->
                        currentSearchType = searchType as SearchType
                        searchViewModel.setSearchType(currentSearchType)

                        // If we have a current search, perform it with new type
                        if (currentSearch.isNotEmpty()) {
                            searchViewModel.search(currentSearch, currentSearchType)
                        }
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
        }
    }

    private fun setupCategoryButtons(){
        binding.apply {
            btnAppointment.setOnClickListener {
                selectSearchType(SearchType.APPOINTMENT)
            }

            btnContact.setOnClickListener {
                selectSearchType(SearchType.CONTACT)
            }

            btnNotes.setOnClickListener {
                selectSearchType(SearchType.NOTE)
            }

            btnHistory.setOnClickListener {
                // Show all search history
                searchViewModel.setSearchType(SearchType.ALL)
                searchViewModel.generateSuggestions("")
            }
        }
    }

    private fun  setupBackButton(){
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun selectSearchType(searchType: SearchType){
        currentSearchType = searchType
        searchViewModel.setSearchType(searchType)

        val tabIndex = when (searchType) {
            SearchType.ALL -> 0
            SearchType.APPOINTMENT -> 1
            SearchType.CONTACT -> 2
            SearchType.NOTE -> 3
        }
        binding.tabSearchFilters.getTabAt(tabIndex)?.select()

        if (currentSearch.isNotEmpty()) {
            searchViewModel.search(currentSearch, searchType)
        }
    }

    private fun observeSearchData(){
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED){
                searchViewModel.uiState.collect { state ->
                    handleSearchUiState(state)
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                searchViewModel.suggestions.collect { suggestions ->
                    Log.d(TAG, "Suggestions updated: ${suggestions.size}")
                    searchSuggestionsAdapter.submitList(suggestions)
                }
            }
        }


        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                searchViewModel.searchResults.collect { results ->
                    searchResultsAdapter.submitSearchResults(results)
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                searchViewModel.currentQuery.collect { query ->
                    if (query != binding.etSearch.text.toString()) {
                        isUpdatingSearch = true
                        binding.etSearch.setText(query)
                        binding.etSearch.setSelection(query.length)
                        isUpdatingSearch = false
                    }
                }
            }
        }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                searchViewModel.isSearching.collect { isSearching ->
                    binding.progressLoading.visibility =
                        if (isSearching) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun performSearch() {
        val query = binding.etSearch.text.toString().trim()
        if (query.isNotEmpty()) {
            currentSearch = query
            searchViewModel.searchImmediate(query, currentSearchType)
            hideKeyboard()
        }
    }

    private fun handleSearchUiState(state: SearchUiState) {
        when (state) {
            is SearchUiState.Idle -> {
                showSuggestions()
                binding.progressLoading.visibility = View.GONE
            }

            is SearchUiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
            }

            is SearchUiState.SuggestionsLoaded -> {
                binding.progressLoading.visibility = View.GONE
                showSuggestions()
            }

            is SearchUiState.LoadingSuggestions -> {

            }

            is SearchUiState.SearchResultsLoaded -> {
                showSearchResults(state.results.isNotEmpty())
                binding.progressLoading.visibility = View.GONE
            }

            is SearchUiState.Error -> {
                binding.progressLoading.visibility = View.GONE
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                showSuggestions()
            }

            is SearchUiState.SearchHistoryDeleted -> {
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }

            is SearchUiState.SearchHistoryCleared -> {
                Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun showSuggestions() {
        binding.apply {
            layoutSuggestions.visibility = View.VISIBLE
            layoutSearchResults.visibility = View.GONE
            layoutNoResults.visibility = View.GONE
        }
    }

    private fun showSearchResults(hasResults: Boolean) {
        binding.apply {
            layoutSuggestions.visibility = View.GONE
            layoutSearchResults.visibility = if (hasResults) View.VISIBLE else View.GONE
            layoutNoResults.visibility = if (hasResults) View.GONE else View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

//    override fun onResume() {
//        super.onResume()
//        Log.d(TAG, "onResume called - currentSearch: $currentSearch, currentSearchType: $currentSearchType")
//
//        lifecycleScope.launch {
//            val currentSuggestions = searchViewModel.suggestions.value
//            if (currentSuggestions.isEmpty() && currentSearch.isEmpty()) {
//                Log.d(TAG, "No suggestions found, force reload")
//                userViewModel.getCurrentUser().value?.let { user ->
//                    searchViewModel.initializeSearch(user.id)
//                }
//            }
//        }
//
//    }

//    override fun onPause() {
//        super.onPause()
//        Log.d(TAG, "onPause called")
//    }
}