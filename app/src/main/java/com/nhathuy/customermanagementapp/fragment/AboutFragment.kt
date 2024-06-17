package com.nhathuy.customermanagementapp.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.databinding.FragmentAboutBinding

class AboutFragment : Fragment() {
    private lateinit var binding: FragmentAboutBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentAboutBinding.inflate(layoutInflater)


        binding.facebook.setOnClickListener{
            openUrl("https://www.facebook.com/honhathuy.travishuy")
        }
        binding.github.setOnClickListener{
            openUrl("https://github.com/TravisHuy")
        }
        binding.linkedin.setOnClickListener{
            openUrl("https://www.linkedin.com/in/honhathuy/")
        }
        return binding.root
    }

    private fun openUrl(url: String) {
        val intent=Intent(Intent.ACTION_VIEW)
        intent.data=Uri.parse(url)
        startActivity(intent)
    }

}