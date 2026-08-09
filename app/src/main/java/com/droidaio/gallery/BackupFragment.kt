package com.droidaio.gallery

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.droidaio.gallery.databinding.FragmentBackupBinding
import com.droidaio.gallery.models.MediaItem
import com.droidaio.gallery.ui.DialogHelper
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class BackupFragment : Fragment() {

    private var _binding : FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private var itemsToBackup : List<MediaItem> = emptyList()

    // Replace with your server endpoint that exchanges serverAuthCode for refresh token and returns an optional access token
    private val serverExchangeUrl : String by lazy {
        // Use a string resource or BuildConfig in production; placeholder below
        getString(R.string.token_Xchange)
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            itemsToBackup = it.getSerializable(ARG_ITEMS) as? List<MediaItem> ?: emptyList()
        }
        OneDriveManager.init(requireContext())
    }

    override fun onCreateView(inflater : LayoutInflater, container : ViewGroup?, savedInstanceState : Bundle?) : View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view : View, savedInstanceState : Bundle?) {
        binding.googleDriveButton.setOnClickListener {
            startGoogleSignIn()
        }

        binding.oneDriveButton.setOnClickListener {
            startOneDriveSignIn()
        }

        binding.backupAllButton.setOnClickListener {
            lifecycleScope.launch {
                // Prefer Google if signed in and token available
                val googleAccount = GoogleDriveManager.getSignedInAccount(requireContext())
                val googleToken = TokenStore.getGoogleToken(requireContext())
                if (googleToken != null) {
                    performGoogleBackup()
                } else if (OneDriveManager.isSignedIn()) {
                    performOneDriveBackup()
                } else {
                    DialogHelper.showInfo(requireContext(), "No Account", "Please sign in to Google Drive or OneDrive first.")
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        try {
            val intent = GoogleDriveManager.getSignInIntent(requireActivity())
            startActivityForResult(intent, REQ_GOOGLE_SIGNIN)
        } catch (e : Exception) {
            DialogHelper.showRetryDialog(requireContext(), "Sign-in Error", "Unable to start Google sign-in: ${e.localizedMessage}") {
                startGoogleSignIn()
            }
        }
    }

    private fun startOneDriveSignIn() {
        OneDriveManager.acquireTokenInteractive(requireActivity(), object : AuthenticationCallback {
            override fun onSuccess(authenticationResult : IAuthenticationResult) {
                requireActivity().runOnUiThread {
                    DialogHelper.showInfo(requireContext(), "Signed In", "OneDrive sign-in successful.")
                }
            }

            override fun onError(exception : MsalException?) {
                requireActivity().runOnUiThread {
                    DialogHelper.showRetryDialog(requireContext(), "Sign-in Error", "OneDrive sign-in failed: ${exception?.localizedMessage}") {
                        startOneDriveSignIn()
                    }
                }
            }

            override fun onCancel() {
                requireActivity().runOnUiThread {
                    DialogHelper.showInfo(requireContext(), "Cancelled", "OneDrive sign-in cancelled.")
                }
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_GOOGLE_SIGNIN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    handleGoogleSignInSuccess(account)
                } catch (e : ApiException) {
                    DialogHelper.showRetryDialog(requireContext(), "Sign-in Error", "Google sign-in failed: ${e.statusCode} ${e.localizedMessage}") {
                        startGoogleSignIn()
                    }
                }
            } else {
                DialogHelper.showInfo(requireContext(), "Sign-in Cancelled", "Google sign-in was cancelled.")
            }
        }
    }

    private fun handleGoogleSignInSuccess(account : GoogleSignInAccount?) {
        if (account == null) {
            DialogHelper.showInfo(requireContext(), "Sign-in Error", "Google sign-in returned no account.")
            return
        }
        lifecycleScope.launch {
            try {
                showProgress("Exchanging auth code with server")
                GoogleDriveManager.onSignInSuccessAndExchange(requireContext(), account, serverExchangeUrl)
                DialogHelper.showInfo(requireContext(), "Signed In", "Google Drive sign-in successful. Server exchange completed.")
            } catch (e : Exception) {
                DialogHelper.showRetryDialog(requireContext(), "Token Exchange Failed", "Failed to exchange auth code with server: ${e.localizedMessage}") {
                    handleGoogleSignInSuccess(account)
                }
            } finally {
                hideProgress()
            }
        }
    }

    private fun performGoogleBackup() {
        lifecycleScope.launch {
            try {
                showProgress("Uploading to Google Drive")
                BackupManager.backupToGoogleDrive(requireContext(), itemsToBackup)
                DialogHelper.showInfo(requireContext(), "Backup Complete", "Files uploaded to Google Drive.")
            } catch (e : Exception) {
                DialogHelper.showRetryDialog(requireContext(), "Upload Error", "Google Drive upload failed: ${e.localizedMessage}") {
                    performGoogleBackup()
                }
            } finally {
                hideProgress()
            }
        }
    }

    private fun performOneDriveBackup() {
        lifecycleScope.launch {
            try {
                showProgress("Uploading to OneDrive")
                BackupManager.backupToOneDrive(requireContext(), itemsToBackup)
                DialogHelper.showInfo(requireContext(), "Backup Complete", "Files uploaded to OneDrive.")
            } catch (e : Exception) {
                DialogHelper.showRetryDialog(requireContext(), "Upload Error", "OneDrive upload failed: ${e.localizedMessage}") {
                    performOneDriveBackup()
                }
            } finally {
                hideProgress()
            }
        }
    }

    private fun showProgress(message : String) {
        binding.backupAllButton.isEnabled = false
        binding.googleDriveButton.isEnabled = false
        binding.oneDriveButton.isEnabled = false
        binding.title.text = message
    }

    private fun hideProgress(message : String = "Backup") {
        binding.backupAllButton.isEnabled = true
        binding.googleDriveButton.isEnabled = true
        binding.oneDriveButton.isEnabled = true
        binding.title.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ITEMS = "items"
        private const val REQ_GOOGLE_SIGNIN = 4001
        fun newInstance(items : List<MediaItem>) = BackupFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_ITEMS, ArrayList(items)) }
        }
    }
}

/*
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.droidaio.gallery.databinding.FragmentBackupBinding
import com.droidaio.gallery.models.MediaItem
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import kotlinx.coroutines.launch

class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private var itemsToBackup: List<MediaItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            itemsToBackup = it.getSerializable(ARG_ITEMS) as? List<MediaItem> ?: emptyList()
        }
        // Ensure OneDriveManager is initialized
        OneDriveManager.init(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.googleDriveButton.setOnClickListener {
            startGoogleSignIn()
        }

        binding.oneDriveButton.setOnClickListener {
            startOneDriveSignIn()
        }

        binding.backupAllButton.setOnClickListener {
            lifecycleScope.launch {
                // Prefer Google if signed in, otherwise OneDrive if signed in, otherwise prompt
                val googleAccount = GoogleDriveManager.getSignedInAccount(requireContext())
                if (googleAccount != null) {
                    showProgress("Uploading to Google Drive")
                    try {
                        BackupManager.backupToGoogleDrive(requireContext(), itemsToBackup)
                        showAlert("Backup Complete", "Files uploaded to Google Drive.")
                    } catch (e: Exception) {
                        showAlert("Upload Error", "Google Drive upload failed: ${e.localizedMessage}")
                    } finally {
                        hideProgress()
                    }
                } else if (OneDriveManager.isSignedIn()) {
                    showProgress("Uploading to OneDrive")
                    try {
                        BackupManager.backupToOneDrive(requireContext(), itemsToBackup)
                        showAlert("Backup Complete", "Files uploaded to OneDrive.")
                    } catch (e: Exception) {
                        showAlert("Upload Error", "OneDrive upload failed: ${e.localizedMessage}")
                    } finally {
                        hideProgress()
                    }
                } else {
                    showAlert("No Account", "Please sign in to Google Drive or OneDrive first.")
                }
            }
        }
    }

    private fun startGoogleSignIn() {
        try {
            val intent = GoogleDriveManager.getSignInIntent(requireActivity())
            startActivityForResult(intent, REQ_GOOGLE_SIGNIN)
        } catch (e: Exception) {
            showAlert("Sign-in Error", "Unable to start Google sign-in: ${e.localizedMessage}")
        }
    }

    private fun startOneDriveSignIn() {
        OneDriveManager.acquireTokenInteractive(requireActivity(), object : AuthenticationCallback {
            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                requireActivity().runOnUiThread {
                    showAlert("Signed In", "OneDrive sign-in successful.")
                }
            }
            override fun onError(exception: Exception) {
                requireActivity().runOnUiThread {
                    showAlert("Sign-in Error", "OneDrive sign-in failed: ${exception.localizedMessage}")
                }
            }
            override fun onCancel() {
                requireActivity().runOnUiThread {
                    showAlert("Cancelled", "OneDrive sign-in cancelled.")
                }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_GOOGLE_SIGNIN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    handleGoogleSignInSuccess(account)
                } catch (e: ApiException) {
                    showAlert("Sign-in Error", "Google sign-in failed: ${e.statusCode} ${e.localizedMessage}")
                }
            } else {
                showAlert("Sign-in Cancelled", "Google sign-in was cancelled.")
            }
        }
    }

    private fun handleGoogleSignInSuccess(account: GoogleSignInAccount?) {
        if (account == null) {
            showAlert("Sign-in Error", "Google sign-in returned no account.")
            return
        }
        lifecycleScope.launch {
            showProgress("Preparing Google Drive access")
            try {
                GoogleDriveManager.onSignInSuccess(requireContext(), account)
                showAlert("Signed In", "Google Drive sign-in successful.")
            } catch (e: Exception) {
                showAlert("Token Error", "Failed to obtain Drive token: ${e.localizedMessage}")
            } finally {
                hideProgress()
            }
        }
    }

    private fun showProgress(message: String) {
        binding.backupAllButton.isEnabled = false
        binding.googleDriveButton.isEnabled = false
        binding.oneDriveButton.isEnabled = false
        binding.title.text = message
    }

    private fun hideProgress() {
        binding.backupAllButton.isEnabled = true
        binding.googleDriveButton.isEnabled = true
        binding.oneDriveButton.isEnabled = true
        binding.title.text = "Backup"
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext()).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ITEMS = "items"
        private const val REQ_GOOGLE_SIGNIN = 4001
        fun newInstance(items: List<MediaItem>) = BackupFragment().apply {
            arguments = Bundle().apply { putSerializable(ARG_ITEMS, ArrayList(items)) }
        }
    }
}*/

