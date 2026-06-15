package com.budgetbuddy.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.budgetbuddy.R
import com.budgetbuddy.data.BudgetRepository
import com.budgetbuddy.data.entity.Category
import com.budgetbuddy.databinding.ActivityAddExpenseBinding
import com.budgetbuddy.util.DateUtils
import com.budgetbuddy.util.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddExpenseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "AddExpenseActivity"
    }

    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var repository: BudgetRepository
    private lateinit var sessionManager: SessionManager
    private var categories: List<Category> = emptyList()
    private var selectedReceiptUri: Uri? = null
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
            selectedReceiptUri = cameraImageUri
            binding.tvReceiptName.text = "Receipt captured"
            Log.d(TAG, "Receipt captured: $cameraImageUri")
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedReceiptUri = uri
        binding.tvReceiptName.text = uri?.lastPathSegment ?: "Receipt selected"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BudgetRepository.getInstance(this)
        sessionManager = SessionManager(this)

        binding.etDate.setText(DateUtils.today())
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.btnUploadReceipt.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndLaunch() }

        lifecycleScope.launch {
            repository.ensureDefaultCategories()
            categories = repository.getCategories()
            binding.spinnerCategory.adapter = ArrayAdapter(
                this@AddExpenseActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categories.map { it.name }
            )
        }

        binding.btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun saveExpense() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull()
        val date = binding.etDate.text.toString().trim()
        val note = binding.etNote.text.toString().trim()
        val category = categories.getOrNull(binding.spinnerCategory.selectedItemPosition)
        val userId = sessionManager.getUserId()

        if (amount == null || date.isEmpty() || category == null) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount <= 0.0) {
            Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val result = repository.addExpense(
                    userId = userId,
                    amount = amount,
                    date = date,
                    categoryId = category.id,
                    note = note,
                    receiptUri = selectedReceiptUri?.toString()
                )
                if (result.newBadges.isNotEmpty()) {
                    val badgeNames = result.newBadges.joinToString(", ") { it.name }
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "🏆 Badge unlocked: $badgeNames (+${result.totalXpEarned} XP)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(this@AddExpenseActivity, "Expense saved", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddExpenseActivity, e.message ?: "Could not save expense", Toast.LENGTH_SHORT).show()
            }
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

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance()
                selected.set(year, month, day)
                binding.etDate.setText(DateUtils.formatDisplay(selected))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
