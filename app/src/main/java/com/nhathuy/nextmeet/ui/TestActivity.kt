package com.nhathuy.nextmeet.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.tabs.TabLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.databinding.ActivityTestBinding
import com.nhathuy.nextmeet.fragment.DashBoardFragment
import com.nhathuy.nextmeet.fragment.HistoryFragment
import com.nhathuy.nextmeet.fragment.NotesFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : AppCompatActivity() {
    lateinit var binding: ActivityTestBinding
    lateinit var navController: NavController

    private var isTabSetupComplete = false

    //enum để quan lý tab
    enum class TabType(val position: Int, val title: String) {
        DASHBOARD(0, "Dashboard"),
        NOTES(1, "Notes"),
        HISTORY(2, "History")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityTestBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupNavigation()
            setupClickListeners()

            Log.d("TestActivity", "TestActivity created successfully")
        } catch (e: Exception) {
            Log.e("TestActivity", "Error in onCreate: ", e)
            // Có thể finish() activity nếu có lỗi critical
            finish()
        }
    }

    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_plus) as? NavHostFragment

            if (navHostFragment == null) {
                Log.e("TestActivity", "NavHostFragment not found")
                return
            }

            navController = navHostFragment.navController
            binding.navBottomNavigation.setupWithNavController(navController)

            binding.navBottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        if (navController.currentDestination?.id != R.id.nav_home) {
                            navController.navigate(R.id.nav_home)
                        }
                        true
                    }
                    R.id.nav_appointment -> {
                        if (navController.currentDestination?.id != R.id.nav_appointment) {
                            navController.navigate(R.id.nav_appointment)
                        }
                        true
                    }
                    R.id.nav_contact -> {
                        if (navController.currentDestination?.id != R.id.nav_contact) {
                            navController.navigate(R.id.nav_contact)
                        }
                        true
                    }
                    R.id.nav_settings -> {
                        if (navController.currentDestination?.id != R.id.nav_settings) {
                            navController.navigate(R.id.nav_settings)
                        }
                        true
                    }
                    else -> false
                }
            }

            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.nav_home -> {
                        showTabLayoutAndTab(true)
                        // Setup tab layout khi vào Home, nhưng chỉ setup 1 lần
                        if (!isTabSetupComplete) {
                            setupTabLayout()
                            isTabSetupComplete = true
                        }
                    }
                    else -> {
                        showTabLayoutAndTab(false)
                    }
                }
            }

            Log.d("TestActivity", "Navigation setup completed")
        } catch (e: Exception) {
            Log.e("TestActivity", "Error in setupNavigation: ", e)
        }
    }

    private fun showTabLayoutAndTab(visibility: Boolean) {
        binding.tabLayout.visibility = if (visibility) View.VISIBLE else View.GONE
        if (visibility) {
            binding.icAdd.show()
        } else {
            binding.icAdd.hide()
        }
    }

    private fun setupTabLayout() {
        try {
            // Clear existing tabs first
            binding.tabLayout.removeAllTabs()

            // Add tabs từ enum
            TabType.values().forEach { tabType ->
                binding.tabLayout.addTab(
                    binding.tabLayout.newTab().setText(tabType.title)
                )
            }

            // Set default tab (Dashboard)
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
            handleTabSelection(0) // Load dashboard fragment initially

            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let { selectedTab ->
                        if (navController.currentDestination?.id == R.id.nav_home) {
                            handleTabSelection(selectedTab.position)
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    // Do nothing
                }

                override fun onTabReselected(tab: TabLayout.Tab?) {
                    tab?.let { selectedTab ->
                        if (navController.currentDestination?.id == R.id.nav_home) {
                            refreshCurrentTab(selectedTab.position)
                        }
                    }
                }
            })

            Log.d("TestActivity", "TabLayout setup completed")
        } catch (e: Exception) {
            Log.e("TestActivity", "Error in setupTabLayout: ", e)
        }
    }

    private fun handleTabSelection(position: Int) {
        try {
            // Show tab content container
            binding.tabContentContainer.visibility = View.VISIBLE

            when(position) {
                TabType.DASHBOARD.position -> {
                    switchToFragment(DashBoardFragment())
                }
                TabType.NOTES.position -> {
                    switchToFragment(NotesFragment())
                }
                TabType.HISTORY.position -> {
                    switchToFragment(HistoryFragment())
                }
            }

            Log.d("TestActivity", "Tab selected: $position")
        } catch (e: Exception) {
            Log.e("TestActivity", "Error in handleTabSelection: ", e)
        }
    }

    private fun switchToFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.tab_content_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e("TestActivity", "Error switching fragment: ", e)
        }
    }

    private fun refreshCurrentTab(position: Int) {
        try {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.tab_content_container)
            when (position) {
                TabType.DASHBOARD.position -> {
                    (currentFragment as? DashBoardFragment)?.refreshData()
                }
                TabType.NOTES.position -> {
                    (currentFragment as? NotesFragment)?.refreshData()
                }
                TabType.HISTORY.position -> {
                    (currentFragment as? HistoryFragment)?.refreshData()
                }
            }
        } catch (e: Exception) {
            Log.e("TestActivity", "Error refreshing tab: ", e)
        }
    }

    private fun setupClickListeners() {
        // Add any click listeners here
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    fun getCurrentTabPosition(): Int {
        return binding.tabLayout.selectedTabPosition
    }

    fun setTabPosition(position: Int) {
        if (position in 0 until binding.tabLayout.tabCount) {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(position))
        }
    }

    fun switchToTab(tabType: TabType) {
        setTabPosition(tabType.position)
    }
}