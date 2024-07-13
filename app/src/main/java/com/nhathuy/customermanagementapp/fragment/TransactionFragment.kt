package com.nhathuy.customermanagementapp.fragment

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.adapter.TransactionAdapter
import com.nhathuy.customermanagementapp.databinding.FragmentTransactionBinding
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel

class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var customerViewModel: CustomerViewModel
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transactionViewModel = ViewModelProvider(this).get(TransactionViewModel::class.java)
        customerViewModel = ViewModelProvider(this).get(CustomerViewModel::class.java)

        setupRecyclerView()
        observeViewModel()
    }

    private fun observeViewModel() {
        transactionViewModel.getAllTransactions().observe(viewLifecycleOwner) { transactions ->
            transactions?.let {
                transactionAdapter.setData(it)
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            requireContext(),
            emptyList(),
            customerViewModel,
            transactionViewModel,
            onSelectionChanged = { isInSelectionMode ->

            }
        )
        binding.recTransaction.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}