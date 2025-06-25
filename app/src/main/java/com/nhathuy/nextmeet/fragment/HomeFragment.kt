package com.nhathuy.nextmeet.fragment

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.adapter.TabsPagerAdapter
import com.nhathuy.nextmeet.databinding.DialogAddGridLayoutBinding
import com.nhathuy.nextmeet.databinding.FragmentHomeBinding
import com.nhathuy.nextmeet.model.RegistrationForm
import com.nhathuy.nextmeet.ui.AddNoteActivity
import com.nhathuy.nextmeet.ui.GoogleMapActivity
import com.nhathuy.nextmeet.ui.TestActivity
import dagger.hilt.android.AndroidEntryPoint


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    val binding get() = _binding!!
    private lateinit var adapter: TabsPagerAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPagerAdapter()
        setupTabLayout()
        setupClickListener()
    }

    private fun setupViewPagerAdapter() {
        adapter = TabsPagerAdapter(this)
        binding.homeViewpager2.adapter = adapter
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.tabLayout, binding.homeViewpager2) { tab, position ->
            tab.text = when (position) {
                0 -> "Dashboard"
                1 -> "Notes"
                2 -> "History"
                else -> "Tab"
            }
        }.attach()

        binding.homeViewpager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position){
                    0->binding.btnAdd.visibility = View.VISIBLE
                    1->binding.btnAdd.visibility = View.GONE
                    2->binding.btnAdd.visibility = View.GONE
                }
            }
        })
    }
    private fun setupClickListener(){
        binding.btnAdd.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val dialogBinding = DialogAddGridLayoutBinding.inflate(LayoutInflater.from(requireContext()))
            dialog.setContentView(dialogBinding.root)

            dialogBinding.ivAddNotes.setOnClickListener {
                startActivity(Intent(requireContext(),AddNoteActivity::class.java))
            }

            dialogBinding.ivCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
            dialog.window?.apply {
                setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawableResource(R.drawable.border_dialog_background)
                attributes.windowAnimations = R.style.DialogAnimation
                setGravity(Gravity.CENTER_HORIZONTAL)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}