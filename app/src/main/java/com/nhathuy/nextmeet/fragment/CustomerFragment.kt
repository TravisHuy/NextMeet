package com.nhathuy.nextmeet.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.nextmeet.adapter.CustomerAdapter
import com.nhathuy.nextmeet.databinding.FragmentCustomerBinding
import com.nhathuy.nextmeet.model.Customer
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CustomerFragment : Fragment() {

    private var _binding: FragmentCustomerBinding? = null
    private val binding get() = _binding!!

    private val customerViewModel: CustomerViewModel by viewModels()

    private lateinit var customerAdapter: CustomerAdapter
    private var allCustomers: List<Customer> = emptyList()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            customerViewModel.allCustomerState.collect { result ->
                when (result) {
                    is Resource.Loading -> {

                    }

                    is Resource.Success -> {
                        result.data?.let {
                            customerAdapter.setData(it)
                            allCustomers = it
                        }
                    }

                    is Resource.Error -> {
                        Toast.makeText(
                            requireContext(),
                            "Loi ko lay duoc danh sach customer ${result.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        customerAdapter = CustomerAdapter(requireContext(), emptyList()) { customer ->
            showDeleteConfirmationDialog(customer)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = customerAdapter
        }
    }

    private fun showDeleteConfirmationDialog(customer: Customer) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete all customer?")
            .setPositiveButton("Delete") { _, _ ->
                customerViewModel.deleteAllCustomers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        customerViewModel.getAllCustomers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun searchCustomers(query: String?) {
        val filteredList = if (query.isNullOrBlank()) {
            allCustomers
        } else {
            allCustomers.filter { customer ->
                customer.name.contains(query, ignoreCase = true) ||
                        customer.name.split(" ").any {
                            it.contains(query, ignoreCase = true)
                        }
            }
        }
        customerAdapter.setData(filteredList)
    }

}
