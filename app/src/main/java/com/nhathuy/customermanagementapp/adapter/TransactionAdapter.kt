package com.nhathuy.customermanagementapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhathuy.customermanagementapp.R
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.viewmodel.CustomerViewModel
import com.nhathuy.customermanagementapp.viewmodel.TransactionViewModel

class TransactionAdapter(
    private val context: Context,
    private var listTransaction: List<Transaction>,
    private val customerViewModel: CustomerViewModel,
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

        holder.productOrService.text = transaction.productOrService
        holder.quantity.text = transaction.quantity.toString()
        holder.price.text = transaction.price.toString()
        holder.date.text = transaction.date

        customerViewModel.getCustomerById(transaction.customerId)
            .observe(context as LifecycleOwner) { customer ->
                holder.customerName.text = customer?.name ?: "Unknown"
            }

        holder.checkBox.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
        holder.checkBox.isChecked = selectItems.contains(transaction)

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

    }
    // show option dialog
    private fun showOptionsDialog(transaction: Transaction) {
        MaterialAlertDialogBuilder(context)
            .setItems(arrayOf("Edit","Delete","Delete All")){
                _,position ->
                when(position){
                    0-> onEditClick(transaction)
                    1 ->  onDeleteClick(transaction)
                    2-> onDeleteAllClick()
                }
            }
            .show()

    }
    // fun edit transaction
    private fun onEditClick(transaction: Transaction) {
        
    }

    // fun delete one transaction
    private fun onDeleteClick(transaction: Transaction) {
        MaterialAlertDialogBuilder(context)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction")
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
