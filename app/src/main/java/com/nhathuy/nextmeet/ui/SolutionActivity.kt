package com.nhathuy.nextmeet.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.get
import androidx.viewpager2.widget.ViewPager2
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.ViewPagerAdapter
import com.nhathuy.nextmeet.databinding.ActivitySolutionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SolutionActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySolutionBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySolutionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager2.adapter = viewPagerAdapter

        // tắt scroll viewpager2 bằng tay
        binding.viewPager2.isUserInputEnabled  = false

        //đồng bộ viewpager2 với bottomNavigationItem
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.navBottomNavigation.menu.get(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.navBottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> binding.viewPager2.currentItem = 0
                R.id.nav_appointment -> binding.viewPager2.currentItem = 1
                R.id.nav_contact -> binding.viewPager2.currentItem = 2
                R.id.nav_settings -> binding.viewPager2.currentItem = 3
            }
            true
        }
    }
}