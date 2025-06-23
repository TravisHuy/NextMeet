package com.nhathuy.nextmeet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.nhathuy.nextmeet.databinding.ItemColorPickerBinding


class ColorPickerAdapter(private val colorList: List<Int>,
                         private val colorNames: Map<Int,String>,
                         private val onColorSelected: (Int,String) -> Unit
) :RecyclerView.Adapter<ColorPickerAdapter.ColorPickerViewHolder>(){

    private var selectedPosition = 0

    inner class ColorPickerViewHolder(val binding: ItemColorPickerBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(resId: Int,isChecked:Boolean){
            binding.ivColor.setBackgroundResource(resId)
            binding.ivCheck.visibility = if(isChecked) View.VISIBLE else View.GONE
            itemView.setOnClickListener {

                if(adapterPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                if(adapterPosition != selectedPosition){
                    val previous = selectedPosition
                    selectedPosition = adapterPosition

                    val colorName = colorNames[resId] ?:"color_white"
                    notifyItemChanged(previous)
                    notifyItemChanged(selectedPosition)
                    onColorSelected(resId,colorName)
                }
            }
        }
    }

    fun setSelectedColor(colorName: String) {
        val colorResId = colorNames.entries.find { it.value == colorName }?.key
        if (colorResId != null) {
            val newPosition = colorList.indexOf(colorResId)
            if (newPosition != -1) {
                val previous = selectedPosition
                selectedPosition = newPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ColorPickerAdapter.ColorPickerViewHolder {
        val view = ItemColorPickerBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ColorPickerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorPickerAdapter.ColorPickerViewHolder, position: Int) {
        holder.bind(colorList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = colorList.size

}