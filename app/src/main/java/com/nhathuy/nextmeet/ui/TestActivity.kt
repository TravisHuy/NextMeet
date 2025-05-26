package com.nhathuy.nextmeet.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.TabsPagerAdapter
import com.nhathuy.nextmeet.databinding.ActivityTestBinding
import com.nhathuy.nextmeet.fragment.DashBoardFragment
import com.nhathuy.nextmeet.fragment.HistoryFragment
import com.nhathuy.nextmeet.fragment.NotesFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : AppCompatActivity() {
    lateinit var binding: ActivityTestBinding
    private lateinit var viewPagerAdapter: TabsPagerAdapter

    //enum để quan lý tab
    enum class TabType(val position: Int, val title: String) {
        DASHBOARD(1, "Dashboard"),
        NOTES(0, "Notes"),
        HISTORY(2, "History")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupTabLayout()

    }

    private fun setupBottomNavigation() {
        binding.navBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showTabLayout()
                    binding.icAdd.visibility = View.GONE
                    true
                }
                R.id.nav_appointment -> {
                    hideTabLayout()
                    binding.icAdd.visibility = View.GONE
                    // Navigate to discover fragment
                    true
                }
                R.id.nav_contact -> {
                    // Handle add action like TikTok
                    hideTabLayout()
                    binding.icAdd.visibility = View.GONE
                    true // Don't select this tab
                }
                R.id.nav_settings-> {
                    hideTabLayout()
                    binding.icAdd.visibility = View.GONE
                    // Navigate to inbox fragment
                    true
                }
                else -> false
            }
        }

        // Set home as default selected
        binding.navBottomNavigation.selectedItemId = R.id.nav_home
    }
    private fun showTabLayout(){
        binding.topStatusBar.visibility = View.VISIBLE
        binding.viewPager2.visibility = View.VISIBLE
        binding.navHostFragmentPlus.visibility = View.GONE
    }
    private fun hideTabLayout(){
        binding.topStatusBar.visibility = View.GONE
        binding.viewPager2.visibility = View.GONE
        binding.navHostFragmentPlus.visibility = View.VISIBLE
    }
    private fun setupTabLayout() {
        // Setup ViewPager adapter
//        viewPagerAdapter = TabsPagerAdapter(this)
//        binding.viewPager2.adapter = viewPagerAdapter
//
//        // Connect TabLayout with ViewPager2
//        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
//            tab.text = when (position) {
//                0 -> "Dashboard"
//                1 -> "Notes"
//                2 -> "History"
//                else -> "Tab $position"
//            }
//        }.attach()
//
//        // Set Dashboard as default selected (position 0)
//        binding.viewPager2.setCurrentItem(0, false)
//
//        // Add page change callback for smooth scrolling
//        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                super.onPageSelected(position)
//                // Handle page selection if needed
//            }
//        })
    }
}