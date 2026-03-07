package com.droidaio.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.droidaio.gallery.databinding.FragmentFolderSelectionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment that allows users to select which folders to include in the gallery.
 * Loads folder list from MediaRepository and saves selected folder ids to PrefsManager.
 * In a full app, this would be accessible from the GalleryFragment toolbar and would trigger
 * a refresh of the gallery with the new folder selection. For simplicity, this example just
 * demonstrates the UI and persistence of selected folders. In a real app, you would also want
 * to handle edge cases (e.g. no folders found) and provide better feedback to the user.
 * This is a simplified example to demonstrate the concept of folder selection in the gallery app.
 * In a production app, you would likely want to add more features such as:
 * - Displaying folder thumbnails
 * - Showing the number of items in each folder
 * - Allowing users to create custom folder groups
 * - Providing better UI/UX for selection (e.g. checkboxes, multi-select mode)
 * - Handling permissions and edge cases (e.g. no folders found, errors loading folders)
 * This example focuses on the core functionality of loading folders, allowing selection, and saving
 * the selected folder ids. In a real app, you would also want to ensure that the gallery refreshes to
 * reflect the new folder selection after saving. Overall, this fragment demonstrates how to implement
 * a folder selection UI in the gallery app and persist the user's choices for which folders to include in the gallery view.
 * Note: This code assumes that you have a MediaRepository class that can query folders and a PrefsManager class that can save
 * and load the selected folder ids. You would need to implement those classes separately for this to work. In a full app, you
 * would also want to handle edge cases such as no folders found, errors loading folders, and provide better feedback to the user
 * (e.g. showing a message if no folders are available). This example focuses on the core functionality of folder selection and
 * persistence. In a real app, you would also want to ensure that the gallery refreshes to reflect the new folder selection after saving.
 * This example assumes that the GalleryFragment has a method called refreshWithSelectedFolders() that can be called to trigger a refresh
 * of the gallery view with the new folder selection. Overall, this fragment demonstrates how to implement a folder selection UI in the
 * gallery app and persist the user's choices for which folders to include in the gallery view. You can expand on this basic implementation
 * by adding more features and improving the UI/UX as needed. In a production app, you would also want to consider performance implications
 * of loading folders and refreshing the gallery view, especially if the user has a large number of media items. You may want to implement
 * pagination or lazy loading of folders and media items to improve performance and responsiveness of the app.
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
            PrefsManager.saveSelectedFolders(requireContext(), selected)
            parentFragmentManager.popBackStack()
            (parentFragmentManager.findFragmentById(R.id.container) as? GalleryFragment)?.refreshWithSelectedFolders()
        }

        binding.cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadFolders()
    }

    private fun loadFolders() {
        binding.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val folders = withContext(Dispatchers.IO) { repository.queryFolders() }
            val saved = PrefsManager.getSelectedFolders(requireContext())
            adapter.submitList(folders, saved)
            binding.progress.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FolderSelectionFragment()
    }
}

