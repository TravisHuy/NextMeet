package com.nhathuy.nextmeet.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.PhotoAdapter
import com.nhathuy.nextmeet.databinding.BottomSheetPhotoAlbumBinding
import com.nhathuy.nextmeet.model.Photo
import com.nhathuy.nextmeet.utils.MediaStoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoAlbumBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPhotoAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoAdapter: PhotoAdapter
    private var onPhotosSelected: ((List<Photo>) -> Unit)? = null
    private val selectedPhotos = mutableListOf<Photo>()
    private var isLoading = false
    private var allPhotos = listOf<Photo>()
    private var albumMap = mapOf<String,List<Photo>>()
    private var currentTab = 0

    private val loadingScope = CoroutineScope(Dispatchers.Main)

    // permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            loadPhotos()
        } else {
            showPermissionDeniedMessage()
        }
    }

    companion object {
        fun newInstance(onPhotosSelected: (List<Photo>) -> Unit): PhotoAlbumBottomSheet {
            return PhotoAlbumBottomSheet().apply {
                this.onPhotosSelected = onPhotosSelected
            }
        }
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?
    ): android.view.View? {
        _binding = BottomSheetPhotoAlbumBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheetBehavior()
        setupRecyclerView()
        setupTabs()
        setupClickListeners()

        checkPermissionsAndLoadPhotos()
    }

    private fun setupBottomSheetBehavior() {
        dialog?.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.6).toInt()
                behavior.isHideable = true
                behavior.skipCollapsed = false

                val layoutParams = it.layoutParams
                layoutParams.height = resources.displayMetrics.heightPixels
                it.layoutParams = layoutParams
            }
        }
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter { photo ->
            togglePhotoSelection(photo)
        }
        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(context,3)
            adapter = photoAdapter
        }
    }
    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.image)))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.album)))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                when (currentTab) {
                    0 -> {
                        binding.tvEmptyState.text = getString(R.string.no_photos_found)
                        if (hasStoragePermission()) {
                            displayPhotos()
                        }
                    }
                    1 -> {
                        binding.tvEmptyState.text = getString(R.string.no_albums_found)
                        if (hasStoragePermission()) {
                            displayAlbums()
                        }
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}

        })
    }

    private fun setupClickListeners(){
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnDone.setOnClickListener {
            if (selectedPhotos.isNotEmpty()) {
                onPhotosSelected?.invoke(selectedPhotos.toList()) // Copy list to avoid reference issues
                dismiss()
            } else {
                Toast.makeText(context,
                    getString(R.string.please_select_at_least_one_photo), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSelectAll.setOnClickListener {
            // FIX: Xử lý select all / deselect all
            if (selectedPhotos.size == photoAdapter.getCurrentPhotos().size) {
                // Nếu đã select all thì deselect all
                deselectAllPhotos()
            } else {
                // Nếu chưa select all thì select all
                selectAllPhotos()
            }
        }
    }

    private fun checkPermissionsAndLoadPhotos() {
        if (hasStoragePermission()) {
            loadPhotos()
        } else {
            requestStoragePermission()
        }
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        permissionLauncher.launch(permissions)
    }


    private fun loadPhotos() {
        if (isLoading) return

        showLoading(true)

        loadingScope.launch {
            try {
                // Load cả photos và albums song song
                val photos = MediaStoreHelper.getImagesFromGallery(requireContext())
                val albums = MediaStoreHelper.getImagesByAlbum(requireContext())

                withContext(Dispatchers.Main) {
                    allPhotos = photos
                    albumMap = albums

                    showLoading(false)

                    // Display data based on current tab
                    when (currentTab) {
                        0 -> displayPhotos()
                        1 -> displayAlbums()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError(getString(R.string.failed_to_load_photos, e.message))
                }
            }
        }
    }

    private fun displayPhotos() {
        if (allPhotos.isNotEmpty()) {
            photoAdapter.updatePhotos(allPhotos)
            showEmptyState(false)
            updateSelectionUI()
        } else {
            showEmptyState(true)
        }
    }

    private fun displayAlbums() {
        if (allPhotos.isNotEmpty()) {
            photoAdapter.updatePhotos(allPhotos)
            showEmptyState(false)
            updateSelectionUI()
        } else {
            showEmptyState(true)
        }
    }

    private fun togglePhotoSelection(photo: Photo) {
        if (selectedPhotos.contains(photo)) {
            selectedPhotos.remove(photo)
        } else {
            selectedPhotos.add(photo)
        }

        updateSelectionUI()
        photoAdapter.updateSelectedPhotos(selectedPhotos)
    }

    private fun selectAllPhotos() {
        val currentPhotos = photoAdapter.getCurrentPhotos()
        selectedPhotos.clear()
        selectedPhotos.addAll(currentPhotos)

        updateSelectionUI()
        photoAdapter.updateSelectedPhotos(selectedPhotos)
    }

    private fun deselectAllPhotos() {
        selectedPhotos.clear()
        updateSelectionUI()
        photoAdapter.updateSelectedPhotos(selectedPhotos)
    }

    private fun updateSelectionUI() {
        val count = selectedPhotos.size
        val totalPhotos = photoAdapter.getCurrentPhotos().size

        // Update count text
        binding.tvSelectedCount.text = if (count > 0) {
            getString(R.string.selected_photo, count)
        } else {
            getString(R.string.select_photos)
        }

        if(count > 0){
            // Update done button state
            binding.btnDone.isEnabled = true
            binding.btnDone.alpha = 1.0f
        }
        else {
            binding.btnDone.isEnabled = false
            binding.btnDone.alpha = 0.5f
        }


        if (totalPhotos > 0) {
            binding.btnSelectAll.text = if (count == totalPhotos) {
                getString(R.string.deselect_all)
            } else {
                getString(R.string.select_all)
            }
            binding.btnSelectAll.isEnabled = true
        } else {
            binding.btnSelectAll.isEnabled = false
        }
    }

    private fun showLoading(isLoading: Boolean) {
        this.isLoading = isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewPhotos.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState(show: Boolean) {
        binding.tvEmptyState.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerViewPhotos.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun showPermissionDeniedMessage() {
        binding.tvEmptyState.text = getString(R.string.permission_required_message)
        showEmptyState(true)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}