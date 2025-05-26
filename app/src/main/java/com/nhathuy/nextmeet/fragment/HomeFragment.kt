package com.nhathuy.nextmeet.fragment

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.tabs.TabLayoutMediator
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.TabsPagerAdapter
import com.nhathuy.nextmeet.databinding.FragmentHomeBinding
import com.nhathuy.nextmeet.ui.TestActivity
import dagger.hilt.android.AndroidEntryPoint


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TabsPagerAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        adapter = TabsPagerAdapter(this)
        binding.homeViewpager2.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.homeViewpager2) { tab, position ->
            tab.text = when (position) {
                0 -> "Dashboard"
                1 -> "Notes"
                2 -> "History"
                else -> "Tab"
            }
        }.attach()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPagerAdapter()
        setupTablayout()
    }

    private fun setupViewPagerAdapter() {

    }

    private fun setupTablayout() {

    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}