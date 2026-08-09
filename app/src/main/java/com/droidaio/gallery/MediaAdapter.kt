package com.droidaio.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.droidaio.gallery.databinding.ItemMediaBinding
import com.droidaio.gallery.models.MediaItem

class MediaAdapter(private val onItemLongClick : (MediaItem, View) -> Unit) : RecyclerView.Adapter<MediaAdapter.VH>() {

    private val items = mutableListOf<MediaItem>()
    private val selected = mutableSetOf<Long>()

    fun submitList(list : List<MediaItem>) {
        items.clear()
        items.addAll(list)
        selected.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        selected.clear()
        items.forEach { selected.add(it.id) }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    fun getSelected() : List<MediaItem> = items.filter { selected.contains(it.id) }

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : VH {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder : VH, position : Int) {
        val item = items[position]
        holder.bind(item, selected.contains(item.id))
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item, it)
            true
        }
        holder.itemView.setOnClickListener {
            if (selected.isNotEmpty()) {
                toggleSelection(item)
            } else {
                MediaViewer.open(holder.itemView.context, item)
            }
        }
    }

    private fun toggleSelection(item : MediaItem) {
        if (selected.contains(item.id)) selected.remove(item.id) else selected.add(item.id)
        notifyDataSetChanged()
    }

    override fun getItemCount() : Int = items.size

    inner class VH(private val binding : ItemMediaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item : MediaItem, isSelected : Boolean) {
            Glide.with(binding.root).load(item.uri).centerCrop().into(binding.thumb)
            binding.check.isVisible = isSelected
        }
    }
}

