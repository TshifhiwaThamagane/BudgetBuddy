package com.budgetbuddy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.databinding.ActivityReceiptViewerBinding
import com.budgetbuddy.util.SessionManager
import kotlinx.coroutines.launch
import java.io.File

class ReceiptViewerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ReceiptViewerActivity"
        const val EXTRA_EXPENSE_ID = "expense_id"
        const val EXTRA_RECEIPT_URI = "receipt_uri"
    }

    private lateinit var binding: ActivityReceiptViewerBinding
    private lateinit var repository: BudgetRepository
    private var expenseId: Int = -1
    private var currentUri: String? = null
    private var cameraImageUri: Uri? = null

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera() else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            currentUri = cameraImageUri.toString()
            displayReceipt(currentUri)
            saveReceipt()
        }
    }

    private val pickGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            currentUri = uri.toString()
            displayReceipt(currentUri)
            saveReceipt()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiptViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BudgetRepository.getInstance(this)
        expenseId = intent.getIntExtra(EXTRA_EXPENSE_ID, -1)
        currentUri = intent.getStringExtra(EXTRA_RECEIPT_URI)

        binding.toolbar.setNavigationOnClickListener { finish() }
        displayReceipt(currentUri)

        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndLaunch() }
        binding.btnChooseGallery.setOnClickListener { pickGalleryLauncher.launch("image/*") }
        binding.btnDeleteReceipt.setOnClickListener { deleteReceipt() }
    }

    private fun displayReceipt(uri: String?) {
        if (uri.isNullOrBlank()) {
            binding.imgReceipt.visibility = View.GONE
            binding.tvNoReceipt.visibility = View.VISIBLE
            return
        }
        binding.imgReceipt.visibility = View.VISIBLE
        binding.tvNoReceipt.visibility = View.GONE
        try {
            binding.imgReceipt.setImageURI(Uri.parse(uri))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading receipt: ${e.message}")
            binding.tvNoReceipt.visibility = View.VISIBLE
            binding.imgReceipt.visibility = View.GONE
        }
    }

    private fun checkCameraPermissionAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        val imageFile = File(cacheDir, "receipts").apply { mkdirs() }
        val file = File(imageFile, "receipt_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        takePictureLauncher.launch(cameraImageUri)
    }

    private fun saveReceipt() {
        if (expenseId == -1) return
        lifecycleScope.launch {
            repository.updateReceipt(expenseId, currentUri)
            Toast.makeText(this@ReceiptViewerActivity, "Receipt saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteReceipt() {
        currentUri = null
        displayReceipt(null)
        if (expenseId != -1) {
            lifecycleScope.launch {
                repository.updateReceipt(expenseId, null)
                Toast.makeText(this@ReceiptViewerActivity, "Receipt deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
