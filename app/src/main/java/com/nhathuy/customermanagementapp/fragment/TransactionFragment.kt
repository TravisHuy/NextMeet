package com.nhathuy.customermanagementapp.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.nhathuy.customermanagementapp.adapter.AppointmentAdapter
import com.nhathuy.customermanagementapp.adapter.TransactionAdapter
import com.nhathuy.customermanagementapp.databinding.FragmentTransactionBinding
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel


class TransactionFragment : Fragment() {


    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var transactionAdapter: TransactionAdapter


    private lateinit var customerViewModel: CustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentTransactionBinding.inflate(layoutInflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel= ViewModelProvider(this).get(TransactionViewModel::class.java)
        customerViewModel= ViewModelProvider(this).get(CustomerViewModel::class.java)

        setupRecyclerView()
        observerViewModel()
    }

    private fun observerViewModel() {
        transactionViewModel.getAllTransactions().observe(viewLifecycleOwner,{
                transactions -> transactions?.let {
            transactionAdapter.setData(it)
             }
        })
    }

    private fun setupRecyclerView() {
        transactionAdapter= TransactionAdapter(requireContext(), emptyList(),customerViewModel)
        binding.recTransaction.apply {
            layoutManager= LinearLayoutManager(requireContext())
            adapter=transactionAdapter
        }
    }
}