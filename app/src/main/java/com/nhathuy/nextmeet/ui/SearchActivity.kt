package com.nhathuy.nextmeet.ui

import android.app.ComponentCaller
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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
import java.util.Locale

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
        const val VOICE_SEARCH_REQUEST_CODE = 1001
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

                    updateSearchBarButtons()

                    if (query.isEmpty()) {
                        searchViewModel.resetToInitialState()
                    } else {
                        searchViewModel.generateSuggestions(query)
                    }
                }

            })

            etSearch.setOnEditorActionListener {
                _,actionId,_ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch()
                    true
                } else false
            }

            etSearch.setOnFocusChangeListener { _, hasFocus ->
                updateSearchBarButtons()
            }

            btnClear.setOnClickListener {
                clearSearch()
            }

            btnVoiceSearch.setOnClickListener {
                startVoiceSearch()
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
                        val newSearchType = searchType as SearchType

                        // Chỉ thực hiện search nếu search type thay đổi
                        if (currentSearchType != newSearchType) {
                            currentSearchType = newSearchType
                            searchViewModel.setSearchType(currentSearchType)

                            // Nếu có query hiện tại, search với type mới
                            if (currentSearch.isNotEmpty()) {
                                Log.d(
                                    TAG,
                                    "Tab changed to $newSearchType with query: $currentSearch"
                                )
                                searchViewModel.search(currentSearch, currentSearchType)
                            } else {
                                // Nếu không có query, có thể hiển thị empty state hoặc suggestions
                                handleEmptyQueryTabChange()
                            }

                            // Update clear button visibility khi chuyển tab
                            updateClearButtonVisibility()
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
        if (currentSearch.isEmpty()) {
            handleEmptyQueryTabChange()
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
                updateSearchBarButtons()
            }

            is SearchUiState.Loading -> {
                binding.progressLoading.visibility = View.VISIBLE
            }

            is SearchUiState.SuggestionsLoaded -> {
                binding.progressLoading.visibility = View.GONE
                showSuggestions()
                updateSearchBarButtons()
            }

            is SearchUiState.LoadingSuggestions -> {

            }

            is SearchUiState.SearchResultsLoaded -> {
                showSearchResults(state.results.isNotEmpty())
                binding.progressLoading.visibility = View.GONE
                updateSearchBarButtons()
            }

            is SearchUiState.Error -> {
                binding.progressLoading.visibility = View.GONE
                Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                showSuggestions()
                updateSearchBarButtons()
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

            if (!hasResults) {
                updateNoResultsMessage()
            }
        }
    }
    private fun updateNoResultsMessage() {
        val message = when (currentSearchType) {
            SearchType.ALL -> "Không tìm thấy kết quả nào"
            SearchType.APPOINTMENT -> "Không tìm thấy cuộc hẹn nào"
            SearchType.CONTACT -> "Không tìm thấy liên hệ nào"
            SearchType.NOTE -> "Không tìm thấy ghi chú nào"
        }

        val suggestion = when (currentSearchType) {
            SearchType.ALL -> "Hãy thử tìm kiếm với từ khóa khác"
            SearchType.APPOINTMENT -> "Hãy thử tìm kiếm cuộc hẹn khác"
            SearchType.CONTACT -> "Hãy thử tìm kiếm liên hệ khác"
            SearchType.NOTE -> "Hãy thử tìm kiếm ghi chú khác"
        }

        // Cập nhật text trong layout no results nếu cần
        // Bạn có thể thêm TextView có id để update message này
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }
    private fun updateSearchBarButtons(){
        val hasText = binding.etSearch.text.toString().trim().isNotEmpty()
        val hasFocus = binding.etSearch.hasFocus()
        val isInSearchMode = isInSearchMode()

        binding.apply {
            btnVoiceSearch.visibility = when {
                hasText -> View.GONE  // Ẩn khi có text
                isInSearchMode  -> View.GONE  // Ẩn khi đang trong search results
                else -> View.VISIBLE  // Hiển thị khi trống và đang trong suggestion mode
            }


            btnClear.visibility = when {
                hasText -> View.VISIBLE  // Hiển thị khi có text
                isInSearchMode  -> View.VISIBLE  // Hiển thị khi đang trong search results
                else -> View.GONE
            }


            etSearch.hint = if (hasFocus && !hasText) {
                "Nói để tìm kiếm hoặc nhập từ khóa"
            } else {
                getString(R.string.search_homes)
            }
        }
    }

    private fun startVoiceSearch(){
        if(!isVoiceSearchAvailable()){
            Toast.makeText(this, "Thiết bị không hỗ trợ tìm kiếm bằng giọng nói", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói để tìm kiếm...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE)
        }
        catch (e: Exception){
            Toast.makeText(this, "Không thể khởi động tìm kiếm bằng giọng nói", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error starting voice search", e)
        }
    }

    private fun isVoiceSearchAvailable(): Boolean {
        val pm = packageManager
        val activities = pm.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        )
        return activities.isNotEmpty()
    }

    private fun updateClearButtonVisibility() {
        val currentUiState = searchViewModel.uiState.value
        val hasText = binding.etSearch.text.toString().trim().isNotEmpty()
        val isShowingResults = currentUiState is SearchUiState.SearchResultsLoaded

        binding.btnClear.visibility = if (hasText || isShowingResults) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        if(resultCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK){
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.firstOrNull()?.let { voiceQuery ->
                // Cập nhật EditText với kết quả voice search
                isUpdatingSearch = true
                binding.etSearch.setText(voiceQuery)
                binding.etSearch.setSelection(voiceQuery.length)
                isUpdatingSearch = false

                // Thực hiện tìm kiếm
                currentSearch = voiceQuery
                searchViewModel.searchImmediate(voiceQuery, currentSearchType)

                // Cập nhật UI
                updateSearchBarButtons()
            }
        }
    }

    // Hàm clear search được cải thiện
    private fun clearSearch() {
        isUpdatingSearch = true
        binding.etSearch.setText("")
        binding.etSearch.clearFocus()
        isUpdatingSearch = false

        currentSearch = ""
        currentSearchType = SearchType.ALL

        // Reset về tab đầu tiên
        binding.tabSearchFilters.getTabAt(0)?.select()

        // Reset về state ban đầu
        searchViewModel.resetToInitialState()

        // Hide keyboard
        hideKeyboard()

        // Update clear button visibility
        updateClearButtonVisibility()
    }

    private fun handleEmptyQueryTabChange() {
        searchViewModel.resetToInitialState()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    fun isInSearchMode(): Boolean {
        return currentSearch.isNotEmpty() ||
                searchViewModel.uiState.value is SearchUiState.SearchResultsLoaded
    }

    override fun onBackPressed() {
        when {
            isInSearchMode() -> {
                clearSearch()
            }
            else -> {
                super.onBackPressed()
            }
        }
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