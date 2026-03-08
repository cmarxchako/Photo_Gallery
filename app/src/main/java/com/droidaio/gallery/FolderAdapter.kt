package com.droidaio.gallery

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.droidaio.gallery.databinding.ItemFolderBinding
import com.droidaio.gallery.models.FolderInfo

/**
 * RecyclerView Adapter for displaying folders in the folder selection screen.
 * Each item shows a thumbnail (sampleUri) and a selection indicator (simple ImageView toggle).
 * The adapter manages a list of FolderInfo items and a set of selected folder ids.
 * It provides methods to submit a new list of folders, get selected folder ids, and toggle selection.
 * In a production app, you would likely want to add more features such as:
 * - Displaying folder names and item counts
 * - Providing better UI/UX for selection (e.g. checkboxes, multi-select mode)
 * - Handling edge cases (e.g. no folders found, errors loading thumbnails)
 */
class FolderAdapter(
    private val onCheckedChanged : (folderId : String, checked : Boolean) -> Unit,
) : RecyclerView.Adapter<FolderAdapter.VH>() {

    private val items = mutableListOf<FolderInfo>()
    private val selected = mutableSetOf<String>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list : List<FolderInfo>, saved : Set<String>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getSelectedFolderIds() : List<String> = selected.toList()

    override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : VH {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder : VH, position : Int) {
        val item = items[position]
        val folderKey = item.bucketId ?: item.id.toString()
        holder.bind(item, selected.contains(folderKey))
        holder.itemView.setOnClickListener {
            val newChecked = if (selected.contains(folderKey)) {
                selected.remove(folderKey); false
            } else {
                selected.add(folderKey); true
            }
            notifyItemChanged(position)
            onCheckedChanged(folderKey, newChecked)
        }
    }

    override fun getItemCount() : Int = items.size

    inner class VH(val binding : ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item : FolderInfo, isSelected : Boolean) {
            // thumbnail
            Glide.with(binding.root)
                .load(item.sampleUri)
                .centerCrop()
                .into(binding.thumb)

            // selection indicator (simple ImageView toggle in item_folder.xml)
            binding.check.visibility = if (isSelected) View.VISIBLE else View.GONE
        }
    }

    // ######################################################################################
    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selected.clear()
        items.forEach { it.bucketId?.let { it1 -> selected.add(it1) } }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun invertSelection() {
        val newSet = mutableSetOf<String>()
        items.forEach {
            if (!selected.contains(it.bucketId)) it.bucketId?.let { it1 -> newSet.add(it1) }
        }
        selected.clear()
        selected.addAll(newSet)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(bucketId : String) {
        if (selected.contains(bucketId)) selected.remove(bucketId) else selected.add(bucketId)
        notifyDataSetChanged()
    }
    // ######################################################################################
}

/**
class FolderAdapter(
private val onCheckedChanged : (folderId : String, checked : Boolean) -> Unit,
) : RecyclerView.Adapter<FolderAdapter.VH>() {

private val items = mutableListOf<FolderInfo>()
private val selected = mutableSetOf<String>()

@SuppressLint("NotifyDataSetChanged")
fun submitList(list : List<FolderInfo>, preselected : Set<String>) {
items.clear()
items.addAll(list)
selected.clear()
selected.addAll(preselected)
notifyDataSetChanged()
}

fun getSelectedFolderIds() : Set<String> = selected.toSet()

@SuppressLint("NotifyDataSetChanged")
fun clearSelection() {
selected.clear()
notifyDataSetChanged()
}

@SuppressLint("NotifyDataSetChanged")
fun selectAll() {
selected.clear()
items.forEach { it.bucketId?.let { it1 -> selected.add(it1) } }
notifyDataSetChanged()
}

@SuppressLint("NotifyDataSetChanged")
fun invertSelection() {
val newSet = mutableSetOf<String>()
items.forEach {
if (!selected.contains(it.bucketId)) it.bucketId?.let { it1 -> newSet.add(it1) }
}
selected.clear()
selected.addAll(newSet)
notifyDataSetChanged()
}

override fun onCreateViewHolder(parent : ViewGroup, viewType : Int) : VH {
val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
return VH(binding)
}

override fun onBindViewHolder(holder : VH, position : Int) {
val item = items[position]
holder.bind(item, selected.contains(item.bucketId))
holder.binding.root.setOnClickListener {
item.bucketId?.let { it1 -> toggleSelection(it1) }
}
holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
if (isChecked) item.bucketId?.let { selected.add(it) } else selected.remove(item.bucketId)
item.bucketId?.let { onCheckedChanged(it, isChecked) }
}
}

@SuppressLint("NotifyDataSetChanged")
private fun toggleSelection(bucketId : String) {
if (selected.contains(bucketId)) selected.remove(bucketId) else selected.add(bucketId)
notifyDataSetChanged()
}

override fun getItemCount() : Int = items.size

inner class VH(val binding : ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
fun bind(item : FolderInfo, isSelected : Boolean) {
binding.name.text = item.displayName
binding.checkbox.isChecked = isSelected
binding.countPlaceholder.text = "${item.count} items"
binding.countPlaceholder.visibility = if (item.count > 0) android.view.View.VISIBLE else android.view.View.GONE
Glide.with(binding.root).load(item.sampleUri).centerCrop().into(binding.thumb)
}
}
}

 */
