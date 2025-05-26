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


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabsPagerAdapter: TabsPagerAdapter
    private var testActivity: TestActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TestActivity) {
            testActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        connectTabLayoutWithViewPager()
    }

    private fun setupViewPager() {
        tabsPagerAdapter = TabsPagerAdapter(childFragmentManager, lifecycle)
        binding.viewPager.adapter = tabsPagerAdapter

        // tắt swipe nêu không muon swipe giữa các tab
        binding.viewPager.isUserInputEnabled = false
    }

    private fun connectTabLayoutWithViewPager() {
        testActivity?.binding?.tabLayout?.let { tabLayout ->
            TabLayoutMediator(tabLayout, binding.viewPager) { tab, position ->
                when (position) {
                    0 -> tab.text = "Dashboard"
                    1 -> tab.text = "Notes"
                    2 -> tab.text = "History"
                }
            }.attach()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDetach() {
        super.onDetach()
        testActivity = null
    }

}