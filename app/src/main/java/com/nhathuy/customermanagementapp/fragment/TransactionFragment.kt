package com.nhathuy.customermanagementapp.fragment

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.adapter.TransactionAdapter
import com.nhathuy.customermanagementapp.databinding.FragmentTransactionBinding
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by viewModels()

    private val customerViewModel: CustomerViewModel by viewModels()

    private lateinit var transactionAdapter: TransactionAdapter

    private var allTransactions: List<Transaction> = emptyList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            transactionViewModel.allTransactionState.collect { result ->
                when (result){
                    is Resource.Loading -> {

                    }
                    is Resource.Success -> {
                        result.data?.let {
                            transactionAdapter.setData(it)
                            allTransactions = it

                        }
                    }
                    is Resource.Error -> {
                        Toast.makeText(requireContext(),"Loi ko lay duoc danh sach transaction ${result.message}"  ,Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            requireContext(),
            emptyList(),
            customerViewModel,
            viewLifecycleOwner,
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

    fun searchTransactions(query: String?) {
        if (query.isNullOrBlank()) {
            transactionAdapter.setData(allTransactions)
        } else {
            val filteredList = allTransactions.filter { transaction ->
                transaction.productOrService.contains(query, ignoreCase = false)
                transaction.productOrService.split(" ").any {
                    it.contains(query, ignoreCase = true)
                }
            }
            transactionAdapter.setData(filteredList)
        }
    }
}