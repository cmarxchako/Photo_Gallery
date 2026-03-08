package com.droidaio.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.droidaio.gallery.databinding.FragmentFolderSelectionBinding
import com.droidaio.gallery.models.FolderInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that allows users to select which folders to include in the gallery.
 * Loads folder list from MediaRepository and saves selected folder ids to PrefsManager.
 */
class FolderSelectionFragment : Fragment() {

    private var _binding : FragmentFolderSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter : FolderAdapter
    private val repository by lazy { MediaRepository(requireContext()) }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        _binding = FragmentFolderSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
        adapter = FolderAdapter { _, _ -> }
        binding.foldersRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.foldersRecycler.adapter = adapter

        binding.selectAllButton.setOnClickListener {
            adapter.selectAll()
        }

        binding.invertButton.setOnClickListener {
            adapter.invertSelection()
        }

        binding.clearButton.setOnClickListener {
            adapter.clearSelection()
        }

        binding.saveButton.setOnClickListener {
            val selected = adapter.getSelectedFolderIds()
            val selectedSet = selected.toSet()
            PrefsManager.saveSelectedFolders(requireContext(), selectedSet)
            parentFragmentManager.popBackStack()
            (parentFragmentManager.findFragmentById(R.id.container) as? GalleryFragment)?.refreshWithSelectedFolders()
        }

        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadFolders()
    }

    private fun loadFolders(): List<FolderInfo> {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) { repository.queryFolders() }
            val saved = PrefsManager.getSelectedFolders(requireContext())
            adapter.submitList(folders, saved)
            binding.progress.visibility = View.GONE
        }
            return emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FolderSelectionFragment()
    }
}

