package com.nhathuy.customermanagementapp.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.adapter.CustomerAdapter
import com.nhathuy.customermanagementapp.databinding.FragmentCustomerBinding
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.resource.Resource
import com.nhathuy.customermanagementapp.ui.CustomerDetailActivity
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CustomerFragment : Fragment() {

    private var _binding: FragmentCustomerBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var customerViewModel: CustomerViewModel

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

        setupRecylerView()
        observerViewModel()
    }

    private fun observerViewModel() {
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

    private fun setupRecylerView() {
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
            .setPositiveButton("Delete") { dialog, which ->
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
    }

    fun searchCustomers(query: String?) {
        if (query.isNullOrBlank()) {
            customerAdapter.setData(allCustomers)
        } else {
            val filteredList = allCustomers.filter { customer ->
                customer.name.contains(query, ignoreCase = false)
                customer.name.split(" ").any {
                    it.contains(query, ignoreCase = true)
                }
            }
            customerAdapter.setData(filteredList)
        }
    }

}
