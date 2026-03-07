package com.droidaio.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.droidaio.gallery.databinding.FragmentVaultBinding
import kotlinx.coroutines.launch
import java.io.File

@Suppress("DEPRECATION")
class VaultFragment : Fragment() {

    private var _binding : FragmentVaultBinding? = null
    private val binding get() = _binding!!
    private val vaultAdapter by lazy { VaultAdapter() }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        _binding = FragmentVaultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
        binding.vaultRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.vaultRecycler.adapter = vaultAdapter

        /** For simplicity, we do direct file loading here. Real app should have better UX and error handling. */
        binding.refreshVaultButton.setOnClickListener {
            loadVaultFiles()
        }

        /** For simplicity, we do direct file restoration here. Real app should have better UX and error handling. */
        binding.restoreVaultButton.setOnClickListener {
            val selected = vaultAdapter.getSelected()
            if (selected.isEmpty()) return@setOnClickListener
            // For this example we simply open a SAF create document intent per selected file to choose destination
            selected.forEach { f ->
                // Launch SAF create document for user to pick destination; real app should do batch SAF operations or a better UX
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, f.name)
                }
                startActivityForResult(intent, REQUEST_CREATE_RESTORE)
                // store selection somewhere (not implemented here) — in this example we assume you will handle selected file onActivityResult
            }
        }

        /** For simplicity, we do direct file deletion here. Real app should have better UX and error handling. */
        binding.deleteVaultButton.setOnClickListener {
            val selected = vaultAdapter.getSelected()
            lifecycleScope.launch {
                selected.forEach { vf ->
                    VaultManager.deleteVaultFile(requireContext(), vf)
                }
                loadVaultFiles()
            }
        }

        // initial load
        loadVaultFiles()
    }

    private fun loadVaultFiles() {
        lifecycleScope.launch {
            // Note: VaultManager.listVaultFiles returns List<File> (on-disk vault files). Convert to VaultFile
            val files : List<File> = VaultManager.listVaultFiles(requireContext())
            val vaultFiles = files.map { file ->
                VaultFile(name = file.name, filePath = file.absolutePath, uri = Uri.fromFile(file))
            }
            vaultAdapter.submitList(vaultFiles)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CREATE_RESTORE = 3001
        fun newInstance() = VaultFragment()
    }
}

