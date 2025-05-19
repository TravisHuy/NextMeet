package com.nhathuy.nextmeet.adapter

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.nhathuy.nextmeet.R
import com.nhathuy.nextmeet.model.Transaction
import com.nhathuy.nextmeet.resource.Resource
import com.nhathuy.nextmeet.viewmodel.CustomerViewModel
import com.nhathuy.nextmeet.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private var listTransaction: List<Transaction>,
    private val customerViewModel: CustomerViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val transactionViewModel: TransactionViewModel,
    private val onSelectionChanged: (Boolean) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private var selectItems = mutableSetOf<Transaction>()
    var isSelectionMode = false

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val customerName: TextView = itemView.findViewById(R.id.name)
        val productOrService: TextView = itemView.findViewById(R.id.productName)
        val quantity: TextView = itemView.findViewById(R.id.quantity)
        val price: TextView = itemView.findViewById(R.id.price)
        val date: TextView = itemView.findViewById(R.id.date)

        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val threeDotMenu: ImageView = itemView.findViewById(R.id.three_dot_menu)

        init {
            itemView.setOnLongClickListener {
                toggleSelectionMode(adapterPosition)
                true
            }
            itemView.setOnClickListener {
                if(isSelectionMode){
                    toggleSelection(adapterPosition)
                }
            }
            threeDotMenu.setOnClickListener {
                showOptionsDialog(listTransaction[adapterPosition])
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView =
            LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = listTransaction[position]
        val isSelected = selectItems.contains(transaction)
        holder.productOrService.text = transaction.productOrService
        holder.quantity.text = transaction.quantity.toString()
        holder.price.text = transaction.price.toString()
        holder.date.text = transaction.date

        (holder.itemView.context as? LifecycleOwner)?.let { lifecycleOwner ->
            lifecycleOwner.lifecycleScope.launch {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    customerViewModel.getCustomerById(transaction.customerId)
                    customerViewModel.getCustomerById.collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                holder.customerName.text = result.data?.name ?: "Unknown"
                            }
                            else -> {
                                // Keep the default "Unknown" text
                            }
                        }
                    }
                }
            }
        }

        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = isSelected
        holder.threeDotMenu.visibility = if (isSelectionMode) View.VISIBLE else View.GONE

    }



    override fun getItemCount(): Int {
        return listTransaction.size
    }



    fun setData(transactions: List<Transaction>) {
        listTransaction = transactions
        notifyDataSetChanged()
    }

    fun getSelectItems(): List<Transaction> {
        return selectItems.toList()
    }
    private fun toggleSelectionMode(position: Int) {
        isSelectionMode=!isSelectionMode
        toggleSelection(position)
        onSelectionChanged(isSelectionMode)
        notifyDataSetChanged()
    }
    fun clearSelection() {
        selectItems.clear()
        isSelectionMode = false
        onSelectionChanged(false)
        notifyDataSetChanged()
    }

    fun toggleSelection(position: Int) {
        val transaction= listTransaction[position]
        if(selectItems.contains(transaction)){
            selectItems.remove(transaction)
        }
        else{
            selectItems.add(transaction)
        }
        notifyItemChanged(position)

        if(selectItems.isEmpty()){
            isSelectionMode=false
            onSelectionChanged(false)
            notifyDataSetChanged()
        }
        else{
            updateSelectMode()
        }
    }

    private fun updateSelectMode() {
        isSelectionMode = selectItems.isNotEmpty()
        onSelectionChanged(isSelectionMode)
        notifyDataSetChanged()
    }

    // show option dialog
    private fun showOptionsDialog(transaction: Transaction) {
        val options = if (selectItems.size > 1) {
            arrayOf("Delete", "Delete All")
        } else {
            arrayOf("Edit", "Delete", "Delete All")
        }

        MaterialAlertDialogBuilder(context)
            .setItems(options) { _, position ->
                when {
                    options[position] == "Edit" -> onEditClick(transaction)
                    options[position] == "Delete" -> onDeleteClick(transaction)
                    options[position] == "Delete All" -> onDeleteAllClick()
                }
            }
            .show()

    }
    // fun edit transaction
    private fun onEditClick(transaction: Transaction) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.edit_transaction)


        val productService =dialog.findViewById<TextInputEditText>(R.id.edit_transaction_product_name)
        val quantity =dialog.findViewById<TextInputEditText>(R.id.edit_transaction_quantity)
        val price =dialog.findViewById<TextInputEditText>(R.id.edit_transaction_price)
        val dateEditText =dialog.findViewById<TextInputEditText>(R.id.edit_transaction_date)
        val dateLayout = dialog.findViewById<TextInputLayout>(R.id.edit_transaction_date_layout)
        val edit = dialog.findViewById<Button>(R.id.btn_edit_transaction)


        productService.setText(transaction.productOrService)
        quantity.setText(transaction.quantity.toString())
        price.setText(transaction.price.toString())
        dateEditText.setText(transaction.date)


        // Set up date picker
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(context, { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateEditText.setText(dateFormat.format(calendar.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        dateLayout.setEndIconOnClickListener {
            datePickerDialog.show()
        }

        dateEditText.setOnClickListener {
            datePickerDialog.show()
        }


        edit.setOnClickListener {
            val updatedTransaction = Transaction(
                id = transaction.id,
                userId = transaction.userId,
                customerId = transaction.customerId,
                productOrService = productService.text.toString(),
                quantity = quantity.text.toString().toIntOrNull() ?: 0,
                price = price.text.toString().toDoubleOrNull() ?: 0.0,
                date = dateEditText.text.toString()
            )

            transactionViewModel.editTransaction(updatedTransaction)
            Toast.makeText(context, "Transaction updated successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.attributes?.windowAnimations=R.style.DialogAnimation;
        dialog.window?.setGravity(Gravity.CENTER_HORIZONTAL)
    }

    // fun delete one transaction
    private fun onDeleteClick(transaction: Transaction) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Transaction(s)")
            .setMessage("Are you sure you want to delete this transaction(s)")
            .setPositiveButton("Delete") { _, _ ->
                selectItems.forEach { transaction ->
                    transactionViewModel.deleteTransaction(transaction)
                }
                clearSelection()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // fun delete all transactions
    private fun onDeleteAllClick() {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete All Transactions")
            .setMessage("Are you sure you want to delete all transactions?")
            .setPositiveButton("Delete") { _, _ ->
                transactionViewModel.deleteAllTransactions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
