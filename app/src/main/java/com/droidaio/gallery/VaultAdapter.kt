package com.droidaio.gallery

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Simple RecyclerView adapter for VaultFile items.
 * - submitList(files) updates the adapter contents
 * - getSelected() returns the selected VaultFile list
 *
 * Uses android.R.layout.simple_list_item_multiple_choice for a minimal selectable row.
 */
class VaultAdapter : RecyclerView.Adapter<VaultAdapter.VaultViewHolder>() {

    private val items = mutableListOf<VaultFile>()
    private val selectedPositions = mutableSetOf<Int>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(files: List<VaultFile>) {
        items.clear()
        items.addAll(files)
        selectedPositions.clear()
        notifyDataSetChanged()
    }

    fun getSelected(): List<VaultFile> {
        return selectedPositions.mapNotNull { pos -> items.getOrNull(pos) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        // Use a built-in layout that supports checked state
        val view =
            inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false)
        return VaultViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaultViewHolder, position: Int) {
        val file = items[position]
        holder.bind(file, selectedPositions.contains(position))
        holder.itemView.setOnClickListener {
            toggleSelection(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleSelection(position: Int) {
        if (selectedPositions.contains(position)) selectedPositions.remove(position) else selectedPositions.add(
            position
        )
    }

    class VaultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkedText: CheckedTextView = itemView.findViewById(android.R.id.text1)

        fun bind(file: VaultFile, checked: Boolean) {
            checkedText.text = file.name
            checkedText.isChecked = checked
        }
    }
}
