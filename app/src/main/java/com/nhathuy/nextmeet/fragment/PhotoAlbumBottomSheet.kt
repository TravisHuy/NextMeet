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

class PhotoAlbumBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetPhotoAlbumBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoAdapter: PhotoAdapter
    private var onPhotosSelected: ((List<Photo>) -> Unit)? = null
    private val selectedPhotos = mutableListOf<Photo>()
    private var isLoading = false

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
                behavior.peekHeight = resources.displayMetrics.heightPixels / 2
                behavior.isHideable = true
                behavior.skipCollapsed = false

                val layoutParams = it.layoutParams
                layoutParams.height = resources.displayMetrics.heightPixels
                it.layoutParams = layoutParams

                behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        when (newState) {
                            BottomSheetBehavior.STATE_EXPANDED -> {
                            }
                            BottomSheetBehavior.STATE_COLLAPSED -> {
                            }
                            BottomSheetBehavior.STATE_HIDDEN -> {
                                dismiss()
                            }
                        }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // Optional: Add slide animations
                    }
                })
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
                when (tab?.position) {
                    0 -> {
                        binding.tvEmptyState.text = getString(R.string.no_photos_found)
                        if (hasStoragePermission()) {
                            loadPhotos()
                        }
                    }
                    1 -> {
                        binding.tvEmptyState.text = getString(R.string.no_albums_found)
                        if (hasStoragePermission()) {
                            loadAlbums()
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
                Toast.makeText(context, "Please select at least one photo", Toast.LENGTH_SHORT).show()
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

        try {
            val photos = MediaStoreHelper.getImagesFromGallery(requireContext())
            showLoading(false)

            if (photos.isNotEmpty()) {
                photoAdapter.updatePhotos(photos)
                showEmptyState(false)
            } else {
                showEmptyState(true)
            }
        } catch (e: Exception) {
            showLoading(false)
            showError("Failed to load photos: ${e.message}")
        }
    }

    private fun loadAlbums() {
        if (isLoading) return

        showLoading(true)

        try {
            val photos = MediaStoreHelper.getImagesFromGallery(requireContext())
            showLoading(false)

            if (photos.isNotEmpty()) {
                photoAdapter.updatePhotos(photos)
                showEmptyState(false)
            } else {
                showEmptyState(true)
            }
        } catch (e: Exception) {
            showLoading(false)
            showError("Failed to load albums: ${e.message}")
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
            "$count selected"
        } else {
            "Select photos"
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